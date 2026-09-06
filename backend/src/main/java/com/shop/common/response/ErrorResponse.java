package com.shop.common.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {
    private String message;
    private String errorCode;
    private String errorMessage;

}
