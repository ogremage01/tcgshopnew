import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

import {
  type CheckoutConfirmFailureResponse,
  type CheckoutConfirmResponse,
  type CheckoutDraftResponse,
  type CreateDraftResponse,
  type PatchCheckoutDraftRequest,
} from "@/types/checkout";
import { api } from "@/lib/api.client";

export const checkoutDraftQueryKey = (publicId: string | undefined) => ["checkoutDraft", publicId] as const;

export function useCheckoutDraftQuery(publicId: string | undefined) {
  return useQuery({
    queryKey: checkoutDraftQueryKey(publicId),
    queryFn: async () => api.get<CheckoutDraftResponse>(`/api/checkout/drafts/${publicId}`),
    enabled: !!publicId,
    retry: false,
  });
}

export function useCreateCheckoutDraftMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () => api.post<CreateDraftResponse>("/api/checkout/drafts"),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["cart"] });
    },
  });
}

export function usePatchCheckoutDraftMutation(publicId: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: PatchCheckoutDraftRequest) =>
      api.patch<CheckoutDraftResponse>(`/api/checkout/drafts/${publicId}`, body),
    onSuccess: (data) => {
      qc.setQueryData(checkoutDraftQueryKey(publicId), data);
    },
  });
}

export function useValidateCheckoutDraftMutation(publicId: string | undefined) {
  return useMutation({
    mutationFn: async () => api.post<void>(`/api/checkout/drafts/${publicId}/validate`),
  });
}

export function useConfirmCheckoutDraftMutation(publicId: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () =>
      api.post<CheckoutConfirmResponse>(`/api/checkout/drafts/${publicId}/confirm`),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["cart"] });
      void qc.invalidateQueries({ queryKey: checkoutDraftQueryKey(publicId) });
    },
  });
}

export function parseCheckoutConflict(err: unknown): CheckoutConfirmFailureResponse | undefined {
  if (!axios.isAxiosError(err)) return undefined;
  const status = err.response?.status;
  if (status !== 409 && status !== 410 && status !== 400) return undefined;
  const data = err.response?.data;
  if (data && typeof data === "object" && "code" in data) {
    return data as CheckoutConfirmFailureResponse;
  }
  return undefined;
}
