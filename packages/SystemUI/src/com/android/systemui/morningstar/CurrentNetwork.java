package com.android.systemui.morningstar;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class CurrentNetwork extends TextView {
    private Context mContext;
    final private static String UPDATE_INTENT = "network_update";

    public CurrentNetwork(Context context) {
        this(context, null);
    }

    public CurrentNetwork(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CurrentNetwork(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setText(currentNetwork());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_INTENT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        mContext.registerReceiver(mBroadcastReceiver, filter);

        IntentFilter wifiFilter = new IntentFilter();
        wifiFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        mContext.registerReceiver(wifiReceiver, wifiFilter);
    }

    @Override
    protected void onDetachedFromWindow() {
        mContext.unregisterReceiver(mBroadcastReceiver);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int vis) {
        super.onVisibilityChanged(changedView, vis);
        updateNetwork();
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        private boolean isScreenOn = true;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                isScreenOn = true;
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                isScreenOn = false;
            } else if (intent.getAction().equals(UPDATE_INTENT)
                    && isScreenOn) {
                updateNetwork();
            }
        }
    };

    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                setText(getCurrentSSID());
            } else {
                setText(getCarrier());
            }
        }
    };

    private void updateNetwork() {
        setText(currentNetwork());
    }

    private String getCurrentSSID() {
        String ssid = null;
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                ssid = connectionInfo.getSSID();
            }
        }
        if (ssid != null && ssid.contains("\"")) {
            ssid = ssid.replace("\"", "");
        }
        return ssid;
    }

    private String getCarrier() {
        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        String carrier = telephonyManager.getNetworkOperatorName();
        if (carrier.equals("")) {
            return "Network\nUnavailable";
        } else {
            return carrier;
        }
    }

    private String currentNetwork() {
        String wifiSSID = getCurrentSSID();
        if (wifiSSID != null) {
            return wifiSSID;
        } else {
            return getCarrier();
        }
    }
}
