package io.github.vitaa1.gestorvp.inventory;

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

	@ExceptionHandler(StockEntryNotFoundException.class)
	ProblemDetail handleNotFound(StockEntryNotFoundException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(InvalidWithdrawalException.class)
	ProblemDetail handleInvalidWithdrawal(InvalidWithdrawalException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
	}

	@ExceptionHandler(InvalidUserTimeZoneException.class)
	ProblemDetail handleInvalidUserTimeZone(InvalidUserTimeZoneException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
	}
}
