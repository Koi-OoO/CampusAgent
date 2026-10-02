package com.campusagent.common.exception;

import com.campusagent.common.result.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常。message 由父类保存，通过 getMessage() 获取。
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BusinessException(ResultCode resultCode) {
        this(resultCode, resultCode.getMessage());
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
