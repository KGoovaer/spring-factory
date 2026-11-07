package be.ae.springfactory.web;

import be.ae.springfactory.exercise.ExerciseNotFoundException;
import be.ae.springfactory.program.BlankProgramNameException;
import be.ae.springfactory.program.InvalidPrescriptionException;
import be.ae.springfactory.program.ProgramNotFoundException;
import be.ae.springfactory.program.SessionNotFoundException;
import be.ae.springfactory.program.WeekNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps domain exceptions to HTTP responses, so controllers keep one success path. Extends Spring's
 * handler so a request that fails validation gets the same ProblemDetail body as a domain failure.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler({
    ProgramNotFoundException.class,
    WeekNotFoundException.class,
    SessionNotFoundException.class,
    ExerciseNotFoundException.class
  })
  ProblemDetail notFound(RuntimeException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler({BlankProgramNameException.class, InvalidPrescriptionException.class})
  ProblemDetail badRequest(RuntimeException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
  }
}
