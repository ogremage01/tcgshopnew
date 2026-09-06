package com.shop.testconfig;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Import;

import com.shop.auth.controller.AuthController;
import com.shop.common.exception.GlobalExceptionHandler;

@SpringBootConfiguration
@Import({AuthController.class, GlobalExceptionHandler.class})
public class AuthWebMvcTestConfig {
}
