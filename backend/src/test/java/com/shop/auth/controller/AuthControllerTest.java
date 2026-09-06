package com.shop.auth.controller;

import com.shop.auth.dto.RegisterRequestDto;
import com.shop.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.security.JwtUtil;
import com.shop.auth.service.AuthService;
import com.shop.user.exception.UserException;
import com.shop.testconfig.AuthWebMvcTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.shop.cart.service.CartService;

/**
 * 인증 API 테스트
 * POST http://localhost:8080/api/auth/register
 */
@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ContextConfiguration(classes = AuthWebMvcTestConfig.class)
class AuthControllerTest {

    private static final String REGISTER_URL = "/api/auth/register";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CartService cartService;

    private String validRegisterBody() throws Exception {
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setName("홍길동");
        dto.setEmail("user@example.com");
        dto.setPassword("Password1!");
        dto.setPasswordConfirm("Password1!");
        return objectMapper.writeValueAsString(dto);
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterApi {

        @Test
        @DisplayName("정상 요청 시 200과 message 반환")
        void register_returns200AndMessage() throws Exception {
            doNothing().when(authService).register(any(RegisterRequestDto.class));

            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRegisterBody()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Registered"));

            verify(authService).register(any(RegisterRequestDto.class));
        }

        @Test
        @DisplayName("이메일 중복 시 409와 messageCode 반환")
        void register_duplicateEmail_returns409() throws Exception {
            doThrow(new UserException(UserException.Code.DUPLICATE_EMAIL))
                    .when(authService).register(any(RegisterRequestDto.class));

            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRegisterBody()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.messageCode").value("user.error.duplicateEmail"));

            verify(authService).register(any(RegisterRequestDto.class));
        }

        @Test
        @DisplayName("비밀번호 규칙 위반 시 400과 messageCode 반환")
        void register_invalidPassword_returns400() throws Exception {
            doThrow(new UserException(UserException.Code.INVALID_PASSWORD))
                    .when(authService).register(any(RegisterRequestDto.class));

            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRegisterBody()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messageCode").value("user.error.invalidPassword"));

            verify(authService).register(any(RegisterRequestDto.class));
        }

        @Test
        @DisplayName("비밀번호 불일치 시 400과 messageCode 반환")
        void register_passwordMismatch_returns400() throws Exception {
            doThrow(new UserException(UserException.Code.PASSWORD_NOT_MATCH))
                    .when(authService).register(any(RegisterRequestDto.class));

            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRegisterBody()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messageCode").value("user.error.passwordNotMatch"));

            verify(authService).register(any(RegisterRequestDto.class));
        }
    }
}
