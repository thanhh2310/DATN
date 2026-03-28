package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {

    Integer id;
    String email;
    String firstName;
    String lastName;
    String phoneNumber;
    Boolean isActive;

    Set<String> roles;

    LocalDateTime createdAt;

}
