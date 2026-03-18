package com.example.demo.Enum;

import lombok.Getter;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND(101, "User not found"),
    USER_AlREADY_EXISTED(102,"User have already existed" ),
    ROLE_ALREADY_EXISTED(201,"Role have already existed" ),
    ROLE_NOT_FOUND(202, " Role not found" ),
    INVALID_OTP_CODE(9999,"Invalid otp code" ),
    USER_NOT_ACTIVE(103,"User is not active" ),
    PASSWORD_NOT_CORRECT(104,"Password is not correct" ),
    UNAUTHENTICATED(9998,"Unauthenticated" ),
    EMAIL_OR_PASSWORD_NOT_CORRECT(105, "Email or password is not correct"),
    PASSWORD_NOT_MATCH(106, "Confirm password is not true" ),
    BRAND_ALREADY_EXISTS(301,"Brand have already existed" ),
    BRAND_NOT_FOUND(302, "Brand not found" ),
    CATEGORY_NOT_FOUND(304,"Category not found" ),
    CATEGORY_ALREADY_EXISTS(305,"Category have already existed" ),
    INVALID_PARENT(306,"Category cannot be its own parent" ),
    ATTRIBUTE_NOT_FOUND(307, "Attribute not found"),
    ATTRIBUTE_ALREADY_EXISTS(308, "Attribute have already existed");

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private final int code;
    private final String message;
}
