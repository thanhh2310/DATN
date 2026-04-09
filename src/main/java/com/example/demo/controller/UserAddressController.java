package com.example.demo.controller;

import com.example.demo.dto.request.UserAddressRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.UserAddressResponse;
import com.example.demo.service.Helper;
import com.example.demo.service.UserAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class UserAddressController {
    private final UserAddressService userAddressService;
    private final Helper helper;
    // CREATE
    @PostMapping
    public ApiResponse<UserAddressResponse> create(@Valid @RequestBody UserAddressRequest request) {
        return ApiResponse.<UserAddressResponse>builder()
                .code(200)
                .message("Create address success")
                .data(userAddressService.create(helper.getCurrentUserId(), request))
                .build();
    }

    // UPDATE
    @PutMapping("/{id}")
    public ApiResponse<UserAddressResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody UserAddressRequest request
    ) {

        return ApiResponse.<UserAddressResponse>builder()
                .code(200)
                .message("Update address success")
                .data(userAddressService.update(helper.getCurrentUserId(),id, request))
                .build();
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        userAddressService.delete(helper.getCurrentUserId(),id);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Delete address success")
                .build();
    }

    // GET ALL BY USER
    @GetMapping("/my-addresses")
    public ApiResponse<List<UserAddressResponse>> getByUser() {
        return ApiResponse.<List<UserAddressResponse>>builder()
                .code(200)
                .message("Get addresses success")
                .data(userAddressService.getByUser(helper.getCurrentUserId()))
                .build();
    }

    // GET DEFAULT
    @GetMapping("/my-default-address")
    public ApiResponse<UserAddressResponse> getDefault() {
        return ApiResponse.<UserAddressResponse>builder()
                .code(200)
                .message("Get default address success")
                .data(userAddressService.getDefault(helper.getCurrentUserId()))
                .build();
    }
}