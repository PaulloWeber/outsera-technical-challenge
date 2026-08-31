package com.paulloweber.goldenraspberry.adapter.in.web.dto;

import com.paulloweber.goldenraspberry.application.port.in.ImportResult;

public record ImportSummary(int movies, int winners, int producers) {
    public static ImportSummary from(ImportResult result) {
        return new ImportSummary(result.movies(), result.winners(), result.producers());
    }
}
