import type { User } from "@/stores/auth-store"

/** 백엔드 UserResponseDto와 1:1 매핑. /me, /user/{id} 응답에서는 id 미포함 */
export interface UserResponseDto {
  id?: number
  name: string
  email: string
  // address: string;
  // phone: string;
  point: number
  createdAt: Date
  updatedAt: Date
  role: string
}

/** 회원가입 요청용 */
export interface RegisterRequestDto {
  name: string
  email: string
  // address?: string;
  // phone?: string;
  password: string
  passwordConfirm: string
}
/** 회원 수정 요청용 */
export interface UserUpdateRequestDto {
  name?: string
  email?: string
  // address?: string;
  // phone?: string;
  password?: string
  passwordConfirm?: string
}
/** 로그인 요청용. email과 password는 필수 */
export interface LoginRequestDto {
  email: string
  password: string
}
/** (관리자용)회원 관리 공통 필드 (응답/요청 공유) */
export interface UserManagementBase {
  id: string
  name: string
  email: string
  // address: string;
  // phone: string;
  role: string
  point: number
  userMemo: string
}
/** (관리자용)회원 관리 정보 응답용 */
export interface UserManagementResponseDto extends UserManagementBase {
  createdAt: Date
  userStatus: string
}
/** (관리자용)회원 관리 요청용 */
export interface UserManagementRequestDto extends UserManagementBase {
  pointChange: number
  changeReason: string
  userStatus: string
}

/** 관리자용 포인트 로그 (실행자 상세 포함) */
export interface PointLogDto {
  id: number
  userId: number
  userName: string
  changedPoint: number
  beforePoint: number
  afterPoint: number
  changeReason: string
  actorType?: "ADMIN" | "SYSTEM" | "USER"
  actorDetail?: string
  executor: string
  actionDate: string
  isSuccess: boolean
}

/** 마이페이지용 포인트 로그 (실행자 마스킹) */
export interface PointLogUserDto {
  id: number
  changedPoint: number
  beforePoint: number
  afterPoint: number
  changeReason: string
  executorDisplay: string
  actionDate: string
  isSuccess: boolean
}

/** 로그인·리프레시 응답의 사용자 요약(백엔드 AuthUserDto와 동일) */
export interface AuthUserDto {
  publicId: string
  email: string
  name: string
  role: string
  point: number
}

/** (사용자용)로그인·토큰 갱신 응답 */
export interface AuthResponseDto {
  token: string
  user: AuthUserDto
}

export function userFromAuthDto(d: AuthUserDto): User {
  return {
    id: d.publicId,
    email: d.email,
    name: d.name ?? d.email,
    role: d.role ?? "ROLE_USER",
    point: Number(d.point) ?? 0,
  }
}