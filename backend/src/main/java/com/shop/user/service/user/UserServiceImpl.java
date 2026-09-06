package com.shop.user.service.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.admin.user.dto.UserManagementRequestDto;
import com.shop.admin.user.dto.UserManagementResponseDto;
import com.shop.admin.user.dto.UserMemoChangeRequest;
import com.shop.admin.user.dto.UserPointChangeRequest;
import com.shop.auth.refresh.repository.RefreshTokenRepository;
import com.shop.cart.repository.CartRepository;
import com.shop.checkout.repository.CheckoutDraftRepository;
import com.shop.log.point.dto.PointLogDto;
import com.shop.log.point.entity.PointLogActorType;
import com.shop.log.point.service.PointLogService;
import com.shop.log.user.repository.UserLogRepository;
import com.shop.user.dto.user.UserResponseDto;
import com.shop.user.entity.User;
import com.shop.user.exception.UserException;
import com.shop.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PointLogService pointLogService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CartRepository cartRepository;
    private final CheckoutDraftRepository checkoutDraftRepository;
    private final UserLogRepository userLogRepository;
    @Override
    public UserResponseDto getUserById(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return null;
        }
        return toUserResponseDto(userOpt.get());
    }

    @Override
    public UserResponseDto getUserByPublicId(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return null;
        }
        return userRepository.findByPublicId(publicId)
                .map(this::toUserResponseDto)
                .orElse(null);
    }

    @Override
    public Optional<Long> findUserIdByPublicId(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByPublicId(publicId).map(User::getId);
    }

    private UserResponseDto toUserResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .publicId(user.getPublicId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .point(user.getPoint())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    public void withdraw(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserException(UserException.Code.USER_NOT_FOUND);
        }
        refreshTokenRepository.deleteByUserId(userId);
        cartRepository.findByUserId(userId).ifPresent(cartRepository::delete);
        checkoutDraftRepository.findByUserId(userId).forEach(checkoutDraftRepository::delete);
        userLogRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
    }

    @Override
    @Transactional
    public void activateAccount(Long userId) {
        userRepository.updateUserStatus(userId, "ACTIVE", LocalDateTime.now());
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String password) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty())
            throw new UserException(UserException.Code.USER_NOT_FOUND);
        User user = userOpt.get();
        if (!passwordEncoder.matches(oldPassword, user.getPassword()))
            throw new UserException(UserException.Code.INVALID_PASSWORD);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    @Override
    public void changeName(Long userId, String name) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty())
            throw new UserException(UserException.Code.USER_NOT_FOUND);
        User user = userOpt.get();
        user.setName(name);
        userRepository.save(user);
    }

    // ------------------------------------------------
    // 관리자용 메서드
    // ------------------------------------------------

    @Override
    public Page<UserManagementResponseDto> getUserListForAdmin(Pageable pageable) {
        return userRepository.findAllForAdmin(pageable).map(user -> UserManagementResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .point(user.getPoint())
                .userStatus(user.getUserStatus())
                .userMemo(user.getUserMemo())
                .createdAt(user.getCreatedAt())
                .build());
    }

    @Override
    public Page<UserManagementResponseDto> getUserListByKeywordForAdmin(String keyword, Pageable pageable) {
        return userRepository.findAllByKeywordForAdmin(keyword, pageable)
                .map(user -> UserManagementResponseDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .point(user.getPoint())
                        .userStatus(user.getUserStatus())
                        .userMemo(user.getUserMemo())
                        .createdAt(user.getCreatedAt())
                        .build());
    }

    @Override
    @Transactional
    public void updateUserForAdmin(UserManagementRequestDto userManagementRequestDto) {
        Optional<User> userOpt = userRepository.findById(userManagementRequestDto.getId());
        if (userOpt.isEmpty())
            throw new UserException(UserException.Code.USER_NOT_FOUND);
        User user = userOpt.get();
        if (userManagementRequestDto.getName() != null)
            user.setName(userManagementRequestDto.getName());
        if (userManagementRequestDto.getRole() != null)
            user.setRole(userManagementRequestDto.getRole());
        if (userManagementRequestDto.getUserStatus() != null)
            user.setUserStatus(userManagementRequestDto.getUserStatus());
        if (userManagementRequestDto.getUserMemo() != null)
            user.setUserMemo(userManagementRequestDto.getUserMemo());
        String password = userManagementRequestDto.getPassword();
        String passwordConfirm = userManagementRequestDto.getPasswordConfirm();
        if (password != null && !password.isBlank()) {
            if (passwordConfirm == null || !password.equals(passwordConfirm))
                throw new UserException(UserException.Code.PASSWORD_NOT_MATCH);
            user.setPassword(passwordEncoder.encode(password));
        } else if (passwordConfirm != null && !passwordConfirm.isBlank()) {
            throw new UserException(UserException.Code.PASSWORD_NOT_MATCH);
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public Long updateUserPointForAdmin(UserPointChangeRequest userPointChangeRequest, String adminExecutorLabel) {
        Optional<User> userOpt = userRepository.findById(userPointChangeRequest.getId());
        if (userOpt.isEmpty())
            throw new UserException(UserException.Code.USER_NOT_FOUND);
        User user = userOpt.get();
        Long lastPoint = user.getPoint();
        user.setPoint(user.getPoint() + userPointChangeRequest.getPoint());
        Long newPoint = userRepository.save(user).getPoint();
        pointLogService.savePointLog(PointLogDto.builder()
                .userId(user.getId())
                .userName(user.getName())
                .changedPoint(userPointChangeRequest.getPoint())
                .beforePoint(lastPoint)
                .afterPoint(newPoint)
                .changeReason(userPointChangeRequest.getChangeReason())
                .actorType(PointLogActorType.ADMIN)
                .actorDetail(adminExecutorLabel)
                .actionDate(LocalDateTime.now())
                .isSuccess(true)
                .build());
        return user.getPoint();
    }

    @Override
    public List<PointLogDto> getRecentPointLogsForAdmin() {
        return pointLogService.getRecentPointLogsExcludeSystem();
    }

    @Override
    public List<PointLogDto> getRecentPointLogsIncludeSystemForAdmin() {
        return pointLogService.getRecentPointLogs();
    }

    @Override
    public Page<PointLogDto> getPointLogsForAdmin(String keyword, boolean includeSystem, Pageable pageable) {
        return pointLogService.getPointLogsForAdmin(keyword, includeSystem, pageable);
    }

    @Override
    public void updateUserMemoForAdmin(UserMemoChangeRequest userMemoChangeRequest) {
        Optional<User> userOpt = userRepository.findById(userMemoChangeRequest.getId());
        if (userOpt.isEmpty())
            throw new UserException(UserException.Code.USER_NOT_FOUND);
        User user = userOpt.get();
        user.setUserMemo(userMemoChangeRequest.getUserMemo());
        userRepository.save(user);
    }
}