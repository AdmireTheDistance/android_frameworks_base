package com.android.systemui.morningstar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;

public class CPUFrequency extends TextView {
    private Context mContext;
    private File freqFile = null;


    final private static String UPDATE_INTENT = "update_alarm";

    private PendingIntent pendingIntent = null;

    private static String[] files = {
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq",
            "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq",};

    public CPUFrequency(Context context) {
        this(context, null);
    }

    public CPUFrequency(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CPUFrequency(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        getFile();
    }

    private void getFile() {
        freqFile = getFreqFile("default");
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
                updateFrequency();
            }
        }
    };

    private void setAlarm(int interval) {
        AlarmManager alarmManager = (AlarmManager) mContext
                .getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(UPDATE_INTENT);
        pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), interval,
                pendingIntent);
    }

    private void cancelAlarm() {
        AlarmManager alarmManager = (AlarmManager) mContext
                .getSystemService(Context.ALARM_SERVICE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private void updateFrequency() {
        try {
            FileInputStream inputStream = new FileInputStream(freqFile);
            StringBuilder stringBuilder = new StringBuilder("");

            byte[] buffer = new byte[1024];
            while (inputStream.read(buffer) != -1) {
                stringBuilder.append(new String(buffer));
            }
            inputStream.close();

            String mString = stringBuilder.toString().replaceAll("[^0-9]+", "");
            Long mLong = Long.valueOf(mString) / 1000;
            int mInt = mLong.intValue();

            if (mInt > 1000) {
                float fFreq = mInt / 1000F;
                mString = String.format("%.2f", fFreq) + " GHz";
            } else {
                mString = String.valueOf(mInt) + " MHz";
            }

            setText(mString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File getFreqFile(String fileName) {
        File mFile = new File(fileName);
        if (!mFile.exists() || !mFile.canRead()) {
            mFile = null;
        }
        for (String file : files) {
            mFile = new File(file);
            if (!mFile.exists() || !mFile.canRead()) {
                mFile = null;
            } else {
                break;
            }
        }
        return mFile;
    }
}