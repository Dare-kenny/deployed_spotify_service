package com.example.Spotify_Top_Weekly.dtos;

import lombok.Builder;

@Builder
public record TrackPlayCount(String trackId, String trackName, String artistName, long count) {

}
