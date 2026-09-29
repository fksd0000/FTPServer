//package com.example.ftpserver;
//import android.app.Notification;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.ServiceInfo;
//import android.net.ConnectivityManager;
//import android.net.LinkAddress;
//import android.net.LinkProperties;
//import android.net.Network;
//import android.net.NetworkCapabilities;
//import android.net.NetworkRequest;
//import android.os.Build;
//import android.os.IBinder;
//import android.os.PowerManager;
//import android.util.Log;
//import android.widget.RemoteViews;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.core.app.NotificationCompat;
//import androidx.core.app.ServiceCompat;
//
//
//import org.apache.ftpserver.ConnectionConfigFactory;
//import org.apache.ftpserver.DataConnectionConfigurationFactory;
//import org.apache.ftpserver.FtpServer;
//import org.apache.ftpserver.FtpServerFactory;
//import org.apache.ftpserver.ftplet.Authority;
//import org.apache.ftpserver.ftplet.UserManager;
//import org.apache.ftpserver.listener.ListenerFactory;
//import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
//import org.apache.ftpserver.usermanager.impl.BaseUser;
//import org.apache.ftpserver.usermanager.impl.WritePermission;
//
//import java.io.File;
//import java.io.IOException;
//import java.net.Inet4Address;
//import java.net.InetAddress;
//import java.net.NetworkInterface;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//public class FTPServerService extends Service {
//    private static final String CHANNEL_ID = "ForegroundServiceChannel";
//    private static final int NOTIFICATION_ID = 1;
//    public static boolean IsRunning = false;
//    public static String Path = "/";
//    public static int Port = 8021;
//
//    public static boolean StopRequested = false;
//    public static int State = 0;
//    public static AndroidSftpServer sftpServer;
//    MyFtpServer ftpServer;
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        createNotificationChannel();
//
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//
//
//        Notification notification = buildNotification();
//
//        // 2. Promote to foreground (handles Android 14+ types safely)
//        ServiceCompat.startForeground(
//                this,
//                NOTIFICATION_ID,
//                notification,
//                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
//                        ? ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
//                        : 0
//        );
//
//        IsRunning = true;
//        // Do your background work here on a separate thread
//
//        sftpServer = new AndroidSftpServer(Path, 8022, "admin", "admin");
//        ftpServer = new MyFtpServer(Path);
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                PowerManager.WakeLock wakeLock = null;
//
//                try {
//
//
//                    ftpServer.startServer();
//                    sftpServer.start();
//                    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
//
//                    wakeLock = powerManager.newWakeLock(
//                            PowerManager.PARTIAL_WAKE_LOCK,
//                            "MyApp::MyWakeLockTag"
//                    );
//
//                    // 3. Acquire the lock with a safe timeout (e.g., 10 minutes)
//                    // Always set a timeout to prevent infinite battery drain if your app crashes
//                    long timeoutMillis = 10 * 60 * 1000;
//                    wakeLock.acquire();
//                    State = 0;
//                    startMonitoring();
//                    try {
//                        //Stopping service too early throws error.
//                        Thread.sleep(5000);
//                    } catch (InterruptedException ex) {
//                        throw new RuntimeException(ex);
//                    }
//
//                    while (StopRequested == false) {
//                        try {
//                            Thread.sleep(100);
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//                    stopMonitoring();
//                    stopService();
//                    StopRequested = false;
//                } catch (Exception e) {
//                    try {
//                        //Stopping service too early throws error.
//                        Thread.sleep(5000);
//                    } catch (InterruptedException ex) {
//                        throw new RuntimeException(ex);
//                    }
//                    try {
//                        stopMonitoring();
//                        stopService();
//                    } catch (Exception ee) {
//                        e.printStackTrace();
//                        throw new RuntimeException(ee);
//                    }
//
//                    e.printStackTrace();
//                } finally {
//                    IsRunning = false;
//                    State = 0;
//                    //wakeLock.release();
//                }
//            }
//        });
//        thread.start();
//
//        return START_STICKY;
//    }
//
//
//    private void createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel serviceChannel = new NotificationChannel(
//                    CHANNEL_ID,
//                    "Foreground Service Channel",
//                    //NotificationManager.IMPORTANCE_DEFAULT
//                    NotificationManager.IMPORTANCE_LOW
//            );
//            serviceChannel.setSound(null, null);
//            serviceChannel.enableVibration(false);
//            NotificationManager manager = getSystemService(NotificationManager.class);
//            if (manager != null) {
//                manager.createNotificationChannel(serviceChannel);
//            }
//        }
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        IsRunning = false;
//        // 1. Clean up resources (threads, listeners, database connections)
//        // 2. Save any unsaved persistent data
//        // 3. (Optional) Broadcast to the app that the service was killed
//    }
//
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//
//    public void stopService() throws IOException {
//        // 1. Remove the service from the foreground state
//        stopForeground(STOP_FOREGROUND_REMOVE);
//
//// 2. Terminate the service completely
//        stopSelf();
//        sftpServer.stop();
//        IsRunning = false;
//        ftpServer.stopServer();
//        NotificationManager notificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        if (notificationManager != null) {
//            notificationManager.cancel(NOTIFICATION_ID);
//        }
//    }
//
//
//
//
//
//
//
//
//
//
//
//}
