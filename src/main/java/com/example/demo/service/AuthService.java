package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.auth.JwtUtils;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.*;
import com.example.demo.dto.response.TokenResponse;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.Cart;
import com.example.demo.model.User;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CartRepository cartRepository;
    private final  EmailService emailService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RedisService redisService;

    // ham register
    @Transactional
    public void register(RegisterRequest request){
        // kiem tra xem user ton tai chua
        var existingUserOpt = userRepository.findByEmail(request.getEmail());

        if(existingUserOpt.isPresent()){
            User existingUser = existingUserOpt.get();
            // 1. Nếu tài khoản đã bị Admin xóa mềm (có deleted_at)
            if (existingUser.getDeletedAt() != null) {
                throw new WebErrorConfig(ErrorCode.USER_IS_DELETED); // Hoặc thay bằng mã lỗi phù hợp
            }
            // 2. Nếu tài khoản đang hoạt động bình thường
            if (existingUser.getIsActive()) {
                throw new WebErrorConfig(ErrorCode.USER_AlREADY_EXISTED);
            }
            // 3. Xử lý khách đã đăng ký nhưng CHƯA verify OTP
            // Khách có thể đổi ý nhập Password hoặc Tên mới so với lần trước
            existingUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            existingUser.setFirstName(request.getFirstName());
            existingUser.setLastName(request.getLastName());
            existingUser.setPhoneNumber(request.getPhoneNumber());

            userRepository.save(existingUser); // Cập nhật lại data

            // Tạo và gửi OTP MỚI
            String code = otpService.generateAndStoreOtp(existingUser.getEmail());
            sendVerificationOtpOrThrow(existingUser.getEmail(), code);

            // RẤT QUAN TRỌNG: return luôn tại đây để KẾT THÚC HÀM.
            // Không chạy xuống đoạn tạo User mới và tạo Giỏ hàng mới ở dưới nữa!
            return;
        }
        //chuyen du lieu qua user va save vao db
        User user = userMapper.registerToUser(request);
        user = userRepository.save(user);

        // tao gio hang rong cho User moi
        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.save(cart);

        //tao va gui otp qua email
        String code = otpService.generateAndStoreOtp(request.getEmail());
        sendVerificationOtpOrThrow(request.getEmail(), code);
    }

    // xac thuc tai khoan
    @Transactional
    public void verify(VerifyRequest request){
        // Xac thuc tai khoan
        if(!otpService.validateOtp(request.getEmail(), request.getCode())){
            throw new WebErrorConfig(ErrorCode.INVALID_OTP_CODE);
        }

        // tim nguoi dung roi kich hoat tai khoan
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new WebErrorConfig(ErrorCode.USER_NOT_FOUND));
        user.setIsActive(true);
        user.setDeletedAt(null);

        //luu thong tin nguoi dug lai
        userRepository.save(user);
    }

    public TokenResponse login(LoginRequest request) {
        try {
            // 1. AuthenticationManager sẽ tự động làm các việc:
            //    - Tìm User theo email
            //    - Check Password (bằng passwordEncoder.matches)
            //    - Check User có Active không (qua hàm isEnabled() trong Entity User)
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // 2. Nếu chạy đến dòng này nghĩa là đăng nhập thành công
            // Set thông tin vào Context (để dùng cho các filter phía sau nếu cần)
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Lấy User ra (Principal chính là User entity của bạn vì class User implement UserDetails)
            User userAuth = (User) auth.getPrincipal();

            // 3. Sinh Token
            String accessToken = jwtUtils.generateAccessToken(userAuth);
            String refreshToken = jwtUtils.generateRefreshToken(userAuth);

            // 4. Lưu Refresh Token vào Redis (Cache)
            // Redis nên lưu key theo format: "refresh_token:{username}" để dễ quản lý
            redisService.saveRefreshTokenToRedis(userAuth.getEmail(), refreshToken, jwtUtils.getJwtLongExpiration());

            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

        } catch (DisabledException e) {
            // Bắt lỗi riêng user chưa kích hoạt (Active = false)
            throw new WebErrorConfig(ErrorCode.USER_NOT_ACTIVE);

        } catch (BadCredentialsException e) {
            // Bắt lỗi sai email hoặc pass
            throw new WebErrorConfig(ErrorCode.EMAIL_OR_PASSWORD_NOT_CORRECT);

        } catch (AuthenticationException e) {
            // Các lỗi khác của Spring Security
            throw new WebErrorConfig(ErrorCode.UNAUTHENTICATED);
        }
    }

    public void logout(String authHeader, LogoutRequest request){
        // 1. Xử lý Access Token (Blacklist)
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);

            // Lấy thời gian hết hạn để tính TTL
            Date expirationDate = jwtUtils.extractExpiration(accessToken);
            long nowMillis = System.currentTimeMillis();
            long ttlMillis = expirationDate.getTime() - nowMillis;

            // Chỉ blacklist nếu token còn sống
            if (ttlMillis > 0) {
                redisService.blackListToken(accessToken, ttlMillis);
            }
        }

        // 2. Xử lý Refresh Token (Xóa khỏi Redis) - QUAN TRỌNG
        // Nếu không có bước này, Logout coi như vô nghĩa
        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            redisService.deleteRefreshToken(currentEmail, request.getRefreshToken());
        }
    }

    // gui yeu cau quen mat khau
    public void forgotPassword(ForgotPasswordRequest request){
        // kiem tra xem nguoi dung co ton tai khong
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new WebErrorConfig(ErrorCode.USER_NOT_FOUND));

        if (!user.getIsActive() || user.getDeletedAt() != null) {
            throw new WebErrorConfig(ErrorCode.USER_NOT_ACTIVE); // Hoặc mã lỗi phù hợp
        }
        ensurePasswordLoginAccount(user);

        //sinh otp va luu vao redis
        String code = otpService.generateAndStoreOtp(request.getEmail());

        // gui email
        sendResetPasswordOtpOrThrow(user.getEmail(), code);
    }

    //thuc hien doi mat khau bang otp
    @Transactional
    public void resetPassword(ResetPasswordRequest request){
        // lay user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new WebErrorConfig(ErrorCode.USER_NOT_FOUND));
        ensurePasswordLoginAccount(user);

        // Xac thuc tai khoan
        if(!otpService.validateOtp(request.getEmail(), request.getOtp())){
            throw new WebErrorConfig(ErrorCode.INVALID_OTP_CODE);
        }

        //Update mat khau moi
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        redisService.deleteAllRefreshTokensOfUser(request.getEmail());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request){
        // Lấy User hiện tại từ SecurityContext (Người đang đăng nhập)
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail =  authentication.getName();

        System.out.println("DEBUG: Authentication Name is: " + currentEmail);

        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.USER_NOT_FOUND));
        ensurePasswordLoginAccount(currentUser);

        // Check mật khẩu cũ có đúng không
        if(!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())){
            throw new WebErrorConfig(ErrorCode.PASSWORD_NOT_CORRECT);
        }
        // Check mật khẩu mới và confirm có khớp nhau không
        if(!request.getNewPassword().equals(request.getConfirmationPassword())){
            throw new WebErrorConfig(ErrorCode.PASSWORD_NOT_MATCH);
        }
        // Lưu mật khẩu mới
        currentUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
        redisService.deleteAllRefreshTokensOfUser(currentUser.getEmail());
    }

    private void ensurePasswordLoginAccount(User user) {
        if (!StringUtils.hasText(user.getPasswordHash())) {
            throw new WebErrorConfig(ErrorCode.OAUTH_ACCOUNT_PASSWORD_NOT_ALLOWED);
        }
    }

    private void sendVerificationOtpOrThrow(String email, String code) {
        try {
            emailService.sendVerificationCode(email, code);
        } catch (MailException exception) {
            otpService.deleteOtp(email);
            throw new WebErrorConfig(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private void sendResetPasswordOtpOrThrow(String email, String code) {
        try {
            emailService.sendResetPasswordOtp(email, code);
        } catch (MailException exception) {
            otpService.deleteOtp(email);
            throw new WebErrorConfig(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        try {
            // 1. Lấy email (username) từ Refresh Token
            String userEmail = jwtUtils.extractEmail(requestRefreshToken);

            // 2. Tìm User trong Database
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new WebErrorConfig(ErrorCode.USER_NOT_FOUND));

            if (!user.getIsActive() || user.getDeletedAt() != null) {
                throw new WebErrorConfig(ErrorCode.USER_NOT_ACTIVE);
            }

            // 3. Kiểm tra Token có hợp lệ với chữ ký và thời hạn không
            if (!jwtUtils.validateToken(requestRefreshToken, user)) {
                throw new WebErrorConfig(ErrorCode.INVALID_REFRESH_TOKEN); // Thêm lỗi này vào Enum
            }

            // 4. Kiểm tra Refresh Token có tồn tại trong Redis không?
            // Tránh trường hợp user đã Logout nhưng hacker vẫn cầm Refresh Token cũ đem đi gọi API
            // (Giả định bạn có hàm getRefreshToken trong RedisService)
            if (!redisService.isRefreshTokenValid(userEmail, requestRefreshToken)) {
                throw new WebErrorConfig(ErrorCode.INVALID_REFRESH_TOKEN);
            }

            // 5. Cấp Access Token mới
            String newAccessToken = jwtUtils.generateAccessToken(user);

            // 6. Cấp Refresh Token mới (Refresh Token Rotation) - Rất quan trọng để bảo mật
            String newRefreshToken = jwtUtils.generateRefreshToken(user);

            // 7. Cập nhật lại Redis: Xóa cái cũ, lưu cái mới
            redisService.deleteRefreshToken(userEmail, requestRefreshToken);
            redisService.saveRefreshTokenToRedis(userEmail, newRefreshToken, jwtUtils.getJwtLongExpiration());

            // 8. Trả về cặp Token mới cho Frontend
            return TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .build();

        } catch (Exception e) {
            // Nếu token hết hạn (ExpiredJwtException) hoặc sai định dạng (MalformedJwtException)
            // Quá trình parse token của jwtUtils sẽ ném lỗi, ta bắt ở đây và ném lỗi nghiệp vụ
            throw new WebErrorConfig(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }
}
