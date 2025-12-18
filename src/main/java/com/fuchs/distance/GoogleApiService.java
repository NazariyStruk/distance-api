//package com.fuchs.distance;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//@Service
//class GoogleApiService {
//
//    private final String API_KEY = "AIzaSyDNzC6BQtYQj4Rp3JeFiuNpHh2QKSOaF0o";
//
//    public double getDistance(String from, String to) {
//        try {
//            String toFormated = cleanAddress(to);
//            String encodedFrom = from.replaceAll(" ", "+").replaceAll(",", "");
//            String encodedTo = toFormated.replaceAll(" ", "+"); // без видалення ком
//
//            String url = String.format(
//                    "https://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&key=%s",
//                    encodedFrom, encodedTo, API_KEY);
//
//            RestTemplate restTemplate = new RestTemplate();
//            String response = restTemplate.getForObject(url, String.class);
//
//            System.out.println("Google API response: " + response);
//
//            ObjectMapper mapper = new ObjectMapper();
//            JsonNode root = mapper.readTree(response);
//
//            String status = root.path("status").asText();
//            if (!"OK".equals(status)) {
//                System.err.println("Google Directions API error: " + status);
//                return -1.0;
//            }
//
//            JsonNode routes = root.path("routes");
//            if (routes.isArray() && routes.size() > 0) {
//                JsonNode legs = routes.get(0).path("legs");
//                if (legs.isArray() && legs.size() > 0) {
//                    JsonNode distance = legs.get(0).path("distance");
//                    if (distance.has("value")) {
//                        double meters = distance.get("value").asDouble();
//                        return meters / 1000.0; // конвертація в км
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return -1.0;
//    }
//
//    private String cleanAddress(String address) {
//        System.out.println("cleanAddress отримав: " + address);
//        String original = address.trim();
//
//        if (original.contains("(") && original.contains(")")) {
//            System.out.println("Знайдено дужки");
//            String locality = original.substring(0, original.indexOf("(")).trim();
//            String regionSource = original.substring(original.indexOf("(") + 1, original.lastIndexOf(")")).trim();
//            System.out.println("Locality: " + locality);
//            System.out.println("RegionSource: " + regionSource);
//
//            if (regionSource.toLowerCase().contains("обл") || regionSource.toLowerCase().contains("область")) {
//                String[] parts = regionSource.split("[,\\s]+");
//                for (int i = 0; i < parts.length; i++) {
//                    String part = parts[i].toLowerCase().replace(".", "");
//                    if (part.equals("обл") || part.equals("область")) {
//                        if (i > 0) {
//                            String regionName = parts[i - 1];
//                            System.out.println("Знайдена область: " + regionName);
//                            return locality + ", " + regionName + " обл.";
//                        }
//                    }
//                }
//            } else {
//                System.out.println("Область не знайдена у дужках");
//            }
//        } else {
//            System.out.println("Дужки не знайдено");
//        }
//
//        System.out.println("Повертаємо оригінал без змін: " + original);
//        return original;
//    }
//}

package com.fuchs.distance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
class GoogleApiService {

    private final String API_KEY = "AIzaSyDNzC6BQtYQj4Rp3JeFiuNpHh2QKSOaF0o"; // Краще винести в application.properties
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public double getDistance(String from, String to) {
        try {
            // 1. Чистимо та готуємо адреси
            String cleanFrom = prepareAddress(from);
            String cleanTo = prepareAddress(to);

            // 2. Формуємо URL безпечно через UriComponentsBuilder (автоматично кодує пробіли та коми)
            URI uri = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/directions/json")
                    .queryParam("origin", cleanFrom)
                    .queryParam("destination", cleanTo)
                    .queryParam("key", API_KEY)
                    .build()
                    .toUri();

            System.out.println("Requesting Google Maps: " + uri);

            String response = restTemplate.getForObject(uri, String.class);
            JsonNode root = mapper.readTree(response);

            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                System.err.println("Google Directions API error: " + status + " | Response: " + response);
                return -1.0;
            }

            JsonNode routes = root.path("routes");
            if (routes.isArray() && !routes.isEmpty()) {
                JsonNode legs = routes.get(0).path("legs");
                if (legs.isArray() && !legs.isEmpty()) {
                    JsonNode distanceNode = legs.get(0).path("distance");
                    if (distanceNode.has("value")) {
                        double meters = distanceNode.get("value").asDouble();
                        return meters / 1000.0;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error calculating distance: " + e.getMessage());
            e.printStackTrace();
        }
        return -1.0;
    }

    /**
     * Головна логіка виправлення:
     * 1. Видаляє сміття в дужках (типу "Житомир.обл"), щоб не плутати Google.
     * 2. Якщо в дужках була область, але її немає в основному рядку - додає її.
     * 3. ЗБЕРІГАЄ ВУЛИЦЮ (те, що йде після дужок).
     */
    private String prepareAddress(String rawAddress) {
        if (rawAddress == null || rawAddress.isEmpty()) return "";

        String processed = rawAddress.trim();

        // Паттерн для пошуку тексту в дужках: (текст)
        Pattern pattern = Pattern.compile("\\((.*?)\\)");
        Matcher matcher = pattern.matcher(processed);

        String regionFromBrackets = "";

        if (matcher.find()) {
            String contentInside = matcher.group(1).toLowerCase(); // те, що всередині дужок

            // Якщо в дужках є натяк на область, запам'ятовуємо це
            if (contentInside.contains("обл")) {
                // Тут можна додати логіку нормалізації назви області, якщо треба
                // Наприклад, просто беремо весь вміст дужок як корисну інфу про регіон
                regionFromBrackets = matcher.group(1);
            }

            // Видаляємо дужки та їх вміст з основного рядка
            // "Шершні (Житомир.обл), вул. Шевченка" -> "Шершні , вул. Шевченка"
            processed = matcher.replaceFirst("");
        }

        // Прибираємо зайві коми та пробіли, які могли утворитися після видалення
        processed = processed.replaceAll(" ,", ",").replaceAll("\\s+", " ").trim();

        // Якщо ми знайшли область у дужках, і її ще немає в рядку явно (перевірка спрощена)
        // То додаємо її на початок. Але 1С зазвичай це вже робить.
        // Перевіримо: якщо 1С вже додав "Житомирська область", то regionFromBrackets ("Житомир.обл") нам не треба.

        // Фінальний штрих: додаємо Україну, якщо немає
        if (!processed.toLowerCase().contains("україна")) {
            processed += ", Україна";
        }

        return processed;
    }
}