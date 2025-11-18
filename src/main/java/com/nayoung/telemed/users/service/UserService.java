package com.nayoung.telemed.users.service;

import com.nayoung.telemed.res.Response;
import com.nayoung.telemed.users.dto.UpdatePasswordRequest;
import com.nayoung.telemed.users.dto.UserDTO;
import com.nayoung.telemed.users.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    User getCurrentUser();
    Response<UserDTO> getMyUserDetails();
    Response<UserDTO> getUserById(Long userId);
    Response<List<UserDTO>> getAllUsers();
    Response<?> updatePassword(UpdatePasswordRequest updatePasswordRequest);
    Response<?> uploadProfilePicture(MultipartFile file);
}
