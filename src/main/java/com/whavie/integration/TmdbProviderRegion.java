package com.whavie.integration;

import java.util.List;

public record TmdbProviderRegion(
        List<TmdbProvider> flatrate,
        List<TmdbProvider> rent,
        List<TmdbProvider> buy
) {
}
