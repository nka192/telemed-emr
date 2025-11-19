package com.nayoung.telemed.patient.service;

import com.nayoung.telemed.patient.dto.PatientDTO;
import com.nayoung.telemed.res.Response;

import java.util.List;

public interface PatientService {
    Response<PatientDTO> getPatientProfile();
    Response<?> updatePatientProfile(PatientDTO patientDTO);
    Response<PatientDTO> getPatientById(Long patientId);
    Response<List<PatientDTO>> getAllBloodGroupEnums();
    Response<List<PatientDTO>> getAllGenotypeEnums();
    Response<List<PatientDTO>> getAllPatients();
}
