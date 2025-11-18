package com.nayoung.telemed.users.service;

import com.nayoung.telemed.doctor.entity.Doctor;
import com.nayoung.telemed.doctor.repo.DoctorRepo;
import com.nayoung.telemed.exceptions.BadRequestException;
import com.nayoung.telemed.exceptions.NotFoundException;
import com.nayoung.telemed.notification.dto.NotificationDTO;
import com.nayoung.telemed.notification.service.NotificationService;
import com.nayoung.telemed.patient.entity.Patient;
import com.nayoung.telemed.patient.repo.PatientRepo;
import com.nayoung.telemed.res.Response;
import com.nayoung.telemed.role.entity.Role;
import com.nayoung.telemed.role.repo.RoleRepo;
import com.nayoung.telemed.security.JwtService;
import com.nayoung.telemed.users.dto.LoginRequest;
import com.nayoung.telemed.users.dto.LoginResponse;
import com.nayoung.telemed.users.dto.RegistrationRequest;
import com.nayoung.telemed.users.dto.ResetPasswordRequest;
import com.nayoung.telemed.users.entity.User;
import com.nayoung.telemed.users.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService; // generate a token for user
    private final NotificationService notificationService; // send account creation details mail to user

    private final PatientRepo patientRepo;
    private final DoctorRepo doctorRepo;

    @Value("${password.reset.link}")
    private String resetLink;

    @Value("${login.link}")
    private String loginLink;

    @Override
    public Response<String> register(RegistrationRequest request) {
        // 1. check if user already exists
        if (userRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("User with email already exists");
        }

        // determine the roles to assign (default to PATIENT if none are provided)
        List<String> requestedRoleNames = (request.getRoles() != null && !request.getRoles().isEmpty())
                ? request.getRoles().stream().map(String::toUpperCase).toList()
                : List.of("PATIENT");

        boolean isDoctor = requestedRoleNames.contains("DOCTOR");

        if (isDoctor && (request.getLicenseNumber() == null || request.getLicenseNumber().isBlank())) {
            throw new BadRequestException("License number required to register as a doctor");
        }

        // 2. load and validate roles from the database
        List<Role> roles = requestedRoleNames.stream()
                .map(roleRepo::findByName)
                .flatMap(Optional::stream)
                .toList();

        if (roles.isEmpty()) {
            throw new NotFoundException("Registration failed: Requested roles were not found in the database");
        }

        // 3. create and save new user entity
        User newUser = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .roles(roles)
                .build();

        User savedUser = userRepo.save(newUser);

        log.info("New user registered: {} with {} roles", savedUser.getEmail(), roles.size());

        // 4. process profile creation (patient, doctor profiles)
        for (Role role : roles) {
            String roleName = role.getName();

            switch (roleName) {
                case "PATIENT":
                    createPatientProfile(savedUser);
                    log.info("Patient profile created: {}", savedUser.getEmail());
                    break;

                case "DOCTOR":
                    createDoctorProfile(request, savedUser);
                    log.info("Doctor profile created: {}", savedUser.getEmail());
                    break;

                case "ADMIN":
                    log.info("Admin role assigned to user: {}", savedUser.getEmail());
                    break;

                default:
                    log.warn("Assigned role '{} has no corresponding profile creation logic", roleName);
                    break;
            }
        }

        // 5. send welcome email out to user
        sendRegistrationEmail(request, savedUser);

        // 6. return success response
        return Response.<String>builder()
                .statusCode(200)
                .message("Registration successful. A welcome email has been sent to you")
                .data(savedUser.getEmail())
                .build();
    }

    @Override
    public Response<LoginResponse> login(LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User Not Found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Password doesn't match");
        }

        String token = jwtService.generateToken(user.getEmail());
        LoginResponse loginResponse = LoginResponse.builder()
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .token(token)
                .build();

        return Response.<LoginResponse>builder()
                .statusCode(200)
                .message("Login Successful")
                .data(loginResponse)
                .build();
    }

    @Override
    public Response<?> forgetPassword(String email) {
        return null;
    }

    @Override
    public Response<?> updatePasswordViaResetCode(ResetPasswordRequest resetPasswordRequest) {
        return null;
    }

    private void createPatientProfile(User user) {
        Patient patient = Patient.builder()
                .user(user)
                .build();

        patientRepo.save(patient);
        log.info("Patient profile created");
    }

    private void createDoctorProfile(RegistrationRequest request, User user) {
        Doctor doctor = Doctor.builder()
                .specialization(request.getSpecialization())
                .licenseNumber(request.getLicenseNumber())
                .user(user)
                .build();

        doctorRepo.save(doctor);
        log.info("Doctor profile created");
    }

    private void sendRegistrationEmail(RegistrationRequest request, User user) {
        NotificationDTO welcomeEmail = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Welcome to CareBridge!")
                .templateName("welcome")
                .message("Thank you for registering. Your account is ready.")
                .templateVariables(Map.of(
                        "name", request.getName(),
                        "loginLink", loginLink
                ))
                .build();
        notificationService.sendEmail(welcomeEmail, user);
    }
}
