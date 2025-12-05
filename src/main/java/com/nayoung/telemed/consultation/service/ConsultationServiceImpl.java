package com.nayoung.telemed.consultation.service;

import com.nayoung.telemed.appointment.entity.Appointment;
import com.nayoung.telemed.appointment.repo.AppointmentRepo;
import com.nayoung.telemed.consultation.dto.ConsultationDTO;
import com.nayoung.telemed.consultation.entity.Consultation;
import com.nayoung.telemed.consultation.repo.ConsultationRepo;
import com.nayoung.telemed.enums.AppointmentStatus;
import com.nayoung.telemed.exceptions.BadRequestException;
import com.nayoung.telemed.exceptions.NotFoundException;
import com.nayoung.telemed.patient.repo.PatientRepo;
import com.nayoung.telemed.res.Response;
import com.nayoung.telemed.users.entity.User;
import com.nayoung.telemed.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationServiceImpl implements ConsultationService{
    private final ConsultationRepo consultationRepo;
    private final AppointmentRepo appointmentRepo;
    private final PatientRepo patientRepo;
    private final UserService userService;
    private final ModelMapper modelMapper;

    @Override
    public Response<ConsultationDTO> createConsultation(ConsultationDTO consultationDTO) {
        User user = userService.getCurrentUser();
        Long appointmentId = consultationDTO.getAppointmentId();

        Appointment appointment = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found."));

        // 1. Security check: must be the doctor linked to the appointment
        if (!appointment.getDoctor().getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Your are not authorized to create notes for this consultation.");
        }
        // 2. Complete the appointment
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepo.save(appointment);
        // 3. Ensure a consultation doesn't already exist for this appointment
        if (consultationRepo.findByAppointmentId(appointmentId).isPresent()) {
            throw new BadRequestException("Consultation notes exist for this appointment");
        }

        Consultation consultation = Consultation.builder()
                .consultationDate(LocalDateTime.now())
                .subjectiveNotes(consultationDTO.getSubjectiveNotes())
                .objectiveFindings(consultationDTO.getObjectiveFindings())
                .assessment(consultationDTO.getAssessment())
                .plan(consultationDTO.getPlan())
                .appointment(appointment)
                .build();

        consultationRepo.save(consultation);

        return success("Consultation notes saved successfully.", null);
    }

    @Override
    public Response<ConsultationDTO> getConsultationByAppointmentId(Long appointmentId) {
        Consultation consultation = consultationRepo.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new NotFoundException("Consultation notes not found for appointment ID: " + appointmentId));

        ConsultationDTO consultationDTO = modelMapper.map(consultation, ConsultationDTO.class);

        return success("Consultation notes retrieved successfully", consultationDTO);
    }

    @Override
    public Response<List<ConsultationDTO>> getConsultationHistoryByPatientId(Long patientId) {
        return null;
    }

    private <T> Response<T> success(String message, T data) {
        return Response.<T>builder()
                .statusCode(200)
                .message(message)
                .data(data)
                .build();
    }
}
