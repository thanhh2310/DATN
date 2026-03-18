package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationCode(String toEmail, String code){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Mã xác thực đăng ký tài khoản");
        message.setText("Mã OTP của bạn là: " + code + "\n"
                + "Mã này sẽ hết hạn trong 5 phút.");
        javaMailSender.send(message);
    }

    @Async
    public void sendResetPasswordOtp(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Mã xác thực để đổi mật khẩu mới.");
        message.setText("Mã OTP của bạn là: " + code + "\n"
                + "Mã này sẽ hết hạn trong 5 phút.");
        javaMailSender.send(message);
    }
}
