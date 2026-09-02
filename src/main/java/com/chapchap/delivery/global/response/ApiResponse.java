package com.chapchap.delivery.global.response;

public record ApiResponse<T>(
    String code
    , String message
    , T data
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
            "00"
            , "SUCCESS"
            , data
        );
    }

    public static ApiResponse<Void> error(
        String code
        , String message
    ) {
        return new ApiResponse<>(
            code
            , message
            , null
        );
    }
}
