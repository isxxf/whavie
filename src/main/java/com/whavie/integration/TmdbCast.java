package com.whavie.integration;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbCast(
        String name,
        @JsonProperty("known_for_department") String knownForDepartment
) {
}
