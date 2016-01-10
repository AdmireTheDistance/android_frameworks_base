package com.android.systemui.morningstar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.AttributeSet;
import android.widget.TextView;

public class BatteryVoltage extends TextView {
    private Context mContext;
    final private static String UPDATE_INTENT = "update_alarm";
    private PendingIntent pendingIntent = null;

    public BatteryVoltage(Context context) {
        this(context, null);
    }

    public BatteryVoltage(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryVoltage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setText(voltage());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_INTENT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        mContext.registerReceiver(mReceiver, filter);
        setAlarm(1000);
    }

    @Override
    protected void onDetachedFromWindow() {
        mContext.unregisterReceiver(mReceiver);
        cancelAlarm();
        super.onDetachedFromWindow();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        private boolean screenOn = true;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                screenOn = true;
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                screenOn = false;
            } else if (intent.getAction().equals(UPDATE_INTENT)
                    && screenOn) {
                updateVoltage();
            }
        }
    };

    private void updateVoltage() {
        setText(voltage());
    }

    private void setAlarm(int interval) {
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(UPDATE_INTENT);
        pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), interval, pendingIntent);
    }

    private void cancelAlarm() {
        AlarmManager alarmManager = (AlarmManager) mContext
                .getSystemService(Context.ALARM_SERVICE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private String voltage() {
        Intent intent = mContext.registerReceiver(null, new IntentFilter(
                Intent.ACTION_BATTERY_CHANGED));
        float mFloat = ((float) intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0));
        int rounded = Math.round(mFloat);
        return String.valueOf(rounded) + " mV";
    }
}