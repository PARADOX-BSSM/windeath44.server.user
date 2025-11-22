package com.example.user.domain.exception;

import com.example.user.global.error.exception.ErrorCode;
import com.example.user.global.error.exception.GlobalException;

public class LastAdminNotDemotableException extends GlobalException {
    private LastAdminNotDemotableException() {
        super(ErrorCode.LAST_ADMIN_NOT_DEMOTABLE);
    }

    private static class Holder {
        private static final LastAdminNotDemotableException INSTANCE = new LastAdminNotDemotableException();
    }

    public static LastAdminNotDemotableException getInstance() {
        return Holder.INSTANCE;
    }
}
