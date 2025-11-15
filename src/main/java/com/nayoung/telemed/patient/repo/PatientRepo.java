package com.nayoung.telemed.patient.repo;

import com.nayoung.telemed.patient.entity.Patient;
import com.nayoung.telemed.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepo extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUser(User user);
}
