package com.android.systemui.screenrecord;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;

public class ScreenRecordService extends Service {
    private static final String TAG = ScreenRecordService.class.getSimpleName();

    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_TOGGLE_POINTER = "toggle_pointer";

    private static ScreenRecord mScreenrecord;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    final Messenger callback = msg.replyTo;
                    toggleScreenrecord();

                    Message reply = Message.obtain(null, 1);
                    try {
                        callback.send(reply);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return new Messenger(mHandler).getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction().equals(ACTION_START)) {
                startScreenrecord();
            } else if (intent.getAction().equals(ACTION_STOP)) {
                stopScreenrecord();
            } else if (intent.getAction().equals(ACTION_TOGGLE_POINTER)) {
                int currentStatus = Settings.System.getIntForUser(getContentResolver(),
                            Settings.System.SHOW_TOUCHES, 0, UserHandle.USER_CURRENT);
                Settings.System.putIntForUser(getContentResolver(), Settings.System.SHOW_TOUCHES,
                            1 - currentStatus, UserHandle.USER_CURRENT);
                mScreenrecord.updateNotification();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void startScreenrecord() {
        if (mScreenrecord == null) {
            mScreenrecord = new ScreenRecord(ScreenRecordService.this);
        }
        mScreenrecord.recordScreen();
    }

    private void stopScreenrecord() {
        if (mScreenrecord == null) {
            return;
        }
        mScreenrecord.stopScreenrecord();

        Settings.System.putIntForUser(getContentResolver(), Settings.System.SHOW_TOUCHES,
                0, UserHandle.USER_CURRENT);
    }

    private void toggleScreenrecord() {
        if (mScreenrecord == null || !mScreenrecord.isRecording()) {
            startScreenrecord();
        } else {
            stopScreenrecord();
        }
    }
}