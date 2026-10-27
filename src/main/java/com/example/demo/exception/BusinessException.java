package com.example.demo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Naruszenie reguły biznesowej (np. brak miejsc na zajęciach) → domyślnie HTTP 409. */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(HttpStatus.CONFLICT, message);
    }

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
