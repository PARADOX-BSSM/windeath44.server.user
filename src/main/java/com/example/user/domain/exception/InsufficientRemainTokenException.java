package com.example.user.domain.exception;
import com.example.user.global.error.exception.ErrorCode;
import com.example.user.global.error.exception.GlobalException;

public class InsufficientRemainTokenException extends GlobalException {

    public InsufficientRemainTokenException() {
        super(ErrorCode.REMAIN_TOKEN_INSUFFICIENT);
    }

    private static class Holder {
        private static final InsufficientRemainTokenException INSTANCE = new InsufficientRemainTokenException();
    }
    public static InsufficientRemainTokenException getInstance() {
        return Holder.INSTANCE;
    }
}
