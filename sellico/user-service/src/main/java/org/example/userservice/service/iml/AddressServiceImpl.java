package org.example.userservice.service.iml;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.request.CreateAddressRequest;
import org.example.userservice.dto.request.UpdateAddressRequest;
import org.example.userservice.dto.response.AddressResponse;
import org.example.userservice.entity.User;
import org.example.userservice.entity.UserAddress;
import org.example.userservice.exception.AuthenException;
import org.example.userservice.exception.ErrorCode;
import org.example.userservice.repository.UserAddressRepository;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressServiceImpl implements AddressService {

    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;

    private static final int MAX_ADDRESSES = 10;

    @Override
    public List<AddressResponse> getAddresses(UUID userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AddressResponse getAddress(UUID userId, UUID addressId) {
        UserAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AuthenException(ErrorCode.ADDRESS_NOT_FOUND));
        return toResponse(address);
    }

    @Override
    public AddressResponse getDefaultAddress(UUID userId) {
        UserAddress address = addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new AuthenException(ErrorCode.ADDRESS_NOT_FOUND));
        return toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse createAddress(UUID userId, CreateAddressRequest request) {
        // Check max addresses limit
        long count = addressRepository.countByUserId(userId);
        if (count >= MAX_ADDRESSES) {
            throw new AuthenException(ErrorCode.MAX_ADDRESSES_REACHED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenException(ErrorCode.USER_NOT_FOUND));

        UserAddress address = new UserAddress();
        address.setId(UUID.randomUUID());
        address.setUser(user);
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setProvince(request.getProvince());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setAddressLine(request.getAddressLine());

        // If this is the first address or request asks for default, set as default
        boolean shouldBeDefault = request.getIsDefault() || count == 0;
        if (shouldBeDefault) {
            addressRepository.clearDefaultExcept(userId, address.getId());
            address.setIsDefault(true);
        } else {
            address.setIsDefault(false);
        }

        address = addressRepository.save(address);
        log.info("Created address {} for user {}", address.getId(), userId);

        return toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, UpdateAddressRequest request) {
        UserAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AuthenException(ErrorCode.ADDRESS_NOT_FOUND));

        if (request.getReceiverName() != null) {
            address.setReceiverName(request.getReceiverName());
        }
        if (request.getReceiverPhone() != null) {
            address.setReceiverPhone(request.getReceiverPhone());
        }
        if (request.getProvince() != null) {
            address.setProvince(request.getProvince());
        }
        if (request.getDistrict() != null) {
            address.setDistrict(request.getDistrict());
        }
        if (request.getWard() != null) {
            address.setWard(request.getWard());
        }
        if (request.getAddressLine() != null) {
            address.setAddressLine(request.getAddressLine());
        }

        address = addressRepository.save(address);
        log.info("Updated address {} for user {}", addressId, userId);

        return toResponse(address);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        UserAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AuthenException(ErrorCode.ADDRESS_NOT_FOUND));

        boolean wasDefault = address.getIsDefault();

        addressRepository.delete(address);
        log.info("Deleted address {} for user {}", addressId, userId);

        // If deleted address was default, set another one as default
        if (wasDefault) {
            List<UserAddress> remaining = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                UserAddress newDefault = remaining.get(0);
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
                log.info("Set address {} as new default for user {}", newDefault.getId(), userId);
            }
        }
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(UUID userId, UUID addressId) {
        UserAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AuthenException(ErrorCode.ADDRESS_NOT_FOUND));

        // Clear other defaults
        addressRepository.clearDefaultExcept(userId, addressId);

        address.setIsDefault(true);
        address = addressRepository.save(address);

        log.info("Set address {} as default for user {}", addressId, userId);
        return toResponse(address);
    }

    private AddressResponse toResponse(UserAddress address) {
        String fullAddress = String.format("%s, %s, %s, %s",
                address.getAddressLine(),
                address.getWard(),
                address.getDistrict(),
                address.getProvince());

        return AddressResponse.builder()
                .id(address.getId())
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .addressLine(address.getAddressLine())
                .fullAddress(fullAddress)
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}

