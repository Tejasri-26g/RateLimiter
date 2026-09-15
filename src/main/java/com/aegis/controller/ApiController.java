package com.aegis.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/resource")
    public ResponseEntity<?> getProtectedResource() {
        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Request successfully processed downstream by the target microservice."
        ));
    }
}