package com.example.kkostov.chat.sharedpreferences;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by kkostov on 29-May-17.
 */

public class SessionManager {
    private static final String SESSION_NAME = "session";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_EMAIL = "client_email";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String IS_ADMIN_MODE_ENABLED = "is_admin_enabled";
    private static SharedPreferences sp;

    private static SessionManager instance;

    private SessionManager() {
    }

    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager();
            sp = context
                    .getSharedPreferences(SESSION_NAME, Context.MODE_PRIVATE);
        }

        return instance;
    }

    public String getAccessToken() {
        return sp.getString(ACCESS_TOKEN, null);
    }

    public void setAccessToken(String authToken) {
        sp.edit().putString(ACCESS_TOKEN, authToken).apply();
    }

    public void removeAccessToken() {
        sp.edit().remove(ACCESS_TOKEN).apply();
    }

    public String getClientEmail() {
        return sp.getString(CLIENT_EMAIL, null);
    }

    public void setClientEmail(String email) {
        sp.edit().putString(CLIENT_EMAIL, email).apply();
    }

    public void removeClientEmail() {
        sp.edit().remove(CLIENT_EMAIL).apply();
    }

    ;

    public long getClientId() {
        return sp.getLong(CLIENT_ID, 0);
    }

    public void setClientId(long clientId) {
        sp.edit().putLong(CLIENT_ID, clientId).apply();
    }

    public void removeClientId() {
        sp.edit().remove(CLIENT_ID).apply();
    }

    public void setAdminMode(boolean isEnabled) {
        sp.edit().putBoolean(IS_ADMIN_MODE_ENABLED, isEnabled).apply();
    }

    public boolean isAdminModeEnabled() {
        return sp.getBoolean(IS_ADMIN_MODE_ENABLED, false);
    }

    public void removeAdminMode() {
        sp.edit().remove(IS_ADMIN_MODE_ENABLED).apply();
    }

    public void removeAllData() {
        removeAccessToken();
        removeAdminMode();
        removeClientEmail();
        removeClientId();
    }

}
