import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";
import type { CheckoutSessionResponse, CreateCheckoutSessionRequest, UpdateAddressRequest, UpdateQuantityRequest, UpdateVoucherRequest, UpdateBuyerNoteRequest } from "../types/checkout";

export const createCheckoutSession = (data: CreateCheckoutSessionRequest) =>
  axiosInstance.post<ApiResponse<CheckoutSessionResponse>>("/api/v1/checkout", data);

export const getCheckoutSession = (sessionId: string) =>
  axiosInstance.get<ApiResponse<CheckoutSessionResponse>>(`/api/v1/checkout/${sessionId}`);

export const updateCheckoutAddress = (sessionId: string, data: UpdateAddressRequest) =>
  axiosInstance.patch<ApiResponse<CheckoutSessionResponse>>(`/api/v1/checkout/${sessionId}/address`, data);

export const updateCheckoutQuantity = (sessionId: string, data: UpdateQuantityRequest) =>
  axiosInstance.patch<ApiResponse<CheckoutSessionResponse>>(`/api/v1/checkout/${sessionId}/quantity`, data);

export const updateCheckoutSellerVoucher = (sessionId: string, sellerId: string, data: UpdateVoucherRequest) =>
  axiosInstance.patch<ApiResponse<CheckoutSessionResponse>>(`/api/v1/checkout/${sessionId}/sellers/${sellerId}/voucher`, data);

export const updateCheckoutPlatformVoucher = (sessionId: string, data: UpdateVoucherRequest) =>
  axiosInstance.patch<ApiResponse<CheckoutSessionResponse>>(`/api/v1/checkout/${sessionId}/voucher`, data);

export const updateCheckoutBuyerNote = (sessionId: string, sellerId: string, data: UpdateBuyerNoteRequest) =>
  axiosInstance.patch<ApiResponse<CheckoutSessionResponse>>(`/api/v1/checkout/${sessionId}/sellers/${sellerId}/buyer-note`, data);
