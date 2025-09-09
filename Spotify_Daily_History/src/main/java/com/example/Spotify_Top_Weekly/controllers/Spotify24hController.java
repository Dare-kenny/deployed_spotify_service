package com.example.Spotify_Top_Weekly.controllers;

import com.example.Spotify_Top_Weekly.services.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.ZoneId;
import java.util.List;

@Controller
public class Spotify24hController {

    @Autowired
    private StatsService statsService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/recent_24h")
    public String recent24H(
            Model model,
            @AuthenticationPrincipal OAuth2User user,
            @RequestParam(name = "tz", required = false) String tz
    ) {
        // username (display_name -> id -> "User")
        String username = "User";
        Object dn = user.getAttribute("display_name");
        Object id = user.getAttribute("id");
        if (dn != null)      username = dn.toString();
        else if (id != null) username = id.toString();

        // zone (query param or default)
        ZoneId zone;
        try {
            zone = (tz != null && !tz.isBlank()) ? ZoneId.of(tz) : ZoneId.of("Africa/Lagos");
        } catch (Exception e) {
            zone = ZoneId.of("Africa/Lagos");
        }

        // TODAY from local midnight, aggregated with counts,
        // ordered by latest play (newest first)
        List<StatsService.DailyTrack> tracks = statsService.todayAggregated(user, zone);

        model.addAttribute("username", username);
        model.addAttribute("tz", zone.getId());
        model.addAttribute("tracks", tracks);
        return "recent_24h";
    }

}

