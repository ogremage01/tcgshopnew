package com.shop.testconfig;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Import;

import com.shop.common.exception.GlobalExceptionHandler;
import com.shop.user.controller.UserController;

@SpringBootConfiguration
@Import({UserController.class, GlobalExceptionHandler.class})
public class UserWebMvcTestConfig {
}
