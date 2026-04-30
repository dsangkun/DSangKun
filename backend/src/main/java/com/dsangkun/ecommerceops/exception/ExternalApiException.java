package com.dsangkun.ecommerceops.exception;

public class ExternalApiException extends BizException {

    public ExternalApiException(String code, String message) {
        super(code, message);
    }
}
