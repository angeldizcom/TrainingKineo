package com.app.kineo.controller;

import com.app.kineo.dto.UserRegistrationRequest;
import com.app.kineo.model.User;
import com.app.kineo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody UserRegistrationRequest request) {
        User createdUser = userService.registerUserAndGeneratePlan(request);
        return ResponseEntity.ok(createdUser);
    }
}
