package be.ae.springfactory.web;

import be.ae.springfactory.program.ProgramNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain exceptions to HTTP responses, so controllers keep one success path. */
@RestControllerAdvice
class ApiExceptionHandler {

  @ExceptionHandler(ProgramNotFoundException.class)
  ProblemDetail programNotFound(ProgramNotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }
}
