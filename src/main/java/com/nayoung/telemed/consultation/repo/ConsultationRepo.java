package com.nayoung.telemed.consultation.repo;

import com.nayoung.telemed.consultation.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationRepo extends JpaRepository<Consultation, Long> {

    Optional<Consultation> findByAppointmentId(Long appointmentId);

    List<Consultation> findByAppointmentPatientIdOrderByConsultationDateDesc(Long patientId);
}
