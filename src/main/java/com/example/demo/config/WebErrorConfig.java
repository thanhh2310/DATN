package com.example.demo.config;

import com.example.demo.Enum.ErrorCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebErrorConfig extends RuntimeException {
    ErrorCode errorCode;
    public WebErrorConfig(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
