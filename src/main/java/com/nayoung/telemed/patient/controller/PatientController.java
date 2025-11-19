package com.nayoung.telemed.patient.controller;

import com.nayoung.telemed.enums.BloodGroup;
import com.nayoung.telemed.enums.Genotype;
import com.nayoung.telemed.patient.dto.PatientDTO;
import com.nayoung.telemed.patient.service.PatientService;
import com.nayoung.telemed.res.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('PATIENT')")
    public ResponseEntity<Response<PatientDTO>> getPatientProfile(){
        return ResponseEntity.ok(patientService.getPatientProfile());
    }

    @PutMapping("/update-profile")
    public ResponseEntity<Response<?>> updatePatientProfile(@RequestBody PatientDTO patientDTO) {
        return ResponseEntity.ok(patientService.updatePatientProfile(patientDTO));
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<Response<PatientDTO>> getPatientById(@PathVariable Long patientId) {
        return ResponseEntity.ok(patientService.getPatientById(patientId));
    }

    @GetMapping("/bloodgroup")
    public ResponseEntity<Response<List<BloodGroup>>> getAllBloodGroupEnums() {
        return ResponseEntity.ok(patientService.getAllBloodGroupEnums());
    };

    @GetMapping("/genotype")
    public ResponseEntity<Response<List<Genotype>>> getAllGenotypeEnums() {
        return ResponseEntity.ok(patientService.getAllGenotypeEnums());
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<List<PatientDTO>>> getAllPatients() {
        return ResponseEntity.ok(patientService.getAllPatients());
    }
}
