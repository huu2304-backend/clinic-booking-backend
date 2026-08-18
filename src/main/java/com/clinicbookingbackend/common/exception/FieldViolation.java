package com.clinicbookingbackend.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class FieldViolation {
    private String field;
    private String message;
}
