package com.techforge.desktop;

import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP Client for communicating with the TechForge Backend API.
 * Uses OkHttp for efficient HTTP operations.
 */
public class ApiClient {

    private static final String BASE_URL = "http://localhost:8080/api/v1";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson;

    // Store current user session
    private static String currentUserId;
    private static String currentUserRole;
    private static JsonObject currentUser;

    public ApiClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * Perform a POST request to the API.
     * @param endpoint API endpoint (e.g., "/auth/login")
     * @param jsonBody JSON string body
     * @return Response body as String
     * @throws IOException if request fails
     */
    public String post(String endpoint, String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request.Builder requestBuilder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(body);

        // Add auth header if user is logged in
        if (currentUserId != null) {
            requestBuilder.addHeader("X-Requester-ID", currentUserId);
        }

        Request request = requestBuilder.build();

        System.out.println("[ApiClient] POST " + request.url());

        try (Response response = client.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            System.out.println("[ApiClient] Response code=" + response.code() + " body=" + (respBody.length() > 200 ? respBody.substring(0, 200) + "..." : respBody));
            if (!response.isSuccessful()) {
                throw new IOException("API Error: " + response.code() + " - " + respBody);
            }
            return respBody;
        }
    }

    /**
     * Perform a GET request to the API.
     * @param endpoint API endpoint (e.g., "/projects")
     * @return Response body as String
     * @throws IOException if request fails
     */
    public String get(String endpoint) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .get();

        // Add auth header if user is logged in
        if (currentUserId != null) {
            requestBuilder.addHeader("X-Requester-ID", currentUserId);
        }

        Request request = requestBuilder.build();

        System.out.println("[ApiClient] GET " + request.url() + " X-Requester-ID=" + (currentUserId != null ? currentUserId : "(none)"));

        try (Response response = client.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            System.out.println("[ApiClient] Response code=" + response.code() + " body=" + (respBody.length() > 200 ? respBody.substring(0, 200) + "..." : respBody));
            if (!response.isSuccessful()) {
                throw new IOException("API Error: " + response.code() + " - " + respBody);
            }
            return respBody;
        }
    }

    /**
     * Perform a PUT request to the API.
     * @param endpoint API endpoint (e.g., "/auth/profile")
     * @param jsonBody JSON string body
     * @return Response body as String
     * @throws IOException if request fails
     */
    public String put(String endpoint, String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request.Builder requestBuilder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .put(body);

        // Add auth header if user is logged in
        if (currentUserId != null) {
            requestBuilder.addHeader("X-Requester-ID", currentUserId);
        }

        Request request = requestBuilder.build();

        System.out.println("[ApiClient] PUT " + request.url());

        try (Response response = client.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            System.out.println("[ApiClient] Response code=" + response.code() + " body=" + (respBody.length() > 200 ? respBody.substring(0, 200) + "..." : respBody));
            if (!response.isSuccessful()) {
                throw new IOException("API Error: " + response.code() + " - " + respBody);
            }
            return respBody;
        }
    }

    /**
     * Perform a DELETE request to the API.
     * @param endpoint API endpoint (e.g., "/tasks/123")
     * @throws IOException if request fails
     */
    public void delete(String endpoint) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .delete();

        // Add auth header if user is logged in
        if (currentUserId != null) {
            requestBuilder.addHeader("X-Requester-ID", currentUserId);
        }

        Request request = requestBuilder.build();

        System.out.println("[ApiClient] DELETE " + request.url());

        try (Response response = client.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            System.out.println("[ApiClient] Response code=" + response.code() + " body=" + (respBody.length() > 200 ? respBody.substring(0, 200) + "..." : respBody));
            if (!response.isSuccessful()) {
                throw new IOException("API Error: " + response.code() + " - " + respBody);
            }
        }
    }

    /**
     * Login to the API and store session.
     * @param email User email
     * @param password User password
     * @return JsonObject containing user data
     * @throws IOException if login fails
     */
    public JsonObject login(String email, String password) throws IOException {
        JsonObject loginData = new JsonObject();
        loginData.addProperty("email", email);
        loginData.addProperty("password", password);

        String response = post("/auth/login", gson.toJson(loginData));
        JsonObject result = gson.fromJson(response, JsonObject.class);

        // Store session data
        if (result.has("userId")) {
            currentUserId = result.get("userId").getAsString();
            currentUserRole = result.has("role") ? result.get("role").getAsString() : "EMPLOYEE";
            currentUser = result.has("user") ? result.getAsJsonObject("user") : null;
        }

        return result;
    }

    /**
     * Logout and clear session.
     */
    public void logout() {
        currentUserId = null;
        currentUserRole = null;
        currentUser = null;
    }

    // Session getters
    public static String getCurrentUserId() {
        return currentUserId;
    }

    public static String getCurrentUserRole() {
        return currentUserRole;
    }

    public static JsonObject getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(JsonObject user) {
        currentUser = user;
        if (user != null) {
            currentUserId = user.has("id") ? user.get("id").getAsString() : null;
            currentUserRole = user.has("role") ? user.get("role").getAsString() : null;
        }
    }

    public static boolean isLoggedIn() {
        return currentUserId != null;
    }

    public Gson getGson() {
        return gson;
    }
}

