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
        return null;
    }

    @Override
    public Response<AppointmentDTO> cancelAppointment(Long appointmentId) {
        return null;
    }

    @Override
    public Response<?> completeAppointment(Long appointmentId) {
        return null;
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
}
