package com.nayoung.telemed.patient.service;

import com.nayoung.telemed.enums.BloodGroup;
import com.nayoung.telemed.enums.Genotype;
import com.nayoung.telemed.exceptions.BadRequestException;
import com.nayoung.telemed.exceptions.NotFoundException;
import com.nayoung.telemed.patient.dto.PatientDTO;
import com.nayoung.telemed.patient.entity.Patient;
import com.nayoung.telemed.patient.repo.PatientRepo;
import com.nayoung.telemed.res.Response;
import com.nayoung.telemed.users.entity.User;
import com.nayoung.telemed.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientServiceImpl implements PatientService{

    private final PatientRepo patientRepo;
    private final UserService userService;
    private final ModelMapper modelMapper;

    @Override
    public Response<PatientDTO> getPatientProfile() {
        User user = userService.getCurrentUser();

        Patient patient = patientRepo.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Patient not found"));

        return success("Patient profile retrieved successfully", modelMapper.map(patient, PatientDTO.class));
    }

    @Override
    public Response<?> updatePatientProfile(PatientDTO patientDTO) {
        User currentUser = userService.getCurrentUser();

        Patient patient = patientRepo.findByUser(currentUser)
                .orElseThrow(() -> new NotFoundException("Patient profile not found"));

        // check basic fields
        if (StringUtils.hasText(patientDTO.getFirstName())) {
            patient.setFirstName(patientDTO.getFirstName());
        }
        if (StringUtils.hasText(patientDTO.getLastName())) {
            patient.setLastName(patientDTO.getLastName());
        }
        if (StringUtils.hasText(patientDTO.getPhone())) {
            patient.setPhone(patientDTO.getPhone());
        }

        // medical fields
        if (StringUtils.hasText(patientDTO.getKnownAllergies())) {
            patient.setKnownAllergies(patientDTO.getKnownAllergies());
        }

        // LocalDate field
        Optional.ofNullable(patientDTO.getDateOfBirth()).ifPresent(patient::setDateOfBirth);

        // Enum fields
        Optional.ofNullable(patientDTO.getBloodGroup()).ifPresent(patient::setBloodGroup);
        Optional.ofNullable(patientDTO.getGenotype()).ifPresent(patient::setGenotype);

        patientRepo.save(patient);

        return success("Patient profile updated successfully", null);
    }

    @Override
    public Response<PatientDTO> getPatientById(Long patientId) {
        Patient patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new BadRequestException("Patient not found with ID: " + patientId));

        PatientDTO patientDTO = modelMapper.map(patient, PatientDTO.class);

        return success("Patient retrieved successfully", patientDTO);
    }

    @Override
    public Response<List<BloodGroup>> getAllBloodGroupEnums() {
        List<BloodGroup> bloodGroups = Arrays.asList(BloodGroup.values());


        return success("All bloodGroups retrieved successfully", bloodGroups);
    }

    @Override
    public Response<List<Genotype>> getAllGenotypeEnums() {
        List<Genotype> genotypes = Arrays.asList(Genotype.values());

        return success("All genotypes retrieved successfully", genotypes);
    }

    @Override
    public Response<List<PatientDTO>> getAllPatients() {
        List<PatientDTO> patientDTOS = patientRepo.findAll().stream()
                .map(patient -> modelMapper.map(patient, PatientDTO.class))
                .toList();

        return success("All patients retrieved successfully", patientDTOS);
    }

    private <T> Response<T> success(String message, T data) {
        return Response.<T>builder()
                .statusCode(200)
                .message(message)
                .data(data)
                .build();
    }
}
