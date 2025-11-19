package com.nayoung.telemed.doctor.service;

import com.nayoung.telemed.doctor.dto.DoctorDTO;
import com.nayoung.telemed.enums.Specialization;
import com.nayoung.telemed.res.Response;

import java.util.List;

public interface DoctorService {
    Response<DoctorDTO> getDoctorProfile();
    Response<?> updateDoctorProfile(DoctorDTO doctorDTO);
    Response<List<DoctorDTO>> getAllDoctors();
    Response<DoctorDTO> getDoctorById(Long doctorId);
    Response<List<DoctorDTO>> searchDoctorsBySpecialization(Specialization specialization);
    Response<List<Specialization>> getAllSpecializationEnums();
}
