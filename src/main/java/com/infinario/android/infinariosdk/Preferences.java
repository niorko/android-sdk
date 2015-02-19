package com.infinario.android.infinariosdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This file has been created by igi on 2/18/15.
 */
public class Preferences {

    private Context context;
    private static Preferences instance = null;

    private Preferences(Context context) {
        this.context = context;
    }

    public static Preferences get(Context context) {
        if (instance == null) {
            instance = new Preferences(context);
        }

        return instance;
    }

    /**
     * @param context application's context.
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return context.getSharedPreferences(Contract.PROPERTY, Context.MODE_PRIVATE);
    }

    /**
     * Gets sender ID from preferences.
     *
     * @return sender ID or project number obtained from Google Developers Console
     */
    public String getSenderId() {
        return getPreferences(context).getString(Contract.PROPERTY_SENDER_ID, null);
    }

    /**
     * Stores sender ID in preferences.
     *
     * @param senderId sender ID or project number obtained from Google Developers Console
     */
    public void setSenderId(String senderId) {
        getPreferences(context).edit().putString(Contract.PROPERTY_SENDER_ID, senderId).commit();
    }

    /**
     * Gets referrer from preferences.
     *
     * @return referrer
     */
    public String getReferrer() {
        return getPreferences(context).getString(Contract.PROPERTY_REFERRER, null);
    }

    /**
     * Stores referrer in preferences.
     *
     * @param referrer referrer from INSTALL_REFERRER intent
     */
    public void setReferrer(String referrer) {
        getPreferences(context).edit().putString(Contract.PROPERTY_REFERRER, referrer).commit();
    }

    /**
     * Gets icon from preferences.
     *
     * @return icon resource
     */
    public int getIcon() {
        return getPreferences(context).getInt(Contract.PROPERTY_ICON, R.drawable.infinario_notification_icon);
    }

    /**
     * Stores notification icon preferences.
     *
     * @param iconDrawable icon for the notifications, e.g. R.drawable.icon
     */
    public void setIcon(int iconDrawable) {
        getPreferences(context).edit().putInt(Contract.PROPERTY_ICON, iconDrawable).commit();
    }

    /**
     * Gets token from preferences.
     *
     * @return token
     */
    public String getToken() {
        return getPreferences(context).getString(Contract.PROPERTY_TOKEN, null);
    }

    /**
     * Stores token in preferences.
     *
     * @param token token
     */
    public void setToken(String token) {
        getPreferences(context).edit().putString(Contract.PROPERTY_TOKEN, token).commit();
    }

    /**
     * Gets target (Infinario API location) from preferences.
     *
     * @return Infinario API location
     */
    public String getTarget() {
        return getPreferences(context).getString(Contract.PROPERTY_TARGET, null);
    }

    /**
     * Stores target (Infinario API location) in preferences.
     *
     * @param target Infinario API location
     */
    public void setTarget(String target) {
        getPreferences(context).edit().putString(Contract.PROPERTY_TARGET, target).commit();
    }

    /**
     * Checks the state of automatic flushing.
     *
     * @return true if flushing is enabled, false otherwise
     */
    public boolean getAutomaticFlushing() {
        return getPreferences(context).getBoolean(Contract.PROPERTY_AUTO_FLUSH, Contract.DEFAULT_AUTO_FLUSH);
    }

    /**
     * Stores status of automatic flushing in preferences.
     *
     * @param value enabled / disabled
     */
    public void setAutomaticFlushing(boolean value) {
        getPreferences(context).edit().putBoolean(Contract.PROPERTY_AUTO_FLUSH, value).commit();
    }

    /**
     * Checks the state of push notifications.
     *
     * @return true if push notifications are enabled, false otherwise
     */
    public boolean getPushNotifications() {
        return getPreferences(context).getBoolean(Contract.PROPERTY_PUSH_NOTIFICATIONS, Contract.DEFAULT_PUSH_NOTIFICATIONS);
    }

