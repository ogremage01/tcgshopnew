import axios from "axios"
import { useAuthStore } from "@/stores/auth-store"
import type { AuthResponseDto } from "@/types/user"
import { userFromAuthDto } from "@/types/user"
import { AxiosHeaders, type AxiosRequestConfig } from "axios"

const baseURL = process.env.NEXT_PUBLIC_API_URL ?? ""

let isRefreshing = false
let refreshPromise: Promise<string> | null = null
let isRedirectingToLogin = false

type RetryableRequestConfig = AxiosRequestConfig & { _retry?: boolean }

function isRefreshEndpoint(url?: string): boolean {
  return typeof url === "string" && url.includes("/api/auth/refresh")
}

function redirectToLoginOnce() {
  if (typeof window === "undefined" || isRedirectingToLogin) return
  const path = window.location.pathname
  if (path.endsWith("/login") || path.endsWith("/register")) return
  isRedirectingToLogin = true
  window.location.href = "/login"
}


/** axios 인스턴스. 인터셉터 등이 필요할 때 사용 */
export const apiClient = axios.create({
  baseURL,
  withCredentials: true,
})

/** 요청 시 Zustand에 저장된 JWT를 Authorization 헤더에 붙임 (클라이언트 전용). */
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token //Zustand에 저장된 JWT 가져오기
  if (token) { //JWT 있으면 Authorization 헤더에 붙임
    config.headers = config.headers ?? {};
    (config.headers as Record<string, string>).Authorization = `Bearer ${token}` //Authorization 헤더에 붙임
  }
  return config //config 반환
})

/**
 * FormData 업로드 시 boundary 없는 `multipart/form-data`만 Content-Type으로 넣으면
 * 서버에서 "no multipart boundary" 오류가 난다. 해당 헤더는 제거해 브라우저/axios가 boundary를 붙이게 함.
 */
function normalizeFormDataRequestConfig(config?: AxiosRequestConfig): AxiosRequestConfig | undefined {
  if (!config?.headers) return config
  const headers = AxiosHeaders.from(
    config.headers as ConstructorParameters<typeof AxiosHeaders>[0],
  )
  const ct = headers.get("Content-Type") ?? headers.get("content-type")
  if (typeof ct === "string" && ct.startsWith("multipart/form-data") && !ct.includes("boundary=")) {
    headers.delete("Content-Type")
    headers.delete("content-type")
  }
  return { ...config, headers }
}

/** 클라이언트 API 래퍼: 응답 body만 반환. */
export const api = {
  get: <T>(path: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> =>
    apiClient.get(path, { data: body, ...config }).then((res) => res.data),
  post: <T>(path: string, body?: any, config?: AxiosRequestConfig): Promise<T> =>
    apiClient.post(path, body, config).then((res) => res.data),
  /** multipart 업로드(엑셀·이미지 등). FormData 사용, Content-Type은 수동 지정하지 말 것. */
  postFormData: <T>(path: string, formData: FormData, config?: AxiosRequestConfig): Promise<T> =>
    apiClient.post(path, formData, normalizeFormDataRequestConfig(config)).then((res) => res.data),
  putFormData: <T>(path: string, formData: FormData, config?: AxiosRequestConfig): Promise<T> =>
    apiClient.put(path, formData, normalizeFormDataRequestConfig(config)).then((res) => res.data),
  put: <T>(path: string, body: unknown, config?: AxiosRequestConfig): Promise<T> =>
    apiClient.put(path, body, config).then((res) => res.data),
  delete: <T>(path: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> =>
    apiClient.delete(path, { data: body, ...config }).then((res) => res.data),
  patch: <T>(path: string, body: unknown, config?: AxiosRequestConfig): Promise<T> =>
    apiClient.patch(path, body, config).then((res) => res.data),
}

/** axios 4xx/5xx 에러에서 서버 메시지 추출. messageCode 우선(i18n 키), 없으면 message(문자열) */
export function getApiErrorMessage(err: unknown): string | undefined {
  //axios 에러인지||응답 데이터 없는지||응답 데이터 객체가 아닌지 확인
  if (!axios.isAxiosError(err) || !err.response?.data || typeof err.response.data !== "object") return undefined
  const data = err.response.data as { messageCode?: string; message?: string } //응답 데이터 타입 추론
  if (data.messageCode) return data.messageCode //messageCode 있으면 반환
  if (data.message) return data.message //message 있으면 반환
  return undefined //없으면 undefined 반환
}

// access token 만료시 refresh token 사용하여 재발급
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    //원본 요청 가져오기
    const originalRequest = error.config as RetryableRequestConfig | undefined
    if (!originalRequest) return Promise.reject(error)

    if (error.response?.status === 401 && !originalRequest._retry) {
      // refresh 요청 자체에서 401이 나면 재시도 루프/큐 대기를 만들지 않고 즉시 종료
      if (isRefreshEndpoint(originalRequest.url)) {
        useAuthStore.getState().clear()
        return Promise.reject(error)
      }

      //첫 번째 401 에러인지 확인
      originalRequest._retry = true

      if (!isRefreshing) { //refresh token 사용중인지 확인
        isRefreshing = true
        //refresh token 사용하여 새 access token 발급
        refreshPromise = axios
          .post<AuthResponseDto>(
            `${baseURL}/api/auth/refresh`,
            {},
            { withCredentials: true },
          )
          .then((res) => {
            const data = res.data
            if (!data?.token || !data?.user) {
              throw new Error("Token refresh response is missing token/user")
            }

            useAuthStore.getState().setAuth(data.token, userFromAuthDto(data.user))
            return data.token
          })
          .catch(error => {
            useAuthStore.getState().clear() //인증 정보 초기화
            redirectToLoginOnce()
            throw error
          })
          .finally(() => {
            isRefreshing = false //refresh token 사용 완료
            refreshPromise = null //refresh promise 초기화
          })
      }

      const token = await refreshPromise
      originalRequest.headers = originalRequest.headers ?? {}
        ; (originalRequest.headers as Record<string, string>).Authorization = `Bearer ${token}`
      return apiClient.request(originalRequest)
    }
    return Promise.reject(error) //원본 요청 거절
  }
)