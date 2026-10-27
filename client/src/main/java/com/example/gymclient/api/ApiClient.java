package com.example.gymclient.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Prosty klient REST API serwera GymManager (JSON + token JWT). */
public class ApiClient {

    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private String token;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void logout() {
        this.token = null;
    }

    public <T> T get(String path, Class<T> type) {
        return read(send(request(path).GET(), "application/json"), type);
    }

    public <T> T get(String path, TypeReference<T> type) {
        String body = send(request(path).GET(), "application/json");
        try {
            return mapper.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw new ApiException(0, "Niepoprawna odpowiedź serwera: " + e.getOriginalMessage());
        }
    }

    /** Pobiera surową treść (np. plik eksportu XML/JSON). */
    public String getText(String path) {
        return send(request(path).GET(), "*/*");
    }

    public <T> T post(String path, Object body, Class<T> type) {
        return read(send(request(path).POST(json(body)).header("Content-Type", "application/json"), "application/json"), type);
    }

    public void post(String path, Object body) {
        send(request(path).POST(json(body)).header("Content-Type", "application/json"), "application/json");
    }

    /** Wysyła surową treść z podanym Content-Type (import plików JSON/XML). */
    public <T> T postText(String path, String content, String contentType, Class<T> type) {
        var builder = request(path)
                .POST(HttpRequest.BodyPublishers.ofString(content, StandardCharsets.UTF_8))
                .header("Content-Type", contentType + "; charset=UTF-8");
        return read(send(builder, "application/json"), type);
    }

    public <T> T put(String path, Object body, Class<T> type) {
        return read(send(request(path).PUT(json(body)).header("Content-Type", "application/json"), "application/json"), type);
    }

    public void delete(String path) {
        send(request(path).DELETE(), "application/json");
    }

    private HttpRequest.Builder request(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private HttpRequest.BodyPublisher json(Object body) {
        try {
            return HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String send(HttpRequest.Builder builder, String accept) {
        try {
            HttpResponse<String> response = http.send(builder.header("Accept", accept).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new ApiException(response.statusCode(), errorMessage(response));
            }
            return response.body();
        } catch (ConnectException e) {
            throw new ApiException(0, "Brak połączenia z serwerem " + baseUrl + ". Czy backend jest uruchomiony?");
        } catch (IOException e) {
            throw new ApiException(0, "Błąd komunikacji z serwerem: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Przerwano żądanie");
        }
    }

    private <T> T read(String body, Class<T> type) {
        if (type == Void.class || body == null || body.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw new ApiException(0, "Niepoprawna odpowiedź serwera: " + e.getOriginalMessage());
        }
    }

    private String errorMessage(HttpResponse<String> response) {
        try {
            JsonNode node = mapper.readTree(response.body());
            StringBuilder sb = new StringBuilder(node.path("message").asText("Błąd " + response.statusCode()));
            JsonNode fields = node.path("fieldErrors");
            if (fields.isObject() && fields.size() > 0) {
                fields.fields().forEachRemaining(f -> sb.append("\n• ").append(f.getKey()).append(": ").append(f.getValue().asText()));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Błąd " + response.statusCode();
        }
    }
}
