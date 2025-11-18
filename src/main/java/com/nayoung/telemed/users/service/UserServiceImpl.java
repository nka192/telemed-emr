package com.nayoung.telemed.users.service;

import com.nayoung.telemed.exceptions.BadRequestException;
import com.nayoung.telemed.exceptions.NotFoundException;
import com.nayoung.telemed.notification.dto.NotificationDTO;
import com.nayoung.telemed.notification.service.NotificationService;
import com.nayoung.telemed.res.Response;
import com.nayoung.telemed.users.dto.UpdatePasswordRequest;
import com.nayoung.telemed.users.dto.UserDTO;
import com.nayoung.telemed.users.entity.User;
import com.nayoung.telemed.users.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService{

    private final UserRepo userRepo;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    private final String uploadDir = "uploads/profile-pictures/"; // backend location for saving images

    // called within service
    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new NotFoundException("User is not authenticated");
        }

        String email = authentication.getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    // called from controller
    @Override
    public Response<UserDTO> getMyUserDetails() {
        User user = getCurrentUser();

        UserDTO userDTO = modelMapper.map(user, UserDTO.class);

        return success("User details retrieved successfully", userDTO);
    }

    @Override
    public Response<UserDTO> getUserById(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found with ID: " + userId));

        UserDTO userDTO = modelMapper.map(user, UserDTO.class);

        return success("User details retrieved successfully", userDTO);
    }

    @Override
    public Response<List<UserDTO>> getAllUsers() {
        List<UserDTO> userDTOS = userRepo.findAll().stream()
                .map(user -> modelMapper.map(user, UserDTO.class))
                .toList();

        return success("All users retrieved successfully", userDTOS);
    }

    @Override
    public Response<?> updatePassword(UpdatePasswordRequest updatePasswordRequest) {
        User user = getCurrentUser();

        String oldPassword = updatePasswordRequest.getOldPassword();
        String newPassword = updatePasswordRequest.getNewPassword();

        if (oldPassword == null || newPassword == null) {
            throw new BadRequestException("Old and new password required");
        }

        // Validate old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadRequestException("Old password not correct");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        // send password change confirmation email to user
        NotificationDTO passwordChangeEmail = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Your password was successfully changed")
                .templateName("password-change")
                .templateVariables(Map.of(
                        "name", user.getName()
                )).build();

        notificationService.sendEmail(passwordChangeEmail, user);

        return success("Password changed successfully", null);
    }

    @Override
    public Response<?> uploadProfilePicture(MultipartFile file) {
        User user = getCurrentUser();

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
                Path oldFile = Paths.get(user.getProfilePictureUrl());
                if(Files.exists(oldFile)) {
                    Files.delete(oldFile);
                }
            }

            // generate a unique file name to avoid conflicts
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";

            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID() + fileExtension;
            Path filePath = uploadPath.resolve(newFileName);

            Files.copy(file.getInputStream(), filePath);
            String fileUrl = uploadDir + newFileName;

            user.setProfilePictureUrl(fileUrl);
            userRepo.save(user);

            return success("Profile picture uploaded successfully", fileUrl);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private <T> Response<T> success(String message, T data) {
        return Response.<T>builder()
                .statusCode(200)
                .message(message)
                .data(data)
                .build();
    }
}
