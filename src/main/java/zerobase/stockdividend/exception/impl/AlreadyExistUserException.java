package zerobase.stockdividend.exception.impl;

import org.springframework.http.HttpStatus;
import zerobase.stockdividend.exception.AbstractException;

public class AlreadyExistUserException extends AbstractException {
    @Override
    public int getStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }

    @Override
    public String getMessage() {
        return "Already exist user";
    }
}
