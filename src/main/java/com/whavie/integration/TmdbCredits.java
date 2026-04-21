package com.whavie.integration;

import java.util.List;

public record TmdbCredits(List<TmdbCast> cast, List<TmdbCrew> crew) {
}
