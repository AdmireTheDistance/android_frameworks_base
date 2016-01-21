package com.android.systemui.screenrecord;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.content.res.Resources;
import android.media.MediaActionSound;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.util.Log;

import com.android.systemui.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

class ScreenRecord {
    private static final String TAG = ScreenRecord.class.getSimpleName();

    private static final int SCREENRECORD_NOTIFICATION_ID = 42;
    private static final int MSG_TASK_ENDED = 1;
    private static final int MSG_TASK_ERROR = 2;

    private static final String TMP_PATH = "/sdcard/__tmp_screenrecord.mp4";

    private Context mContext;
    private Handler mHandler;
    private NotificationManager mNotificationManager;

    private MediaActionSound mCameraSound;

    private CaptureThread mCaptureThread;

    private class CaptureThread extends Thread {
        public void run() {
            Runtime rt = Runtime.getRuntime();
            String[] cmds = new String[] {"/system/bin/screenrecord", TMP_PATH};
            try {
                Process proc = rt.exec(cmds);
                BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));

                while (!isInterrupted()) {
                    if (br.ready()) {
                        String log = br.readLine();
                        Log.d(TAG, log);
                    }

                    try {
                        int code = proc.exitValue();
                        Message msg = Message.obtain(mHandler, MSG_TASK_ENDED, code, 0, null);
                        mHandler.sendMessage(msg);
                        return;
                    } catch (IllegalThreadStateException e) {
                        e.printStackTrace();
                    }
                }

                rt.exec(new String[]{"killall", "-2", "screenrecord"});
            } catch (IOException e) {
                Message msg = Message.obtain(mHandler, MSG_TASK_ERROR);
                mHandler.sendMessage(msg);
            }
        }
    };

    /**
     * @param context everything needs a context :(
     */
    public ScreenRecord(Context context) {
        mContext = context;
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == MSG_TASK_ENDED) {
                    stopScreenrecord();
                } else if (msg.what == MSG_TASK_ERROR) {
                    mCaptureThread = null;
                }
            }
        };

        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public boolean isRecording() {
        return (mCaptureThread != null);
    }

    void recordScreen() {
        if (mCaptureThread != null) {
            return;
        }

        mCaptureThread = new CaptureThread();
        mCaptureThread.start();
        updateNotification();
    }

    public void updateNotification(){
        final Resources r = mContext.getResources();
        Notification.Builder builder = new Notification.Builder(mContext)
            .setTicker(r.getString(R.string.screen_record_ticker))
            .setContentTitle(r.getString(R.string.screen_record_noti_title))
            .setSmallIcon(R.drawable.record_screen)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true);

        Intent stopIntent = new Intent(mContext, ScreenRecordService.class)
            .setAction(ScreenRecordService.ACTION_STOP);
        PendingIntent stopPendIntent = PendingIntent.getService(mContext, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT);

        Intent pointerIntent = new Intent(mContext, ScreenRecordService.class)
            .setAction(ScreenRecordService.ACTION_TOGGLE_POINTER);
        PendingIntent pointerPendIntent = PendingIntent.getService(mContext, 0, pointerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT);

        boolean showTouches = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.SHOW_TOUCHES, 0, UserHandle.USER_CURRENT) != 0;
        int togglePointerIconId = showTouches ?
                R.drawable.ic_pointer_off :
                R.drawable.ic_pointer_on;
        int togglePointerStringId = showTouches ?
                R.string.screen_record_hide_pointer :
                R.string.screen_record_show_pointer;
        builder
            .addAction(com.android.internal.R.drawable.ic_media_stop,
                r.getString(R.string.screen_record_stop), stopPendIntent)
            .addAction(togglePointerIconId,
                r.getString(togglePointerStringId), pointerPendIntent);

        Notification notif = builder.build();
        mNotificationManager.notify(SCREENRECORD_NOTIFICATION_ID, notif);
    }

    void stopScreenrecord() {
        if (mCaptureThread == null) {
            return;
        }

        mNotificationManager.cancel(SCREENRECORD_NOTIFICATION_ID);

        try {
            mCaptureThread.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }

        while (mCaptureThread.isAlive()) {
            // wait...
        }

        mHandler.postDelayed(new Runnable() { public void run() {
            mCaptureThread = null;

            String fileName = "SCR_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".mp4";
            File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File screenshots = new File(pictures, "Screenshots");

            if (!screenshots.exists()) {
                if (!screenshots.mkdir()) {
                    return;
                }
            }

            File input = new File(TMP_PATH);
            final File output = new File(screenshots, fileName);

            try {
                copyFileUsingStream(input, output);
            } catch (IOException e) {
                Log.e(TAG, "Unable to copy output file", e);
                e.printStackTrace();
                Message msg = Message.obtain(mHandler, MSG_TASK_ERROR);
                mHandler.sendMessage(msg);
                return;
            } finally {
                input.delete();
            }

            MediaScannerConnection.scanFile(mContext,
                new String[] { output.getAbsolutePath(), input.getAbsolutePath() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                    Log.i(TAG, "MediaScanner finished scanning " + path);
                }
            });
        } }, 2000);
    }

    private static void copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }
}