    /**
     * Stores status of push notifications in preferences.
     *
     * @param value enabled / disabled
     */
    public void setPushNotifications(boolean value) {
        getPreferences(context).edit().putBoolean(Contract.PROPERTY_PUSH_NOTIFICATIONS, value).commit();
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     * registration ID.
     */
    public String getRegistrationId() {
        final SharedPreferences prefs = getPreferences(context);
        String registrationId = prefs.getString(Contract.PROPERTY_REG_ID, "");

        if (registrationId.isEmpty()) {
            Log.i(Contract.TAG, "Registration not found.");
            return "";
        }

        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(Contract.PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion();

        if (registeredVersion != currentVersion) {
            Log.i(Contract.TAG, "App version changed.");
            return "";
        }

        return registrationId;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param registrationId registration ID
     */
    public void setRegistrationId(String registrationId) {
        int appVersion = getAppVersion();
        Log.i(Contract.TAG, "Saving regId on app version " + appVersion);

        getPreferences(context)
                .edit()
                .putString(Contract.PROPERTY_REG_ID, registrationId)
                .putInt(Contract.PROPERTY_APP_VERSION, appVersion)
                .commit();
    }

    /**
     * Gets cookie ID from preferences.
     *
     * @return cookie ID
     */
    public String getCookieId() {
        SharedPreferences prefs = getPreferences(context);
        String cookieId = prefs.getString(Contract.COOKIE, "");

        if (cookieId.equals(Contract.NEGOTIATING_STATUS)) {
            cookieId = "";
        }

        return cookieId;
    }

    /**
     * Ensures cookie ID is available. Negotiates one if necessary.
     *
     * @return availability of cookie ID
     */
    @SuppressLint("CommitPrefEdits")
    public boolean ensureCookieId() {
        SharedPreferences prefs = getPreferences(context);
        String cookieId = prefs.getString(Contract.COOKIE, "");

        if (cookieId.isEmpty()) {
            String token = getToken();

            if (token == null) return false;

            prefs.edit().putString(Contract.COOKIE, Contract.NEGOTIATING_STATUS).commit();

            Map<String, String> ids = new HashMap<>();
            Map<String, String> device = new HashMap<>();
            Map<String, Object> data;

            cookieId = UUID.randomUUID().toString();
            ids.put(Contract.COOKIE, cookieId);

            device.put("device_model", Build.MODEL);
            device.put("os_version", Build.VERSION.RELEASE);
            device.put("os_name", "Android");

            Customer customer = new Customer(ids, token, null);

            data = customer.getData();
            data.put("device", device);
            data.put("campaign_id", getReferrer());

            HttpHelper http = new HttpHelper(getTarget());

            JSONObject response = http.post(Contract.NEGOTIATION_ENDPOINT, new JSONObject(data));

            if (response != null) {
                try {
                    cookieId = response.getJSONObject("data").getJSONObject("ids").getString("cookie");
                    Log.d(Contract.TAG, "Negotiated cookie id");
                    prefs.edit().putString(Contract.COOKIE, cookieId).commit();
                    return true;
                }
                catch (JSONException ignored) {
                }
            }

            prefs.edit().putString(Contract.COOKIE, "").commit();

            return false;
        }
        else if (cookieId.equals(Contract.NEGOTIATING_STATUS)) {
            return false;
        }

        return true;
    }

    /**
     * Gets app's version.
     *
     * @return Application's version code from the {@code PackageManager}.
     */
    private int getAppVersion() {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Clears cached information from device's memory (registration id, app version,
     * cookie ID)
     */
    @SuppressWarnings("unused")
    public void clearStoredData() {
        getPreferences(context).edit()
                .remove(Contract.PROPERTY_APP_VERSION)
                .remove(Contract.PROPERTY_REG_ID)
                .remove(Contract.PROPERTY_ICON)
                .remove(Contract.PROPERTY_PUSH_NOTIFICATIONS)
                .remove(Contract.PROPERTY_SENDER_ID)
                .remove(Contract.PROPERTY_AUTO_FLUSH)
                .remove(Contract.COOKIE)
                .commit();
    }
}
