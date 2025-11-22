package com.example.user.domain.exception;

import com.example.user.global.error.exception.ErrorCode;
import com.example.user.global.error.exception.GlobalException;

public class SystemAccountNotDemotableException extends GlobalException {
    private SystemAccountNotDemotableException() {
        super(ErrorCode.SYSTEM_ACCOUNT_NOT_DEMOTABLE);
    }

    private static class Holder {
        private static final SystemAccountNotDemotableException INSTANCE = new SystemAccountNotDemotableException();
    }

    public static SystemAccountNotDemotableException getInstance() {
        return Holder.INSTANCE;
    }
}
