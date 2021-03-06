/**
 * Copyright 2014-present Liquid Data Intelligence S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.lqd.sdk.model;

import android.Manifest.permission;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;

import io.lqd.sdk.LQLog;
import io.lqd.sdk.LiquidTools;

public class LQDevice {

    private String mVendor;
    private String mDeviceModel;
    private int mSystemVersion;
    private String mScreenSize;
    private String mCarrier;
    private String mInternetConnectivity;
    private String mUid;
    private String mAppBundle;
    private String mAppVersion;
    private String mAppName;
    private int mReleaseVersion;
    private String mLiquidVersion;
    private HashMap<String, Object> mAttributes;

    private Context mContext;
    private String mLocale;
    private String mSystemLanguage;

    // Initialization
    public LQDevice(Context context, String liquidVersion) {
        mAttributes = new HashMap<String,Object>();
        mContext = context;
        mVendor = LQDevice.getDeviceVendor();
        mDeviceModel = LQDevice.getDeviceModel();
        mSystemVersion = LQDevice.getSystemVersion();
        mScreenSize = LQDevice.getScreenSize(context);
        mCarrier = LQDevice.getCarrier(context);
        mInternetConnectivity = LQDevice.getInternetConnectivity(context);
        mUid = LQDevice.getDeviceID(context);
        mAppBundle = LQDevice.getAppBundle(context);
        mAppName = LQDevice.getAppName(context);
        mAppVersion = LQDevice.getAppVersion(context);
        mReleaseVersion = LQDevice.getReleaseVersion(context);
        mLiquidVersion = liquidVersion;
        mLocale = LQDevice.getLocale();
        mSystemLanguage = LQDevice.getSystemLanguage();
    }

    public LQDevice(Context context, String liquidVersion, Location location) {
        this(context, liquidVersion);
        setLocation(location);
    }

    // Attributes
    public String getUID() {
        return mUid;
    }

    public void setLocation(Location location) {
        if (location == null) {
            mAttributes.remove("latitude");
            mAttributes.remove("longitude");
        } else {
            mAttributes.put("latitude", Double.valueOf(location.getLatitude()));
            mAttributes.put("longitude",Double.valueOf(location.getLongitude()));
        }
    }

    public void setPushId(String id) {
        if(id == null || id.equals("")) {
            mAttributes.remove("push_token");
        } else {
            mAttributes.put("push_token", id);
        }
    }

    // JSON
    public JSONObject toJSON() {
        // Updating to avoid callbacks
        mInternetConnectivity = LQDevice.getInternetConnectivity(mContext);

        HashMap<String, Object> attrs = new HashMap<String, Object>();
        if(mAttributes != null) {
            attrs.putAll(mAttributes);
        }
        attrs.put("vendor", mVendor);
        attrs.put("platform", "Android");
        attrs.put("model", mDeviceModel);
        attrs.put("system_version", mSystemVersion);
        attrs.put("screen_size", mScreenSize);
        attrs.put("carrier", mCarrier);
        attrs.put("internet_connectivity", mInternetConnectivity);
        attrs.put("unique_id", mUid);
        attrs.put("app_bundle", mAppBundle);
        attrs.put("app_name", mAppName);
        attrs.put("app_version", mAppVersion);
        attrs.put("release_version", mReleaseVersion);
        attrs.put("liquid_version", mLiquidVersion);
        attrs.put("locale", mLocale);
        attrs.put("system_language", mSystemLanguage);

        JSONObject json = new JSONObject();
        try {
            if(attrs != null){
                for(String key : attrs.keySet()){
                    json.put(key, attrs.get(key));
                }
            }
            return json;
        } catch (JSONException e) {
            LQLog.error("LQDevice toJSON: " + e.getMessage());
        }
        return null;
    }

    // Device Info
    private static String getDeviceVendor() {
        return android.os.Build.MANUFACTURER;
    }

    private static String getDeviceModel() {
        return android.os.Build.MODEL;
    }

    private static int getSystemVersion() {
        return android.os.Build.VERSION.SDK_INT;
    }

    private static String getSystemLanguage() {
        return Locale.getDefault().getLanguage();
    }

    private static String getLocale() {
        return Locale.getDefault().toString();
    }

    @SuppressWarnings("deprecation")
    private static String getScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        int width = display.getWidth(); // deprecated
        int height = display.getHeight(); // deprecated
        return width + "x" + height;
    }

    private static String getCarrier(Context context) {
        TelephonyManager telephonyManager = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
        return "" + telephonyManager.getNetworkOperatorName();
    }

    private static String getInternetConnectivity(Context context) {
        if (LiquidTools.checkForPermission(permission.ACCESS_NETWORK_STATE, context)) {
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mNetwork = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (mWifi != null && mWifi.isConnected()) {
                return "WiFi";
            } else if (mNetwork != null && mNetwork.isConnected()) {
                return "Cellular";
            } else {
                return "No Connectivity";
            }
        }else {
            return "No ACCESS_NETWORK_STATE permission";
        }
    }

    public synchronized static String getDeviceID(Context context) {
        String uid;
        SharedPreferences sharedPrefs = context.getSharedPreferences("io.lqd.UUID", Context.MODE_PRIVATE);
        uid = sharedPrefs.getString("io.lqd.UUID", null);
        if (uid == null) {
            uid = LQModel.newIdentifier();
            Editor editor = sharedPrefs.edit();
            editor.putString("io.lqd.UUID", uid);
            editor.commit();
        }
        return uid;
    }

    private static String getAppBundle(Context context) {
        return context.getPackageName();
    }

    private static String getAppName(Context context) {
        int stringId = context.getApplicationInfo().labelRes;
        return stringId == 0 ? "(unknown)" : context.getString(stringId);

    }

    private static String getAppVersion(Context context) {
        try {
            PackageInfo pInfo;
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";

    }

    private static int getReleaseVersion(Context context) {
        try {
            PackageInfo pInfo;
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;

    }
}
