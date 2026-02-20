package com.example.walkingmate.core.network;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.walkingmate.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

public class WeatherManager {
    // [보안 리팩토링] 날씨 API 키는 local.properties -> BuildConfig로 주입
    private static final String API_KEY = BuildConfig.OPEN_WEATHER_API_KEY;
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    public interface WeatherCallback {
        void onWeatherLoaded(String weatherIcon, String temperature);
    }

    public static void getWeather(Context context, double lat, double lon, final WeatherCallback callback) {
        if (API_KEY == null || API_KEY.isEmpty()) {
            Log.w("WeatherManager", "OPEN_WEATHER_API_KEY is missing. Check local.properties.");
            return;
        }
        String url = String.format("%s?lat=%.2f&lon=%.2f&units=metric&appid=%s", BASE_URL, lat, lon, API_KEY);

        // Ensure to use Application context for initializing RequestQueue
        RequestQueue queue = Volley.newRequestQueue(context.getApplicationContext());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject weatherObject = response.getJSONArray("weather").getJSONObject(0);
                        String weatherIcon = weatherObject.getString("icon");

                        JSONObject mainObject = response.getJSONObject("main");
                        String temperature = mainObject.getString("temp") + "°C";

                        callback.onWeatherLoaded(weatherIcon, temperature);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("WeatherManager", "Error fetching weather data", error)
        );

        queue.add(jsonObjectRequest);
    }
}
