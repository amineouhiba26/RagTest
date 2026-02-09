package com.example.demo.controller;

import com.example.demo.model.UserTest;
import com.example.demo.repo.UserTestRepo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController

public class UserTestController {
    private UserTestRepo userTestRepo;

    @GetMapping("/user-tests")
    public List<UserTest> getUserTests() {
        return userTestRepo.findAll() ;
    }

    @PostMapping("/user-tests")
    public UserTest CreateUserTest(@RequestBody UserTest userTest) {
        return userTestRepo.save(userTest);
    }



}
