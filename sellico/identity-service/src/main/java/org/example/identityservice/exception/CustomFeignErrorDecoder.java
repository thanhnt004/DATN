package org.example.identityservice.exception;

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
            // 1. Đọc nội dung ErrorResponse từ Service con
            ErrorResponse externalError = objectMapper.readValue(bodyIs, ErrorResponse.class);

            // 2. Nếu là lỗi logic (4xx), tạo một BaseException với Code gốc
            if (response.status() >= 400 && response.status() < 500) {
                ExternalErrorCode dynamicCode = new ExternalErrorCode(
                        externalError.getErrorCode(), // Lấy chính xác "USER_ALREADY_EXISTS", "INVALID_TOKEN",...
                        externalError.getMessage(),
                        response.status()
                );

                // Ném ra một Exception kế thừa từ BaseException của bạn
                return new ExternalServiceException(dynamicCode);
            }
        } catch (Exception e) {
            log.error("Không thể giải mã lỗi từ service con", e);
        }

        return new Default().decode(methodKey, response);
    }
}