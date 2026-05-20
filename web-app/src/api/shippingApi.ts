import axiosInstance from "./axiosInstance";
import type { ApiResponse } from "../types/auth";

export interface ShippingFeeRequest {
  sellerId?: string;
  serviceId?: number;
  serviceTypeId?: number;
  toDistrictId?: number;
  toWardCode?: string;
  fromDistrictId?: number;
  fromWardCode?: string;
  weight?: number;
  length?: number;
  width?: number;
  height?: number;
  insuranceValue?: number;
}

export interface GHNFeeResponse {
  total: number;
  serviceFee: number;
  insuranceFee: number;
  pickStationFee: number;
  couponValue: number;
  r2sFee: number;
}

export interface Province {
  ProvinceID: number;
  ProvinceName: string;
  Code: string;
}

export interface District {
  DistrictID: number;
  ProvinceID: number;
  DistrictName: string;
  Code: string;
}

export interface Ward {
  WardCode: string;
  DistrictID: number;
  WardName: string;
}

export const getProvinces = () =>
  axiosInstance.get<Province[]>("/api/v1/shipping/provinces");

export const getDistricts = (provinceId: number) =>
  axiosInstance.get<District[]>("/api/v1/shipping/districts", { params: { provinceId } });

export const getWards = (districtId: number) =>
  axiosInstance.get<Ward[]>("/api/v1/shipping/wards", { params: { districtId } });

export const calculateShippingFee = (data: ShippingFeeRequest) =>
  axiosInstance.post<ApiResponse<GHNFeeResponse>>("/api/v1/shipping/calculate-fee", data);
