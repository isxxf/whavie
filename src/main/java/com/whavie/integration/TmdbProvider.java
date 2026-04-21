package com.whavie.integration;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbProvider(
        @JsonProperty("provider_id") Integer providerId,
        @JsonProperty("provider_name") String providerName,
        @JsonProperty("logo_path") String logoPath
) {
}
