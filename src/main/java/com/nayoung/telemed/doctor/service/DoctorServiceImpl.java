package com.nayoung.telemed.doctor.service;

import com.nayoung.telemed.doctor.dto.DoctorDTO;
import com.nayoung.telemed.doctor.entity.Doctor;
import com.nayoung.telemed.doctor.repo.DoctorRepo;
import com.nayoung.telemed.enums.Specialization;
import com.nayoung.telemed.exceptions.BadRequestException;
import com.nayoung.telemed.exceptions.NotFoundException;
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
public class DoctorServiceImpl implements DoctorService{

    private final DoctorRepo doctorRepo;
    private final UserService userService;
    private final ModelMapper modelMapper;

    @Override
    public Response<DoctorDTO> getDoctorProfile() {
        User user = userService.getCurrentUser();

        Doctor doctor = doctorRepo.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Doctor not found"));

        return success("Doctor profile retrieved successfully", modelMapper.map(doctor, DoctorDTO.class));
    }

    @Override
    public Response<?> updateDoctorProfile(DoctorDTO doctorDTO) {
        User user = userService.getCurrentUser();

        Doctor doctor = doctorRepo.findByUser(user)
                .orElseThrow(() -> new NotFoundException("Doctor not found"));

        // check fields
        if (StringUtils.hasText(doctorDTO.getFirstName())) {
            doctor.setFirstName(doctorDTO.getFirstName());
        }
        if (StringUtils.hasText(doctorDTO.getLastName())) {
            doctor.setLastName(doctorDTO.getLastName());
        }
        if (StringUtils.hasText(doctorDTO.getLicenseNumber())) {
            doctor.setLicenseNumber(doctorDTO.getLicenseNumber());
        }
        Optional.ofNullable(doctorDTO.getSpecialization()).ifPresent(doctor::setSpecialization);
        Optional.ofNullable(doctorDTO.getSpecialization()).ifPresent(doctor::setSpecialization);

        doctorRepo.save(doctor);
        log.info("Doctor profile updated");

        return success("Doctor profile updated successfully", null);
    }

    @Override
    public Response<List<DoctorDTO>> getAllDoctors() {
        List<DoctorDTO> doctorDTOS = doctorRepo.findAll().stream()
                .map(doctor -> modelMapper.map(doctor, DoctorDTO.class))
                .toList();

        return success("All doctors retrieved successfully", doctorDTOS);
    }

    @Override
    public Response<DoctorDTO> getDoctorById(Long doctorId) {
        Doctor doctor = doctorRepo.findById(doctorId)
                .orElseThrow(() -> new BadRequestException("Doctor not found with ID: " + doctorId));

        DoctorDTO doctorDTO = modelMapper.map(doctor, DoctorDTO.class);

        return success("Doctor retrieved successfully", doctorDTO);
    }

    @Override
    public Response<List<DoctorDTO>> getDoctorsBySpecialization(Specialization specialization) {
        List<Doctor> doctors = doctorRepo.findBySpecialization(specialization);

        List<DoctorDTO> doctorDTOS = doctors.stream()
                .map(doctor -> modelMapper.map(doctor, DoctorDTO.class))
                .toList();

        String message = doctorDTOS.isEmpty() ?
                "No doctors found for specialization: " + specialization.name() :
                "Doctors retrieved successfully for specialization: " + specialization.name();

        return success(message, doctorDTOS);
    }

    @Override
    public Response<List<Specialization>> getAllSpecializationEnums() {
        List<Specialization> specializations = Arrays.asList(Specialization.values());

        return success("All specializations retrieved successfully", specializations);
    }

    private <T> Response<T> success(String message, T data) {
        return Response.<T>builder()
                .statusCode(200)
                .message(message)
                .data(data)
                .build();
    }
}
