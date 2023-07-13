package com.pyonsnalcolor.exception;

import com.pyonsnalcolor.exception.model.ErrorCode;
import lombok.Getter;

@Getter
public class PyonsnalcolorBatchException extends PyonsnalcolorException {

    private final Exception originException;

    public PyonsnalcolorBatchException(ErrorCode errorCode, Exception originException) {
        super(errorCode);
        this.originException = originException;
    }
}