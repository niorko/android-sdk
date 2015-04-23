package com.infinario.android.infinariosdk;

import android.app.AlarmManager;
import android.os.Build;

/**
 * This file has been created by igi on 1/14/15.
 */
public class Contract {
    /**
     * Logging
     */
    public static final String TAG = "Infinario";

    /**
     * SDK
     */
    public static final String SDK = "AndroidSDK";
    public static final String VERSION = "1.0.5";
    public static final String OS = "Android";

    /**
     * Preferences details
     */
    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String COOKIE = "cookie";
    public static final String CAMPAIGN_COOKIE = "campaign_cookie";
    public static final String REGISTERED = "registered";
    public static final String PROPERTY_APP_VERSION = "app_version";
    public static final String PROPERTY_SENDER_ID = "sender_id";
    public static final String PROPERTY_TARGET = "target";
    public static final String PROPERTY_AUTO_FLUSH = "auto_flush";
    public static final String PROPERTY_PUSH_NOTIFICATIONS = "push_notifications";
    public static final String PROPERTY_ICON = "icon";
    public static final String PROPERTY_REFERRER = "referrer";
    public static final String PROPERTY_TOKEN = "token";
    public static final String PROPERTY = "infinario";
    public static final String PROPERTY_SESSION_START = "session_start";
    public static final String PROPERTY_SESSION_END = "session_end";
    public static final String EXTRA_REQUEST_CODE = "request_code";

    /**
     * Cookie ID negotiation
     */
    public static final String NEGOTIATION_ENDPOINT = "/crm/customers/track";

    /**
     * Infinario admin details
     */
    public static final String DB_REGISTRATION_ID = "__android_registration_id";

    /**
     * Command details
     */
    public static final String CUSTOMER_ENDPOINT = "crm/customers";
    public static final String EVENT_ENDPOINT = "crm/events";
    public static final String DEFAULT_TARGET = "https://api.infinario.com";
    public static final String PING_TARGET = "/system/time";
    public static final String BULK_URL = "/bulk";

    /**
     * GCM details
     */
    public static final int NOTIFICATION_ID = 444;
    public static final boolean DEFAULT_PUSH_NOTIFICATIONS = false;

    /**
     * Automatic flushing
     */
    public static final long UPDATE_INTERVAL = 6 * AlarmManager.INTERVAL_HOUR;
    public static final long FLUSH_DELAY = 10 * 1000;
    public static final int FLUSH_COUNT = 50;
    public static final boolean DEFAULT_AUTO_FLUSH = true;
    public static final int PERIODIC_ALARM = 1;
    public static final int DELAYED_ALARM = 2;
    public static final int IMMEDIATE_ALARM = 3;

    /**
     * Session
     */
    public static final int SESSION_PING_INTERVAL = 1; // in seconds
    public static final int SESSION_TIMEOUT = 20 * 1000;

    /**
     * DbQueue controls
     */
    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_RETRIES = 20;

    /**
     * Database
     */
    public static final String TABLE_COMMANDS = "commands";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_COMMAND = "command";
    public static final String COLUMN_RETRIES = "retries";

    public static final String DATABASE_NAME = "commands.db";
    public static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    public static final String DATABASE_CREATE = "create table "
            + TABLE_COMMANDS + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_COMMAND
            + " text not null, " + COLUMN_RETRIES
            + " integer not null default 0);";
}
