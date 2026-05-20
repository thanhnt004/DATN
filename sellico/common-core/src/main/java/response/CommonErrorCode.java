package response;

public enum CommonErrorCode implements BaseErrorCode {

    // Lỗi 500: Lỗi hệ thống không lường trước được
    UNCATEGORIZED_EXCEPTION("UNCATEGORIZED_EXCEPTION", "Uncategorized error", 500),

    // Lỗi 400: Request gửi lên bị sai cú pháp, thiếu trường bắt buộc chung chung
    INVALID_KEY("INVALID_KEY", "Uncategorized error", 400),

    // Lỗi 401: Token không hợp lệ hoặc hết hạn (Dùng chung cho các filter)
    UNAUTHENTICATED("UNAUTHENTICATED", "Unauthenticated", 401),

    // Lỗi 403: Không có quyền truy cập
    UNAUTHORIZED("UNAUTHORIZED", "You do not have permission", 403),
    BAD_REQUEST("BAD_REQUEST", "Bad request", 400);
    private final String code;
    private final String message;
    private final int statusCode;

    CommonErrorCode(String code, String message, int statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }
}