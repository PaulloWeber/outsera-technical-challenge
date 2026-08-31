package com.paulloweber.goldenraspberry.domain.model;

/**
 * The gap between two consecutive awards won by the same producer.
 */
public record AwardInterval(String producer, int interval, int previousWin, int followingWin) {
}
