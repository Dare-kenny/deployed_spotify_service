package com.example.Spotify_Top_Weekly.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SpotifyClientConfiguration {

    @Bean
    public WebClient webClient(WebClient.Builder builder){
        return builder.baseUrl("https://api.spotify.com/v1").build();
    }
}
