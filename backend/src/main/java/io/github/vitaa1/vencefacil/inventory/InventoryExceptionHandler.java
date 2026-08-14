package io.github.vitaa1.vencefacil.inventory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
class InventoryExceptionHandler {

	@ExceptionHandler(ConstraintViolationException.class)
	ProblemDetail handleConstraintViolation() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Os parâmetros informados são inválidos.");
	}
}
