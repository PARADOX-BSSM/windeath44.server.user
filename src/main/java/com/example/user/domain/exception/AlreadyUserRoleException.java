package com.example.user.domain.exception;

import com.example.user.global.error.exception.ErrorCode;
import com.example.user.global.error.exception.GlobalException;

public class AlreadyUserRoleException extends GlobalException {
    private AlreadyUserRoleException() {
        super(ErrorCode.ROLE_ALREADY_USER);
    }

    private static class Holder {
        private static final AlreadyUserRoleException INSTANCE = new AlreadyUserRoleException();
    }

    public static AlreadyUserRoleException getInstance() {
        return Holder.INSTANCE;
    }
}
