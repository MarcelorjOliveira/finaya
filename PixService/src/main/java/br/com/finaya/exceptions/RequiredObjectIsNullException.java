package br.com.finaya.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RequiredObjectIsNullException extends RuntimeException {

	public RequiredObjectIsNullException(String string) {
		super(string);
	}
	
	public RequiredObjectIsNullException() {
		super("It is not allowed to persist a null object");
	}

	private static final long serialVersionUID = 1L;

}
