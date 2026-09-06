package com.shop.user.service;

import com.shop.auth.refresh.repository.RefreshTokenRepository;
import com.shop.cart.repository.CartRepository;
import com.shop.checkout.repository.CheckoutDraftRepository;
import com.shop.log.user.repository.UserLogRepository;
import com.shop.user.dto.user.UserResponseDto;
import com.shop.user.entity.User;
import com.shop.user.exception.UserException;
import com.shop.user.repository.UserRepository;
import com.shop.user.service.user.UserServiceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserServiceImpl 단위 테스트 (회원 조회·탈퇴)
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.shop.log.point.service.PointLogService pointLogService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CheckoutDraftRepository checkoutDraftRepository;

    @Mock
    private UserLogRepository userLogRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2025, 1, 1, 12, 0);

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("존재하면 UserResponseDto로 반환한다")
        void returnsDtoWhenUserExists() {
            User user = User.builder()
                    .id(1L)
                    .publicId("01ARZ3NDKTSV0RRFFQ69G2FAVW")
                    .name("홍길동")
                    .email("user@example.com")
                    .createdAt(FIXED_TIME)
                    .updatedAt(FIXED_TIME)
                    .point(100L)
                    .role("ROLE_USER")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserResponseDto result = userService.getUserById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getPublicId()).isEqualTo("01ARZ3NDKTSV0RRFFQ69G2FAVW");
            assertThat(result.getName()).isEqualTo("홍길동");
            assertThat(result.getEmail()).isEqualTo("user@example.com");
            assertThat(result.getCreatedAt()).isEqualTo(FIXED_TIME);
            assertThat(result.getUpdatedAt()).isEqualTo(FIXED_TIME);
            assertThat(result.getPoint()).isEqualTo(100L);
            assertThat(result.getRole()).isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("없으면 null을 반환한다")
        void returnsNullWhenMissing() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            UserResponseDto result = userService.getUserById(99L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("연관 데이터 정리 후 users 행을 삭제한다")
        void hardDeletesUserAndRelatedData() {
            when(userRepository.existsById(5L)).thenReturn(true);
            when(cartRepository.findByUserId(5L)).thenReturn(Optional.empty());
            when(checkoutDraftRepository.findByUserId(5L)).thenReturn(Collections.emptyList());

            userService.withdraw(5L);

            verify(refreshTokenRepository).deleteByUserId(5L);
            verify(userLogRepository).deleteByUserId(5L);
            verify(userRepository).deleteById(5L);
        }

        @Test
        @DisplayName("회원이 없으면 USER_NOT_FOUND")
        void throwsWhenUserMissing() {
            when(userRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> userService.withdraw(99L))
                    .isInstanceOf(UserException.class)
                    .extracting(e -> ((UserException) e).getCode())
                    .isEqualTo(UserException.Code.USER_NOT_FOUND);
        }
    }
}
