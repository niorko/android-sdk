package com.infinario.android.infinariosdk;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * This file has been created by igi on 1/13/15.
 */
public class HttpHelper {

    private String target;
    private String userAgent;

    public HttpHelper(String target, String userAgent) {
        if (target == null) {
            this.target = Contract.DEFAULT_TARGET;
        }
        else {
            this.target = target;
        }

        this.userAgent = userAgent;
    }

    @SuppressWarnings("unused")
    public HttpHelper() {
        this(null, null);
    }

    /**
     * Sends HTTP POST request to {@code url} with {@code data}
     * encoded as JSON body.
     *
     * @param url target URL
     * @param data nested key-value data encoded as JSON in the body
     * @return HTTP response as JSONObject
     */
    public JSONObject post(String url, JSONObject data) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(target + url);
        StringEntity entity = null;
        HttpResponse response;
        String stringResponse;

        try {
            entity = new StringEntity(data.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        httppost.setEntity(entity);
        httppost.setHeader("Accept", "application/json");
        httppost.setHeader("Content-type", "application/json");

        if (userAgent != null) {
            httppost.setHeader("User-Agent", userAgent);
        }

        try {
            response = httpclient.execute(httppost);
            stringResponse = EntityUtils.toString(response.getEntity());
            return new JSONObject(stringResponse);
        } catch (IOException e) {
            Log.e(Contract.TAG, "Request to Infinario API failed.");
            return null;
        } catch (JSONException e) {
            Log.e(Contract.TAG, "Request to Infinario API failed: cannot parse response as JSON.");
            return null;
        }
    }
}
