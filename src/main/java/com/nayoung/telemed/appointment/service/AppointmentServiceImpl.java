package com.nayoung.telemed.appointment.service;

import com.nayoung.telemed.appointment.dto.AppointmentDTO;
import com.nayoung.telemed.appointment.entity.Appointment;
import com.nayoung.telemed.appointment.repo.AppointmentRepo;
import com.nayoung.telemed.doctor.entity.Doctor;
import com.nayoung.telemed.doctor.repo.DoctorRepo;
import com.nayoung.telemed.enums.AppointmentStatus;
import com.nayoung.telemed.exceptions.BadRequestException;
import com.nayoung.telemed.exceptions.NotFoundException;
import com.nayoung.telemed.notification.dto.NotificationDTO;
import com.nayoung.telemed.notification.service.NotificationService;
import com.nayoung.telemed.patient.entity.Patient;
import com.nayoung.telemed.patient.repo.PatientRepo;
import com.nayoung.telemed.res.Response;
import com.nayoung.telemed.users.entity.User;
import com.nayoung.telemed.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService{

    private final AppointmentRepo appointmentRepo;
    private final PatientRepo patientRepo;
    private final DoctorRepo doctorRepo;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy 'at' hh:mm a");

    @Override
    public Response<AppointmentDTO> bookAppointment(AppointmentDTO appointmentDTO) {
        User user = userService.getCurrentUser();

        // 1. get the patient initiating the booking
        Patient patient = patientRepo.findByUser(user)
                .orElseThrow(() -> new NotFoundException("Patient profile required for booking"));

        // 2. get the target doctor
        Doctor doctor = doctorRepo.findById(appointmentDTO.getDoctorId())
                .orElseThrow(() -> new NotFoundException("Doctor not found"));

        // VALIDATION LOGIC
        // define the proposed time slot and the end time
        LocalDateTime startTime = appointmentDTO.getStartTime();
        LocalDateTime endTime = startTime.plusMinutes(60); // assuming 60-min slot

        // 3. basic validation - booking must be at least 1 hr in advance
        if (startTime.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new BadRequestException("Appointments must be booked at least 1 hour in advance");
        }

        // enforce a mandatory one-hour break (or buffer) for the doctor before a new appointment
        LocalDateTime checkStart = startTime.minusMinutes(60);

        // check for existing appointments whose END TIME overlaps with the proposed start time
        // or whose start time overlaps with the proposed end time
        List<Appointment> conflicts = appointmentRepo.findConflictingAppointments(
                doctor.getId(),
                checkStart,
                endTime
        );

        if (!conflicts.isEmpty()) {
            throw new BadRequestException("Doctor is not available at the requested time. Please check their schedule.");
        }

        // 4a. generate a unique, random string for the room name
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String uniqueRoomName = "carebridge-" + uuid.substring(0, 10);

        // 4b. use the public Jitsi Meet domain with unique room name
        String meetingLink = "https://meet.jit.si/" + uniqueRoomName;

        log.info("Generated Jitsi meeting link: {}", meetingLink);

        // 5, build and save appointment
        Appointment appointment = Appointment.builder()
                .startTime(appointmentDTO.getStartTime())
                .endTime(appointmentDTO.getStartTime().plusMinutes(60))
                .meetingLink(meetingLink)
                .initialSymptoms(appointmentDTO.getInitialSymptoms())
                .purposeOfConsultation(appointmentDTO.getPurposeOfConsultation())
                .status(AppointmentStatus.SCHEDULED)
                .doctor(doctor)
                .patient(patient)
                .build();

        Appointment savedAppointment = appointmentRepo.save(appointment);

        sendAppointmentConfirmation(savedAppointment);

        return Response.<AppointmentDTO>builder()
                .statusCode(200)
                .message("Appointment booked successfully")
                .build();
    }

    @Override
    public Response<List<AppointmentDTO>> getMyAppointments() {

        User user = userService.getCurrentUser();
        Long userId = user.getId();
        List<Appointment> appointments;

        // 1. Check if the user is doctor or patient
        boolean isDoctor = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("DOCTOR"));

        if (isDoctor) {
            // Check for doctor profile existence (required to throw the correct exception)
            doctorRepo.findByUser(user)
                    .orElseThrow(() -> new NotFoundException("Doctor profile not found"));
            // Fetch appointments of the doctor efficiently
            appointments = appointmentRepo.findByDoctor_User_IdOrderByIdDesc(userId);
        } else {
            // Check for patient profile existence
            patientRepo.findByUser(user)
                    .orElseThrow(() -> new NotFoundException("Patient profile not found"));
            // Fetch appointments using the User ID to navigate patient relationship
            appointments = appointmentRepo.findByPatient_User_IdOrderByIdDesc(userId);
        }

        // 2. Convert the list of entities to DTOs in a single step
        List<AppointmentDTO> appointmentDTOList = appointments.stream()
                .map(appointment -> modelMapper.map(appointment, AppointmentDTO.class))
                .toList();

        return success("Appointments retrieved successfully.", appointmentDTOList);
    }

    @Override
    public Response<AppointmentDTO> cancelAppointment(Long appointmentId) {
        User user = userService.getCurrentUser();
        Appointment appointment = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found."));

        // Add security check: only the patient or doctor involved can cancel
        boolean isOwner = appointment.getPatient().getUser().getId().equals(user.getId()) ||
                appointment.getDoctor().getUser().getId().equals(user.getId());
        if (!isOwner) {
            throw new BadRequestException("You do not have permission to cancel this appointment.");
        }

        // Update appointment status
        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment savedAppointment = appointmentRepo.save(appointment);

        // Send notification to the other party (doctor/patient)
        sendAppointmentCancellation(savedAppointment, user);

        return success("Appointment cancelled successfully.", null);
    }

    @Override
    public Response<?> completeAppointment(Long appointmentId) {

        // Get the current user (must be the Doctor)
        User currentUser = userService.getCurrentUser();

        // 1. Fetch the appointment
        Appointment appointment = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found with ID: " + appointmentId));

        // Security check 1: ensure the current user is the doctor assigned to this appointment
        if (!appointment.getDoctor().getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Only the assigned doctor can mark this appointment as completed");
        }

        // 2. Update appointment status and end time
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setEndTime(LocalDateTime.now());

        appointmentRepo.save(appointment);

        return success("Appointment successfully marked as completed. You may proceed to create the consultation notes.", null);
    }

    private void sendAppointmentConfirmation(Appointment appointment) {

        // 1. prepare patient notification
        User patientUser = appointment.getPatient().getUser();
        String formattedTime = appointment.getStartTime().format(FORMATTER);

        Map<String, Object> patientVars = new HashMap<>();

        patientVars.put("patientName", patientUser.getName());
        patientVars.put("doctorName", appointment.getDoctor().getUser().getName());
        patientVars.put("appointmentTime", formattedTime);
        patientVars.put("isVirtual", true);
        patientVars.put("meetingLink", appointment.getMeetingLink());
        patientVars.put("purposeOfConsultation", appointment.getPurposeOfConsultation());

        NotificationDTO patientNotification = NotificationDTO.builder()
                .recipient(patientUser.getEmail())
                .subject("CareBridge: Your Appointment is Confirmed")
                .templateName("patient-appointment")
                .templateVariables(patientVars)
                .build();

        // dispatch patient email using the low-level service
        notificationService.sendEmail(patientNotification, patientUser);
        log.info("Dispatched confirmation email for patient: {}", patientUser.getEmail());

        // 2. prepare doctor notification
        User doctorUser = appointment.getDoctor().getUser();

        Map<String, Object> doctorVars = new HashMap<>();

        doctorVars.put("doctorName", doctorUser.getName());
        doctorVars.put("patientFullName", patientUser.getName());
        doctorVars.put("appointmentTime", formattedTime);
        doctorVars.put("isVirtual", true);
        doctorVars.put("meetingLink", appointment.getMeetingLink());
        doctorVars.put("initialSymptoms", appointment.getInitialSymptoms());
        doctorVars.put("purposeOfConsultation", appointment.getPurposeOfConsultation());

        NotificationDTO doctorNotification = NotificationDTO.builder()
                .recipient(doctorUser.getEmail())
                .subject("CareBridge: Your Appointment is Booked")
                .templateName("doctor-appointment")
                .templateVariables(doctorVars)
                .build();

        // dispatch doctor email using the low-level service
        notificationService.sendEmail(doctorNotification, doctorUser);
        log.info("Dispatched confirmation email for doctor: {}", doctorUser.getEmail());
    }

    private void sendAppointmentCancellation(Appointment appointment, User cancellingUser) {

        User patientUser = appointment.getPatient().getUser();
        User doctorUser = appointment.getDoctor().getUser();

        // Safety check to ensure the cancellingUser is involved
        boolean isOwner = patientUser.getId().equals(cancellingUser.getId()) ||
                doctorUser.getId().equals(cancellingUser.getId());
        if (!isOwner) {
            log.error("Cancellation initiated by user not associated with appointment. User Id: {}", cancellingUser);
        }

        String formattedTime = appointment.getStartTime().format(FORMATTER);
        String cancellingPartyName = cancellingUser.getName();

        // Common variables for the template
        Map<String, Object> baseVars = new HashMap<>();
        baseVars.put("cancellingPartyName", cancellingPartyName);
        baseVars.put("appointmentTime", formattedTime);
        baseVars.put("doctorName", appointment.getDoctor().getLastName());
        baseVars.put("patientFullName", patientUser.getName());

        // 1. Dispatch email to patient
        Map<String, Object> patientVars = new HashMap<>(baseVars);
        patientVars.put("recipientName", patientUser.getName());

        NotificationDTO patientNotification = NotificationDTO.builder()
                .recipient(patientUser.getEmail())
                .subject("CareBridge: Your Appointment is Confirmed")
                .templateName("appointment-cancellation")
                .templateVariables(patientVars)
                .build();

        notificationService.sendEmail(patientNotification, patientUser);
        log.info("Dispatched cancellation email to patient: {}", patientUser.getEmail());

        // 2. Dispatch email to doctor
        Map<String, Object> doctorVars = new HashMap<>(baseVars);
        doctorVars.put("recipientName", doctorUser.getName());

        NotificationDTO doctorNotification = NotificationDTO.builder()
                .recipient(doctorUser.getEmail())
                .subject("CareBridge: Appointment Cancellation")
                .templateName("appointment-cancellation")
                .templateVariables(doctorVars)
                .build();

        // dispatch doctor email using the low-level service
        notificationService.sendEmail(doctorNotification, doctorUser);
        log.info("Dispatched cancellation email to doctor: {}", doctorUser.getEmail());
    }

    private <T> Response<T> success(String message, T data) {
        return Response.<T>builder()
                .statusCode(200)
                .message(message)
                .data(data)
                .build();
    }
}
