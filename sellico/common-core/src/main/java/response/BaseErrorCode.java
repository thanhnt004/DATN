package response;

public interface BaseErrorCode {
    String getCode();
    String getMessage();
    int getStatusCode(); // Dùng int thay vì HttpStatus
}