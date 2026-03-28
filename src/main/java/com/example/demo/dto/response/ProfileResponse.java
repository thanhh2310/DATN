package com.example.demo.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileResponse {
    String email;
    String firstName;
    String lastName;
    String phoneNumber;

}
