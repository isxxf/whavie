package com.whavie.integration;

import java.util.Map;

public record TmdbWatchProviders (
        Map<String, TmdbProviderRegion> results
)
{}
