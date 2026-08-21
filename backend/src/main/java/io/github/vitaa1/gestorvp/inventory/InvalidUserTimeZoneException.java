package io.github.vitaa1.gestorvp.inventory;

final class InvalidUserTimeZoneException extends RuntimeException {

	InvalidUserTimeZoneException(Throwable cause) {
		super("O fuso horário informado é inválido.", cause);
	}
}
