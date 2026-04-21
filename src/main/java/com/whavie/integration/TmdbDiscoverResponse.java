package com.whavie.integration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TmdbDiscoverResponse(
        @JsonProperty("total_pages") Integer total_pages,
        List<TmdbMovieBasic> results) {
}
