package com.simats.pcos;

import android.content.Context;
import android.content.SharedPreferences;

public class IpV4Connection {
    private static final String PREF_NAME = "MyPrefs";
    private static final String CONNECTION_ID_KEY = "connectionId";
    private static String baseUrl = "http://14.139.187.229:8081/pcos_db/";

    public static String getBaseUrl() {
        return baseUrl;
    }

    public static void setBaseUrl(String baseUrl) {
        IpV4Connection.baseUrl = baseUrl;
    }

    public static String getUrl(String path) {
        return getBaseUrl() + path;
    }

    public static String cleanJsonResponse(String response) {
        if (response == null) return "";
        int firstBrace = response.indexOf('{');
        int firstBracket = response.indexOf('[');
        int startIndex = -1;

        if (firstBrace != -1 && firstBracket != -1) {
            startIndex = Math.min(firstBrace, firstBracket);
        } else if (firstBrace != -1) {
            startIndex = firstBrace;
        } else if (firstBracket != -1) {
            startIndex = firstBracket;
        }

        if (startIndex != -1) {
            int lastBrace = response.lastIndexOf('}');
            int lastBracket = response.lastIndexOf(']');
            int endIndex = Math.max(lastBrace, lastBracket);
            if (endIndex != -1 && endIndex >= startIndex) {
                return response.substring(startIndex, endIndex + 1);
            }
        }
        return response.trim();
    }

    private static String connectionId;

    public static String getConnectionId(Context context) {
        if (connectionId == null) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            connectionId = prefs.getString(CONNECTION_ID_KEY, null);
        }
        return connectionId;
    }

    public static void setConnectionId(Context context, String connectionId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(CONNECTION_ID_KEY, connectionId);
        editor.apply();
        IpV4Connection.connectionId = connectionId;
    }
}
