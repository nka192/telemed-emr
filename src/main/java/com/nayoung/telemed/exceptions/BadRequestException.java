package com.nayoung.telemed.exceptions;

public class BadRequestException extends RuntimeException{
    public BadRequestException(String ex) {
        super(ex);
    }
}
