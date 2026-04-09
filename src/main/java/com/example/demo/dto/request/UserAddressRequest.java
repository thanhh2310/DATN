package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAddressRequest {

    @NotBlank(message = "Địa chỉ (dòng 1) không được để trống")
    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    String addressLine1;

    @Size(max = 255, message = "Địa chỉ (dòng 2) không được vượt quá 255 ký tự")
    String addressLine2;

    @NotBlank(message = "Thành phố không được để trống")
    @Size(max = 100, message = "Tên thành phố không được vượt quá 100 ký tự")
    String city;

    @NotBlank(message = "Tỉnh/Thành/Bang không được để trống")
    @Size(max = 100, message = "Tên Tỉnh/Thành/Bang không được vượt quá 100 ký tự")
    String state;

    @NotBlank(message = "Quốc gia không được để trống")
    @Size(max = 100, message = "Tên quốc gia không được vượt quá 100 ký tự")
    String country;

    @NotBlank(message = "Mã bưu chính không được để trống")
    @Pattern(regexp = "^[A-Za-z0-9\\s-]{3,20}$", message = "Mã bưu chính không hợp lệ")
    String postalCode;

    Boolean isDefault;
}