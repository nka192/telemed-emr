package com.nayoung.telemed.consultation.service;

import com.nayoung.telemed.consultation.dto.ConsultationDTO;
import com.nayoung.telemed.res.Response;

import java.util.List;

public interface ConsultationService {
    Response<ConsultationDTO> createConsultation(ConsultationDTO consultationDTO);
    Response<ConsultationDTO> getConsultationByAppointmentId(Long appointmentId);
    Response<List<ConsultationDTO>> getConsultationHistoryByPatientId(Long patientId);
}
