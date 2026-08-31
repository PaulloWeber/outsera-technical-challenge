package com.paulloweber.goldenraspberry.application.port.in;

/** What an import changed: how many movies, how many of them won, how many distinct producers. */
public record ImportResult(int movies, int winners, int producers) {
}
