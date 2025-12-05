package com.fuchs.distance;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
class DistanceController {

    private final GoogleApiService googleApiService;

    public DistanceController(GoogleApiService googleApiService) {
        this.googleApiService = googleApiService;
    }

    @GetMapping("/distance")
    public Map<String, Double> getDistance(@RequestParam String from, @RequestParam String to) {

        String cleanedFrom = from.trim();
        String cleanedTo = to.trim();
        // Додаємо Україну для кращої точності (якщо немає)
        if (!cleanedFrom.toLowerCase().contains("україна")) {
            cleanedFrom += ", Україна";
        }
        if (!cleanedTo.toLowerCase().contains("україна")) {
            cleanedTo += ", Україна";
        }

        double distance = googleApiService.getDistance(cleanedFrom, cleanedTo);
        return Collections.singletonMap("distance_km", distance);
    }
}
