package de.mvitz.javaspektrum.http;

import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class Apache {

    private static final String TWITTER_API_KEY = "YOUR_TWITTER_API_KEY";
    private static final String TWITTER_API_KEY_SECRET = "YOUR_TWITTER_API_KEY_SECRET";

    public static void main(String[] args) throws IOException {
        final String accessToken = getAccessToken(TWITTER_API_KEY, TWITTER_API_KEY_SECRET);
        final JSONObject listMembers = getListMembers(accessToken, "171867803");
        System.out.println(listMembers.toString(2));
    }

    private static JSONObject getListMembers(String accessToken, String listId) throws IOException {
        try (final var client = HttpClients.custom()
                .build()) {

            final var request = ClassicRequestBuilder.get()
                    .setUri("https://api.twitter.com/1.1/lists/members.json?list_id=%s&count=5000".formatted(listId))
                    .addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType())
                    .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .build();

            try(final var response = client.execute(request)) {
                if (response.getCode() != HttpStatus.SC_OK) {
                    throw new IllegalStateException("Unable to retrieve list members: " + response.getCode() + ": " + response.getReasonPhrase());
                }
                try (InputStream in = response.getEntity().getContent()) {
                    return new JSONObject(new JSONTokener(in));
                }
            }
        }
    }

    private static String getAccessToken(String apiKey, String apiKeySecret) throws IOException {
//        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//        credentialsProvider.setCredentials(
//                new AuthScope("api.twitter.com", 443),
//                new UsernamePasswordCredentials(apiKey, apiKeySecret.toCharArray()));

        try (final var client = HttpClients.custom()
//                .setDefaultCredentialsProvider(credentialsProvider)
                .addRequestInterceptorFirst((httpRequest, entityDetails, httpContext) -> {
                    System.out.print("Executing request: " + httpRequest);
                    System.out.println(" with headers: " + Arrays.asList(httpRequest.getHeaders()));
                })
                .build()) {

            final var basicAuth = new BasicScheme();
            basicAuth.initPreemptive(new UsernamePasswordCredentials(apiKey, apiKeySecret.toCharArray()));

            final var context = HttpClientContext.create();
            context.resetAuthExchange(new HttpHost("https", "api.twitter.com", 443), basicAuth);

            final ClassicHttpRequest request = ClassicRequestBuilder.post()
                    .setUri("https://api.twitter.com/oauth2/token")
                    .addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType())
                    .addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
                    .addParameter("grant_type", "client_credentials")
                    .build();

            try (final var response = client.execute(request, context)) {
                if (response.getCode() != HttpStatus.SC_OK) {
                    throw new IllegalStateException("Unable to retrieve access token: " + response.getCode() + ": " + response.getReasonPhrase());
                }
                try (var in = response.getEntity().getContent()) {
                    final JSONObject responseJson = new JSONObject(new JSONTokener(in));
                    return responseJson.getString("access_token");
                }
            }
        }
    }
}
