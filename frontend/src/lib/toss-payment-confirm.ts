import axios from "axios"

import { api } from "@/lib/api.client"
import type { CheckoutConfirmResponse } from "@/types/checkout"

export const TOSS_CONFIRM_MAX_ATTEMPTS = 3
const RETRY_DELAY_MS = 1500

export function isRetriableTossConfirmError(error: unknown): boolean {
  if (axios.isCancel(error)) {
    return false
  }
  if (!axios.isAxiosError(error)) {
    return false
  }
  if (error.code === "ERR_CANCELED") {
    return false
  }
  if (!error.response) {
    return true
  }
  const status = error.response.status
  return status === 408 || status === 429 || status === 502 || status === 503 || status === 504
}

export async function confirmTossPayment(params: {
  paymentKey: string
  orderId: string
  amount: number
}): Promise<CheckoutConfirmResponse> {
  let lastError: unknown
  for (let attempt = 1; attempt <= TOSS_CONFIRM_MAX_ATTEMPTS; attempt++) {
    try {
      return await api.post<CheckoutConfirmResponse>("/api/checkout/toss/confirm", params)
    } catch (error) {
      lastError = error
      if (!isRetriableTossConfirmError(error) || attempt === TOSS_CONFIRM_MAX_ATTEMPTS) {
        throw error
      }
      await delay(RETRY_DELAY_MS * attempt)
    }
  }
  throw lastError
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => {
    window.setTimeout(resolve, ms)
  })
}
