package org.example.userservice.service;

import org.example.userservice.dto.request.CreateAddressRequest;
import org.example.userservice.dto.request.UpdateAddressRequest;
import org.example.userservice.dto.response.AddressResponse;

import java.util.List;
import java.util.UUID;

public interface AddressService {

    /**
     * Get all addresses for a user
     */
    List<AddressResponse> getAddresses(UUID userId);

    /**
     * Get address by id
     */
    AddressResponse getAddress(UUID userId, UUID addressId);

    /**
     * Get default address for a user
     */
    AddressResponse getDefaultAddress(UUID userId);

    /**
     * Create new address
     */
    AddressResponse createAddress(UUID userId, CreateAddressRequest request);

    /**
     * Update address
     */
    AddressResponse updateAddress(UUID userId, UUID addressId, UpdateAddressRequest request);

    /**
     * Delete address
     */
    void deleteAddress(UUID userId, UUID addressId);

    /**
     * Set address as default
     */
    AddressResponse setDefaultAddress(UUID userId, UUID addressId);
}

