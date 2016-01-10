package com.android.systemui.morningstar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;

public class CPUTemperature extends TextView {
    private Context mContext;
    private File file = null;
    private boolean isCelcius = false;

    final private static String UPDATE_INTENT = "update_alarm";

    private PendingIntent pendingIntent = null;

    private static String[] files = {
            "/sys/device/platform/omap/omap_temp_sensor.0/temperature",
            "/sys/kernel/debug/tegra_thermal/temp_tj",
            "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
            "/sys/class/thermal/thermal_zone-/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/devices/platform/s5p-tmu/curr_temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp",
            "/sys/device/virtual/thermal/thermal_zone1/temp",
            "/sys/devides/system/cpu/cpufreq/cput_attributes/cur_temp",
            "/sys/devices/platform/s5p-tmu/temperature",
    };

    public CPUTemperature(Context context) {
        this(context, null);
    }

    public CPUTemperature(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CPUTemperature(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;

        this.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isCelcius) {
                    isCelcius = true;
                } else {
                    isCelcius = false;
                }
            }
        });
        getFile();
    }

    private void getFile() {
        file = getTempFile("default");
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
            } else if (intent.getAction().equals(UPDATE_INTENT) && screenOn) {
                updateTemperature();
            }
        }
    };

    private void setAlarm(int interval) {
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(UPDATE_INTENT);
        pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), interval, pendingIntent);
    }

    private void cancelAlarm() {
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private void updateTemperature() {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            StringBuilder stringBuilder = new StringBuilder(" ");
            String unit;

            byte[] buffer = new byte[1024];
            while (inputStream.read(buffer) != -1) {
                stringBuilder.append(new String(buffer));
            }
            inputStream.close();

            String mString = stringBuilder.toString().replaceAll("[^0-9.] + ", " ");
            float mFloat = Float.valueOf(mString);

            if (isCelcius) {
                unit = "C";
            } else {
                unit = "F";
                mFloat = (mFloat * 9 / 5) + 32;
            }

            setText((int)mFloat + "°" + unit);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File getTempFile(String fileName) {
        File mFile = null;

        if (fileName != null) {
            mFile = new File(fileName);
            if (!mFile.exists() || !mFile.canRead()) {
                mFile = null;
            }
        }
        if (mFile == null || fileName.equals("default")) {
            for (String tempFileName : files) {
                mFile = new File(tempFileName);
                if (!mFile.exists() || !mFile.canRead()) {
                    mFile = null;
                }
                else {
                    break;
                }
            }
        }
        return mFile;
    }
}