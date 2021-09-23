package de.mvitz.javaspektrum.http;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import org.json.JSONTokener;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class Retro {

    private static final String TWITTER_API_KEY = "YOUR_TWITTER_API_KEY";
    private static final String TWITTER_API_KEY_SECRET = "YOUR_TWITTER_API_KEY_SECRET";

    public static void main(String[] args) throws IOException {
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.twitter.com")
                .addConverterFactory(new OrgJsonFactory())
                .client(new OkHttpClient.Builder()
                        .addNetworkInterceptor(chain -> {
                            System.out.println("Executing request: " + chain.request());
                            final Response response = chain.proceed(chain.request());
                            System.out.println("Got response: " + response);
                            return response;
                        })
                        .build())
                .build();
        final Twitter twitter = retrofit.create(Twitter.class);

        final String accessToken = getAccessToken(twitter, TWITTER_API_KEY, TWITTER_API_KEY_SECRET);
        final JSONObject listMembers = getListMembers(twitter, accessToken, "171867803");
        System.out.println(listMembers.toString(2));
    }

    private static JSONObject getListMembers(Twitter client, String accessToken, String listId) throws IOException {
        final var request = client.getListMembers("Bearer " + accessToken, listId);

        final var response = request.execute();

        if (!response.isSuccessful()) {
            throw new IllegalStateException("Unable to retrieve list members: " + response.code() + ": " + response.message());
        }

        return response.body();
    }

    private static String getAccessToken(Twitter client, String apiKey, String apiKeySecret) throws IOException {
        final var request = client.getAccessToken(Credentials.basic(apiKey, apiKeySecret), "client_credentials");

        final var response = request.execute();

        if (!response.isSuccessful()) {
            throw new IllegalStateException("Unable to retrieve access token: " + response.code() + ": " + response.message());
        }

        return response.body().getString("access_token");
    }

    interface Twitter {
        @POST("/oauth2/token")
        @FormUrlEncoded
        @Headers("Accept: application/json; charset=utf-8")
        Call<JSONObject> getAccessToken(@Header("Authorization") String authorization, @Field("grant_type") String grantType);

        @GET("1.1/lists/members.json?count=5000")
        @Headers("Accept: application/json; charset=utf-8")
        Call<JSONObject> getListMembers(@Header("Authorization") String authorization, @Query("list_id") String listId);
    }

    static final class OrgJsonFactory extends Converter.Factory {
        @Override
        public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
            return (Converter<ResponseBody, Object>) value -> {
                try (InputStream in = value.byteStream()) {
                    return new JSONObject(new JSONTokener(in));
                }
            };
        }
    }
}
