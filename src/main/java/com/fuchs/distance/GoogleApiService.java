package com.fuchs.distance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
class GoogleApiService {

    private final String API_KEY = "AIzaSyDNzC6BQtYQj4Rp3JeFiuNpHh2QKSOaF0o";

    public double getDistance(String from, String to) {
        try {
            String toFormated = cleanAddress(to);
            String encodedFrom = from.replaceAll(" ", "+").replaceAll(",", "");
            String encodedTo = toFormated.replaceAll(" ", "+"); // без видалення ком

            String url = String.format(
                    "https://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&key=%s",
                    encodedFrom, encodedTo, API_KEY);

            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);

            System.out.println("Google API response: " + response);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                System.err.println("Google Directions API error: " + status);
                return -1.0;
            }

            JsonNode routes = root.path("routes");
            if (routes.isArray() && routes.size() > 0) {
                JsonNode legs = routes.get(0).path("legs");
                if (legs.isArray() && legs.size() > 0) {
                    JsonNode distance = legs.get(0).path("distance");
                    if (distance.has("value")) {
                        double meters = distance.get("value").asDouble();
                        return meters / 1000.0; // конвертація в км
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1.0;
    }

    private String cleanAddress(String address) {
        System.out.println("cleanAddress отримав: " + address);
        String original = address.trim();

        if (original.contains("(") && original.contains(")")) {
            System.out.println("Знайдено дужки");
            String locality = original.substring(0, original.indexOf("(")).trim();
            String regionSource = original.substring(original.indexOf("(") + 1, original.lastIndexOf(")")).trim();
            System.out.println("Locality: " + locality);
            System.out.println("RegionSource: " + regionSource);

            if (regionSource.toLowerCase().contains("обл") || regionSource.toLowerCase().contains("область")) {
                String[] parts = regionSource.split("[,\\s]+");
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i].toLowerCase().replace(".", "");
                    if (part.equals("обл") || part.equals("область")) {
                        if (i > 0) {
                            String regionName = parts[i - 1];
                            System.out.println("Знайдена область: " + regionName);
                            return locality + ", " + regionName + " обл.";
                        }
                    }
                }
            } else {
                System.out.println("Область не знайдена у дужках");
            }
        } else {
            System.out.println("Дужки не знайдено");
        }

        System.out.println("Повертаємо оригінал без змін: " + original);
        return original;
    }
}
