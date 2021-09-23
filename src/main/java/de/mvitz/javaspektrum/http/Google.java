package de.mvitz.javaspektrum.http;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.UriTemplate;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Google {

    private static final String TWITTER_API_KEY = "YOUR_TWITTER_API_KEY";
    private static final String TWITTER_API_KEY_SECRET = "YOUR_TWITTER_API_KEY_SECRET";

    public static void main(String[] args) throws IOException {
        enableLogging();

        final HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();

        final String accessToken = getAccessToken(requestFactory, TWITTER_API_KEY, TWITTER_API_KEY_SECRET);
        final JSONObject listMembers = getListMembers(requestFactory, accessToken, "171867803");
        System.out.println(listMembers.toString(2));
    }

    private static JSONObject getListMembers(HttpRequestFactory requestFactory, String accessToken, String listId) throws IOException {
        final HttpRequest request = requestFactory.buildGetRequest(
            new GenericUrl(UriTemplate.expand("https://api.twitter.com/1.1/lists/members.json{?list_id,count}",
                Map.of("list_id", listId, "count", 5000), false)))
            .setHeaders(new HttpHeaders()
                .setAccept("application/json; charset=utf-8")
                .setAuthorization("Bearer " + accessToken))
            .setLoggingEnabled(true);

        final HttpResponse response = request.execute();

        if (!HttpStatusCodes.isSuccess(response.getStatusCode())) {
            throw new IllegalStateException("Unable to retrieve list members: " + response.getStatusCode() + ": " + response.getStatusMessage());
        }

        try (InputStream in = response.getContent()) {
            return new JSONObject(new JSONTokener(in));
        }
    }

    private static String getAccessToken(HttpRequestFactory requestFactory, String apiKey, String apiKeySecret) throws IOException {
        final HttpRequest request = requestFactory.buildPostRequest(
                new GenericUrl("https://api.twitter.com/oauth2/token"),
                ByteArrayContent.fromString("application/x-www-form-urlencoded; charset=utf-8", "grant_type=client_credentials"))
            .setHeaders(new HttpHeaders()
                .setAccept("application/json; charset=utf-8")
                .setBasicAuthentication(apiKey, apiKeySecret));

        final HttpResponse response = request.execute();

        if (!HttpStatusCodes.isSuccess(response.getStatusCode())) {
            throw new IllegalStateException("Unable to retrieve access token: " + response.getStatusCode() + ": " + response.getStatusMessage());
        }

        try (InputStream in = response.getContent()) {
            final JSONObject responseJson = new JSONObject(new JSONTokener(in));
            return responseJson.getString("access_token");
        }
    }

    private static void enableLogging() {
        Logger logger = Logger.getLogger(HttpTransport.class.getName());
        logger.setLevel(Level.CONFIG);
        logger.addHandler(new Handler() {

            @Override
            public void close() throws SecurityException {
            }

            @Override
            public void flush() {
            }

            @Override
            public void publish(LogRecord record) {
                // Default ConsoleHandler will print >= INFO to System.err.
                if (record.getLevel().intValue() < Level.INFO.intValue()) {
                    System.out.println(record.getMessage());
                }
            }
        });
    }
}
