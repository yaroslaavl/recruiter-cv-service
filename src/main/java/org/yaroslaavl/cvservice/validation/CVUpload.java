package org.yaroslaavl.cvservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(value = RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CVUploadValidator.class)
public @interface CVUpload {

    String message() default "Upload error";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
