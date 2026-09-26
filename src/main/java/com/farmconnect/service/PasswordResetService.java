package com.farmconnect.service;

import com.farmconnect.dto.request.ForgotPasswordRequest;
import com.farmconnect.dto.request.ResetPasswordRequest;
import com.farmconnect.dto.response.MessageResponse;

public interface PasswordResetService {
    MessageResponse forgotPassword(ForgotPasswordRequest request);
    MessageResponse resetPassword(ResetPasswordRequest request);
}
