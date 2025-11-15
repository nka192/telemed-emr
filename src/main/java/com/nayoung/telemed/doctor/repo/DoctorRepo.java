package com.nayoung.telemed.doctor.repo;

import com.nayoung.telemed.doctor.entity.Doctor;
import com.nayoung.telemed.enums.Specialization;
import com.nayoung.telemed.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepo extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUser(User user);

    List<Doctor> findBySpecialization(Specialization specialization);
}
