package com.example.Spotify_Top_Weekly.services;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;


@Service
public class StatsService {

    @Autowired
    private OAuth2AuthorizedClientService clientService;

    @Autowired
    private WebClient webClient;

    public List<DailyTrack> todayAggregated(OAuth2User user, ZoneId userZone) {
        Instant since = LocalDate.now(userZone).atStartOfDay(userZone).toInstant(); // starting from that day's morning
        return sinceAggregated(user, since);
    }


    public List<DailyTrack> sinceAggregated(OAuth2User user, Instant since) {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient("spotify", user.getName()); // get username of current logged-in user
        if (client == null || client.getAccessToken() == null) {
            return List.of();
        }
        String token = client.getAccessToken().getTokenValue();

        String beforeCursor = null;
        List<Map<String, Object>> allItems = new ArrayList<>();

        while (true) {
            StringBuilder url = new StringBuilder("/me/player/recently-played?limit=50");
            if (beforeCursor != null) url.append("&before=").append(beforeCursor);

            Map<String, Object> response = webClient.get()
                    .uri(url.toString())
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) break;
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.getOrDefault("items", List.of());
            if (items.isEmpty()) break;

            boolean crossedBoundary = false;
            for (Map<String, Object> item : items) {
                Object playedAtObj = item.get("played_at");
                if (playedAtObj == null) continue;
                Instant playedAt = Instant.parse(playedAtObj.toString());

                if (playedAt.isBefore(since)) {
                    crossedBoundary = true;
                    break;
                }
                allItems.add(item);
            }

            Map<String, Object> last = items.get(items.size() - 1);
            String lastPlayedAt = String.valueOf(last.get("played_at"));
            beforeCursor = String.valueOf(Instant.parse(lastPlayedAt).toEpochMilli());

            if (crossedBoundary) break;
        }

        // Aggregate by trackId → count + lastPlayedAt
        Map<String, Agg> aggMap = new HashMap<>();
        for (Map<String, Object> item : allItems) {
            Map<String, Object> track = (Map<String, Object>) item.get("track");
            if (track == null) continue;

            String trackId = Objects.toString(track.get("id"), "unknown");
            String trackName = Objects.toString(track.get("name"), "Unknown Track");
            List<Map<String, Object>> artists = (List<Map<String, Object>>) track.get("artists");
            String artistName = (artists != null && !artists.isEmpty())
                    ? Objects.toString(artists.get(0).get("name"), "Unknown Artist")
                    : "Unknown Artist";

            Instant playedAt = Instant.parse(item.get("played_at").toString());

            aggMap.compute(trackId, (k, v) -> {
                if (v == null) return new Agg(trackId, trackName, artistName, 1, playedAt);
                v.count++;
                if (playedAt.isAfter(v.latestPlayedAt)) v.latestPlayedAt = playedAt;
                return v;
            });
        }

        return aggMap.values().stream()
                .sorted(Comparator.comparing(Agg::getLatestPlayedAt).reversed()) // newest first
                .map(a -> DailyTrack.builder()
                        .trackId(a.trackId)
                        .trackName(a.trackName)
                        .artistName(a.artistName)
                        .count(a.count)
                        .latestPlayedAt(a.latestPlayedAt)
                        .build())
                .toList();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DailyTrack {
        private String trackId;
        private String trackName;
        private String artistName;
        private long count;
        private Instant latestPlayedAt;
    }

    private static class Agg {
        String trackId, trackName, artistName;
        long count;
        Instant latestPlayedAt;

        Agg(String id, String name, String artist, long c, Instant lp) {
            this.trackId = id; this.trackName = name; this.artistName = artist; this.count = c; this.latestPlayedAt = lp;
        }

        public Instant getLatestPlayedAt() { return latestPlayedAt; }
    }
}
