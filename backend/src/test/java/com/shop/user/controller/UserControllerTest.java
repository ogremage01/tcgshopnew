package com.shop.user.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.testconfig.UserWebMvcTestConfig;
import com.shop.common.exception.GlobalExceptionHandler;
import com.shop.security.JwtUtil;
import com.shop.user.service.user.UserService;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ContextConfiguration(classes = UserWebMvcTestConfig.class)
class UserControllerTest {

    private static final String USER_PUBLIC_ID = "user-public-id";
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        USER_PUBLIC_ID,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    @DisplayName("change-name unauthorized returns 401 messageCode")
    void changeName_unauthorized_returns401MessageCode() throws Exception {
        mockMvc.perform(post("/api/user/change-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("name", "Tester"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.messageCode").value("user.error.unauthorized"));

        verify(userService, never()).changeName(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("change-name blank name returns 400 message")
    void changeName_blankName_returns400Message() throws Exception {
        when(userService.findUserIdByPublicId(USER_PUBLIC_ID)).thenReturn(Optional.of(USER_ID));
        authenticateAsUser();

        mockMvc.perform(post("/api/user/change-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("name", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Name is required"));

        verify(userService, never()).changeName(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("change-password accepts legacy password field")
    void changePassword_acceptsLegacyPasswordField() throws Exception {
        when(userService.findUserIdByPublicId(USER_PUBLIC_ID)).thenReturn(Optional.of(USER_ID));
        doNothing().when(userService).changePassword(USER_ID, "old-password", "new-password");
        authenticateAsUser();

        mockMvc.perform(post("/api/user/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                        "currentPassword", "old-password",
                        "password", "new-password",
                        "passwordConfirm", "new-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed"));

        verify(userService).changePassword(USER_ID, "old-password", "new-password");
    }

    @Test
    @DisplayName("change-password mismatch returns 400 messageCode")
    void changePassword_mismatch_returns400MessageCode() throws Exception {
        when(userService.findUserIdByPublicId(USER_PUBLIC_ID)).thenReturn(Optional.of(USER_ID));
        authenticateAsUser();

        mockMvc.perform(post("/api/user/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                        "currentPassword", "old-password",
                        "newPassword", "new-password",
                        "passwordConfirm", "different-password"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messageCode").value("user.error.passwordNotMatch"));

        verify(userService, never()).changePassword(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}
