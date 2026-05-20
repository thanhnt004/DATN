package com.example.sellerservice.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import response.ErrorResponse;

import java.io.InputStream;

@Slf4j
public class CustomFeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public Exception decode(String methodKey, Response response) {
        try (InputStream bodyIs = response.body().asInputStream()) {
            ErrorResponse externalError = objectMapper.readValue(bodyIs, ErrorResponse.class);

            if (response.status() >= 400 && response.status() < 500) {
                ExternalErrorCode dynamicCode = new ExternalErrorCode(
                        externalError.getErrorCode(),
                        externalError.getMessage(),
                        response.status()
                );
                return new ExternalServiceException(dynamicCode);
            }
        } catch (Exception e) {
            log.error("Không thể giải mã lỗi từ service con", e);
        }

        return new Default().decode(methodKey, response);
    }
}

