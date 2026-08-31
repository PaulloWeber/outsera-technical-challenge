package com.paulloweber.goldenraspberry.adapter.in.web;

import com.paulloweber.goldenraspberry.application.port.out.InvalidMovieDataException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    /**
     * The message stays format-agnostic on purpose: {@code InvalidMovieDataException} is
     * the contract of {@code MovieParserPort}, and a future adapter reading another format
     * would raise the very same exception. What was actually wrong is in {@code errors}.
     */
    @ExceptionHandler(InvalidMovieDataException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInvalidMovieData(InvalidMovieDataException exception) {
        return Map.of(
                "message", "Invalid movie data",
                "errors", List.copyOf(exception.getErrors()));
    }
}
