package com.whavie.integration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TmdbMovieDetails(
        Long id,
        String title,
        String overview,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("vote_average") Double voteAverage,
        List<TmdbGenre> genres,
        @JsonProperty("original_language") String originalLanguage,
        TmdbCredits credits,
        @JsonProperty("watch/providers")
        TmdbWatchProviders watchProviders
) {}
