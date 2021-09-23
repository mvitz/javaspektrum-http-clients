package de.mvitz.javaspektrum.http;

import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;

public class Ok {

    private static final String TWITTER_API_KEY = "YOUR_TWITTER_API_KEY";
    private static final String TWITTER_API_KEY_SECRET = "YOUR_TWITTER_API_KEY_SECRET";

    public static void main(String[] args) throws IOException {
        final String accessToken = getAccessToken(TWITTER_API_KEY, TWITTER_API_KEY_SECRET);
        final JSONObject listMembers = getListMembers(accessToken, "171867803");
        System.out.println(listMembers.toString(2));
    }

    private static JSONObject getListMembers(String accessToken, String listId) throws IOException {
        final OkHttpClient client = new OkHttpClient.Builder()
                .build();

        final Request request = new Request.Builder()
                .get()
                .url(HttpUrl.parse("https://api.twitter.com/1.1/lists/members.json").newBuilder()
                        .addQueryParameter("list_id", listId)
                        .addQueryParameter("count", "5000")
                        .build())
                .header("Accept", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Unable to retrieve list members: " + response.code() + ": " + response.message());
            }
            try (InputStream in = response.body().byteStream()) {
                return new JSONObject(new JSONTokener(in));
            }
        }
    }

    private static String getAccessToken(String apiKey, String apiKeySecret) throws IOException {
        final OkHttpClient client = new OkHttpClient.Builder()
//                .authenticator((route, response) -> {
//                    if (response.request().header("Authorization") != null) {
//                        return null; // Give up, we've already attempted to authenticate.
//                    }
//
//                    System.out.println("Authenticating for response: " + response);
//                    System.out.println("Challenges: " + response.challenges());
//                    String credential = Credentials.basic(apiKey, apiKeySecret);
//                    return response.request().newBuilder()
//                            .header("Authorization", credential)
//                            .build();
//                })
                .addNetworkInterceptor(chain -> {
                    System.out.println("Executing request: " + chain.request());
                    final Response response = chain.proceed(chain.request());
                    System.out.println("Got response: " + response);
                    return response;
                })
                .build();

        final Request request = new Request.Builder()
                .post(new FormBody.Builder()
                        .add("grant_type", "client_credentials")
                        .build())
                .url("https://api.twitter.com/oauth2/token")
                .header("Accept", "application/json; charset=utf-8")
                .header("Authorization", Credentials.basic(apiKey, apiKeySecret))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Unable to retrieve access token: " + response.code() + ": " + response.message());
            }
            try (InputStream in = response.body().byteStream()) {
                final JSONObject responseJson = new JSONObject(new JSONTokener(in));
                return responseJson.getString("access_token");
            }
        }
    }
}
