package com.nayoung.telemed.users.service;

import com.nayoung.telemed.res.Response;
import com.nayoung.telemed.users.dto.LoginRequest;
import com.nayoung.telemed.users.dto.LoginResponse;
import com.nayoung.telemed.users.dto.RegistrationRequest;
import com.nayoung.telemed.users.dto.ResetPasswordRequest;

public interface AuthService {
    Response<String> register(RegistrationRequest request);
    Response<LoginResponse> login(LoginRequest loginRequest);
    Response<?> forgetPassword(String email);
    Response<?> updatePasswordViaResetCode(ResetPasswordRequest resetPasswordRequest);
}
