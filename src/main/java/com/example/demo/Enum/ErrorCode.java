package com.example.demo.Enum;

import lombok.Getter;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND(101, "User not found"),
    USER_AlREADY_EXISTED(102,"User have already existed" ),
    USER_IS_DELETED(103, "User is deleted" ),
    USER_AlREADY_ACTIVE(104, "User have already active" ),
    ROLE_ALREADY_EXISTED(201,"Role have already existed" ),
    UNAUTHORIZED_ACTION(105, "Unauthorize action" ),
    ROLE_NOT_FOUND(202, " Role not found" ),
    INVALID_OTP_CODE(9999,"Invalid otp code" ),
    USER_NOT_ACTIVE(103,"User is not active" ),
    PASSWORD_NOT_CORRECT(104,"Password is not correct" ),
    UNAUTHENTICATED(9998,"Unauthenticated" ),
    EMAIL_OR_PASSWORD_NOT_CORRECT(105, "Email or password is not correct"),
    PASSWORD_NOT_MATCH(106, "Confirm password is not true" ),
    BRAND_ALREADY_EXISTED(301,"Brand have already existed" ),
    BRAND_NOT_FOUND(302, "Brand not found" ),
    CATEGORY_NOT_FOUND(304,"Category not found" ),
    CATEGORY_ALREADY_EXISTED(305,"Category have already existed" ),
    INVALID_CATEGORY_PARENT(306,"Category cannot be its own parent" ),
    CATEGORY_CYCLE_DETECTED(309, "Category cycle detected" ),
    ATTRIBUTE_NOT_FOUND(307, "Attribute not found"),
    ATTRIBUTE_ALREADY_EXISTED(308, "Attribute have already existed"),
    INVALID_ATTRIBUTE_VALUE(310, "Invalid attribute value"),
    ATTRIBUTE_VALUE_NOT_FOUND(311, "Attribute value not found"),
    PRODUCT_NOT_FOUND(401, "Product not found" ),
    ATTRIBUTE_NOT_ALLOWED_FOR_CATEGORY(312, "Attribute not allowed for category"),
    SKU_CODE_ALREADY_EXISTED(313, "Sku code already existed"),
    CART_NOT_FOUND(501, "Cart not found"),
    CART_ITEM_NOT_FOUND(502, "Cart item not found"),
    UNAUTHORIZED_CART_ACCESS(503, "You don't have permission to modify this cart item"),
    SKU_NOT_FOUND(504, "Product SKU not found"),
    SKU_INACTIVE(505, "Product SKU is no longer available"),
    INSUFFICIENT_STOCK(506, "Not enough stock available"),
    INVALID_QUANTITY(507, "Quantity must be greater than 0"),
    MISSING_CART_IDENTIFIER(508, "Must provide either UserId or SessionId"),
    ADDRESS_NOT_FOUND(509,"Address not found" ),
    SHIPPING_METHOD_NOT_FOUND(510,"Shipping method not found" ),
    COUPON_NOT_FOUND(511, "Coupon not found" ),
    INVALID_DATE_RANGE(512, "Invalid date range" ),
    COUPON_INACTIVE(513, "Coupon inactive" ),
    COUPON_NOT_STARTED(514,"Coupon not started" ),
    COUPON_EXPIRED(515,"Coupon expired" ),
    COUPON_USAGE_EXCEEDED(516, "Coupon usage exceeded"),
    COUPON_NOT_APPLICABLE(517, "Coupon not applicable"),
    SHIPPING_METHOD_ALREADY_EXISTED(518,"Shipping method already existed"),
    COUPON_ALREADY_EXISTED(519, "Coupon already existed"),
    CART_IS_EMPTY(520, "Cart is empty"),
    PAYMENT_METHOD_NOT_FOUND(521, "Payment method not found"),
    PAYMENT_METHOD_ALREADY_EXISTED(522,"payment method have already existed" );

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private final int code;
    private final String message;
}
