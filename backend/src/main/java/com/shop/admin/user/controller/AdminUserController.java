package com.shop.admin.user.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.shop.admin.user.dto.UserManagementRequestDto;
import com.shop.admin.user.dto.UserManagementResponseDto;
import com.shop.admin.user.dto.UserMemoChangeRequest;
import com.shop.admin.user.dto.UserPointChangeRequest;
import com.shop.common.identity.AuthenticatedUserProvider;
import com.shop.common.page.dto.PageResponseDto;
import com.shop.log.point.dto.PointLogDto;
import com.shop.user.service.user.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    // 회원 관리 기능입니다. (회원 목록 조회, 상세 조회, 등록, 정보 수정, 삭제)

    private final UserService userService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    /**
     * 회원 목록을 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 회원 목록
     */
    @GetMapping("/list")
    public ResponseEntity<PageResponseDto<UserManagementResponseDto>> getUserListForAdmin(Pageable pageable) {
        Page<UserManagementResponseDto> page = userService.getUserListForAdmin(pageable);
        return ResponseEntity.ok(PageResponseDto.<UserManagementResponseDto>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .build());
    }

    /**
     * 회원 검색 목록을 조회합니다.
     *
     * @param keyword  검색 키워드
     * @param pageable 페이징 정보
     * @return 회원 검색 목록
     */
    @GetMapping("/search")
    public ResponseEntity<PageResponseDto<UserManagementResponseDto>> getUserListByKeywordForAdmin(String keyword,
            Pageable pageable) {
        Page<UserManagementResponseDto> page = userService.getUserListByKeywordForAdmin(keyword,
                pageable);
        return ResponseEntity.ok(PageResponseDto.<UserManagementResponseDto>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .build());
    }

    /**
     * 회원 정보를 수정합니다.
     *
     * @param userManagementRequestDto 회원 정보 수정 요청
     * @return 회원 정보 수정 결과
     */
    @PutMapping("/detail")
    public ResponseEntity<Void> updateUserForAdmin(@RequestBody UserManagementRequestDto userManagementRequestDto) {
        userService.updateUserForAdmin(userManagementRequestDto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/point")
    public ResponseEntity<Long> updateUserPointForAdmin(@RequestBody UserPointChangeRequest userPointChangeRequest) {
        String adminExecutorLabel = authenticatedUserProvider.resolveExecutorLabel()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Long point = userService.updateUserPointForAdmin(userPointChangeRequest, adminExecutorLabel);
        return ResponseEntity.ok(point);
    }

    @PutMapping("/memo")
    public ResponseEntity<Void> updateUserMemoForAdmin(@RequestBody UserMemoChangeRequest userMemoChangeRequest) {
        userService.updateUserMemoForAdmin(userMemoChangeRequest);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserForAdmin(@PathVariable Long id) {
        userService.withdraw(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/point-log/recent")
    public ResponseEntity<List<PointLogDto>> getRecentPointLogsForAdmin() {
        return ResponseEntity.ok(userService.getRecentPointLogsForAdmin());
    }

    @GetMapping("/point-log/recent/include-system")
    public ResponseEntity<List<PointLogDto>> getRecentPointLogsIncludeSystemForAdmin() {
        return ResponseEntity.ok(userService.getRecentPointLogsIncludeSystemForAdmin());
    }

    @GetMapping("/point-log")
    public ResponseEntity<PageResponseDto<PointLogDto>> getPointLogsForAdmin(String keyword, boolean includeSystem,
            Pageable pageable) {
        Page<PointLogDto> page = userService.getPointLogsForAdmin(keyword, includeSystem, pageable);
        return ResponseEntity.ok(PageResponseDto.<PointLogDto>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .build());
    }

    // TODO: 회원 등록

}
