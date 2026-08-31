package com.paulloweber.goldenraspberry.adapter.in.web;

import com.paulloweber.goldenraspberry.adapter.in.web.dto.ImportSummary;
import com.paulloweber.goldenraspberry.application.port.in.ImportMoviesUseCase;
import com.paulloweber.goldenraspberry.application.port.out.InvalidMovieDataException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/movies")
public class MovieImportController {
    private final ImportMoviesUseCase importMovies;

    public MovieImportController(ImportMoviesUseCase importMovies) {
        this.importMovies = importMovies;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportSummary importCsv(@RequestParam("file") MultipartFile file) {
        validateUpload(file);
        try (InputStream input = file.getInputStream()) {
            return ImportSummary.from(importMovies.importMovies(input));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded file", e);
        }
    }

    private void validateUpload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidMovieDataException(List.of("Uploaded file is empty"));
        }
        String filename = file.getOriginalFilename();
        if (filename != null && !filename.isBlank()
                && !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new InvalidMovieDataException(List.of("Only .csv files are accepted"));
        }
    }
}
