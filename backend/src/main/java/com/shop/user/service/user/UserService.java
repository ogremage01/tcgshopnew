package com.shop.user.service.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.shop.admin.user.dto.UserManagementRequestDto;
import com.shop.admin.user.dto.UserManagementResponseDto;
import com.shop.admin.user.dto.UserMemoChangeRequest;
import com.shop.admin.user.dto.UserPointChangeRequest;
import com.shop.log.point.dto.PointLogDto;
import com.shop.user.dto.user.UserResponseDto;

public interface UserService {

    // 회원 서비스

    /**
     * 회원 정보 조회
     *
     * @param id 회원 ID
     * @return 회원 정보
     */
    UserResponseDto getUserById(Long id);

    /**
     * 공개 ULID로 회원 정보 조회
     *
     * @param publicId ULID
     * @return 회원 정보, 없으면 null
     */
    UserResponseDto getUserByPublicId(String publicId);

    /**
     * JWT subject(ULID)에 대응하는 내부 PK. 비관리 API에서 FK용으로 사용.
     *
     * @param publicId ULID
     * @return 내부 user id
     */
    Optional<Long> findUserIdByPublicId(String publicId);

    /**
     * 탈퇴(회원 행 및 세션·장바구니 등 개인 연관 데이터 물리 삭제)
     *
     * @param userId 회원 ID
     */
    void withdraw(Long userId);

    /**
     * 계정 활성화
     * 
     * @param userId 회원 ID
     */
    void activateAccount(Long userId);

    /**
     * 비밀번호 변경
     * 
     * @param userId   회원 ID
     * @param password 비밀번호
     */
    void changePassword(Long userId, String oldPassword, String password);

    /**
     * 이름 변경
     * 
     * @param userId 회원 ID
     * @param name   이름
     */
    void changeName(Long userId, String name);

    // ========== 관리자용 메서드 ==============

    /**
     * 전체 목록 조회
     * 
     * @param pageable 페이지 정보
     * @return 회원 목록
     */
    Page<UserManagementResponseDto> getUserListForAdmin(Pageable pageable);

    /**
     * 검색 목록 조회
     * 
     * @param keyword  검색 키워드
     * @param pageable 페이지 정보
     * @return 회원 검색 목록
     */
    Page<UserManagementResponseDto> getUserListByKeywordForAdmin(String keyword, Pageable pageable);

    /**
     * 회원 정보 수정
     * 
     * @param userManagementRequestDto 회원 정보 수정 요청
     */
    void updateUserForAdmin(UserManagementRequestDto userManagementRequestDto);

    /**
     * 회원 포인트 수정
     *
     * @param userPointChangeRequest 회원 포인트 수정 요청
     * @param adminExecutorLabel     변경 처리 관리자 표시명 (이름·이메일, JWT로 조회)
     */
    Long updateUserPointForAdmin(UserPointChangeRequest userPointChangeRequest, String adminExecutorLabel);

    /**
     * 회원 포인트 변경 로그 최신 10건 조회
     *
     * @return 포인트 변경 로그 목록
     */
    List<PointLogDto> getRecentPointLogsForAdmin();

    /**
     * 회원 포인트 변경 로그 최신 10건 조회 (SYSTEM 포함)
     *
     * @return 포인트 변경 로그 목록
     */
    List<PointLogDto> getRecentPointLogsIncludeSystemForAdmin();

    Page<PointLogDto> getPointLogsForAdmin(String keyword, boolean includeSystem, Pageable pageable);

    /**
     * 회원 메모 수정
     * 
     * @param userMemoChangeRequest 회원 메모 수정 요청
     */
    void updateUserMemoForAdmin(UserMemoChangeRequest userMemoChangeRequest);

}
