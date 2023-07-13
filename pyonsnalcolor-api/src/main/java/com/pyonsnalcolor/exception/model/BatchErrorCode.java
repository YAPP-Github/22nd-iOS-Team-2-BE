package com.pyonsnalcolor.exception.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Getter
@RequiredArgsConstructor
public enum BatchErrorCode implements ErrorCode{

    INVALID_ACCESS(BAD_REQUEST, "해당 URL이 유효하지 않습니다."),
    TIME_OUT(BAD_REQUEST, "연결 시간이 초과되었습니다."),
    IO_EXCEPTION(BAD_REQUEST, "페이지에 접근할 수 없습니다."),
    BATCH_UNAVAILABLE(BAD_REQUEST, "배치를 실행할 수 없습니다. 확인이 필요합니다."),
    INVALID_PRODUCT_TYPE(BAD_REQUEST, "상품 정보가 기존 형식과 다릅니다.");

    private final HttpStatus httpStatus;
    private final String message;
}