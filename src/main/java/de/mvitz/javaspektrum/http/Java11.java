package de.mvitz.javaspektrum.http;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Java11 {

    private static final String TWITTER_API_KEY = "YOUR_TWITTER_API_KEY";
    private static final String TWITTER_API_KEY_SECRET = "YOUR_TWITTER_API_KEY_SECRET";

    public static void main(String[] args) throws IOException, InterruptedException {
        final String accessToken = getAccessToken(TWITTER_API_KEY, TWITTER_API_KEY_SECRET);
        final JSONObject listMembers = getListMembers(accessToken, "171867803");
        System.out.println(listMembers.toString(2));
    }

    private static JSONObject getListMembers(String accessToken, String listId) throws IOException, InterruptedException {
        final HttpClient client = HttpClient.newBuilder()
                .build();

        final HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://api.twitter.com/1.1/lists/members.json?list_id=%s&count=5000".formatted(listId)))
                .header("Accept", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + accessToken)
                .build();

        final HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("Unable to retrieve list members: " + response.statusCode() + ": " + new String(response.body().readAllBytes(), UTF_8));
        }

        return new JSONObject(new JSONTokener(response.body()));
    }

    private static String getAccessToken(String apiKey, String apiKeySecret) throws IOException, InterruptedException {
        final HttpClient client = HttpClient.newBuilder()
//                .authenticator(new Authenticator() {
//                    @Override
//                    protected PasswordAuthentication getPasswordAuthentication() {
//                        return new PasswordAuthentication(apiKey, apiKeySecret.toCharArray());
//                    }
//                })
                .build();

        final HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .uri(URI.create("https://api.twitter.com/oauth2/token"))
                .header("Accept", "application/json; charset=utf-8")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((apiKey + ":" + apiKeySecret).getBytes(UTF_8)))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                .build();

        final HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("Unable to retrieve access token: " + response.statusCode() + ": " + new String(response.body().readAllBytes(), UTF_8));
        }

        final JSONObject responseJson = new JSONObject(new JSONTokener(response.body()));
        return responseJson.getString("access_token");
    }
}
