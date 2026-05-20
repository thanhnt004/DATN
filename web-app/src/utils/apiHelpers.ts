export function getApiErrorMessage(err: unknown, fallback = "Có lỗi xảy ra. Vui lòng thử lại."): string {
  if (err && typeof err === "object") {
    const e = err as {
      response?: { data?: { message?: unknown } };
      message?: unknown;
    };

    const apiMessage = e.response?.data?.message;
    if (typeof apiMessage === "string" && apiMessage.trim()) {
      return apiMessage;
    }

    if (typeof e.message === "string" && e.message.trim()) {
      return e.message;
    }
  }

  return fallback;
}
