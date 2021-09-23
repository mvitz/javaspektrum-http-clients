package de.mvitz.javaspektrum.http;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.nio.charset.StandardCharsets.UTF_8;

public class LegacyJava {

    private static final String TWITTER_API_KEY = "YOUR_TWITTER_API_KEY";
    private static final String TWITTER_API_KEY_SECRET = "YOUR_TWITTER_API_KEY_SECRET";

  public static void main(String[] args) throws IOException {
    var accessToken = getAccessToken(TWITTER_API_KEY, TWITTER_API_KEY_SECRET);
    var listMembers = getListMembers(accessToken, "171867803");
    System.out.println(listMembers.toString(2));
  }

  static JSONObject getListMembers(String accessToken, String listId)
      throws IOException {
    return performRequest(
        "GET",
        "https://api.twitter.com/1.1/lists/members.json?list_id=%s&count=5000".formatted(listId),
        Map.of("Accept", "application/json; charset=utf-8",
          "Authorization", "Bearer " + accessToken),
        null);
  }

  static String getAccessToken(String apiKey, String apiKeySecret)
      throws IOException {
    var encodedSecret = Base64.getEncoder()
      .encodeToString((apiKey + ":" + apiKeySecret).getBytes(UTF_8));
    return performRequest(
        "POST",
        "https://api.twitter.com/oauth2/token",
         Map.of("Accept", "application/json; charset=utf-8",
           "Authorization", "Basic " + encodedSecret,
                 "Content-Type", "application/x-www-form-urlencoded; charset=utf-8"),
        "grant_type=client_credentials")
      .getString("access_token");
    }

    static JSONObject performRequest(String method, String uri, Map<String, String> headers, String body)
        throws IOException {
      final URL url = new URL(uri);

      final var connection = (HttpURLConnection) url.openConnection();

      connection.setRequestMethod(method);
      headers.entrySet().forEach(header ->
          connection.setRequestProperty(header.getKey(), header.getValue()));

      if (body != null) {
        connection.setDoOutput(true);
      }

      System.err.println("Performing request: " +
          connection.getRequestMethod() + " " + connection.getURL() +
          " with headers: " + connection.getRequestProperties());

      connection.connect();

      if (body != null) {
        try (PrintWriter writer = new PrintWriter(connection.getOutputStream())) {
          writer.print(body);
        }
      }

      System.err.println("Got response: " +
          connection.getResponseCode() + " " + connection.getResponseMessage() +
          " with headers: " + connection.getHeaderFields());

      if (connection.getResponseCode() != HTTP_OK) {
        throw new IllegalStateException("Error: " +
            connection.getResponseCode() + " " + connection.getResponseMessage());
      }

      try (InputStream in = connection.getInputStream()) {
        return new JSONObject(new JSONTokener(in));
      }
    }
}
