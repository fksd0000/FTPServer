package com.example.ftpserver;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FTPServerService extends Service {
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    public static boolean IsRunning = false;
    public static String Path = "/";
    public static int Port = 8021;

    public static boolean StopRequested = false;
    public static int State = 0;

    MyFtpServer ftpServer;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        Notification notification = buildNotification();

        // 2. Promote to foreground (handles Android 14+ types safely)
        ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                        ? ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        : 0
        );

        IsRunning = true;
        // Do your background work here on a separate thread


        ftpServer = new MyFtpServer(Path);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                PowerManager.WakeLock wakeLock = null;

                try {


                    ftpServer.startServer();
                    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

                    wakeLock = powerManager.newWakeLock(
                            PowerManager.PARTIAL_WAKE_LOCK,
                            "MyApp::MyWakeLockTag"
                    );

                    // 3. Acquire the lock with a safe timeout (e.g., 10 minutes)
                    // Always set a timeout to prevent infinite battery drain if your app crashes
                    long timeoutMillis = 10 * 60 * 1000;
                    wakeLock.acquire();
                    State = 0;
                    startMonitoring();
                    try {
                        //Stopping service too early throws error.
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }

                    while (StopRequested == false) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    stopMonitoring();
                    stopService();
                    StopRequested = false;
                } catch (Exception e) {
                    try {
                        //Stopping service too early throws error.
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    stopMonitoring();
                    stopService();
                    e.printStackTrace();
                } finally {
                    IsRunning = false;
                    State = 0;
                    wakeLock.release();
                }
            }
        });
        thread.start();

        return START_STICKY;
    }

    private Notification buildNotification() {

        // Setup the Intent targeting a BroadcastReceiver or an Activity
        Intent actionIntentTap = new Intent(this, NotificationReceiver.class);
        actionIntentTap.setAction("ACTION_TAP");

        PendingIntent actionPendingIntentTap = PendingIntent.getBroadcast(this, 0, actionIntentTap,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent activityIntent = new Intent(this, MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
// Pass any server data or routing instructions as extras
        activityIntent.putExtra("notification_action", "open_server_panel");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                activityIntent,
                PendingIntent.FLAG_IMMUTABLE // Use FLAG_UPDATE_CURRENT if you modify extras dynamically
        );

        Intent intentStop = new Intent(this, NotificationReceiver.class);
        intentStop.setAction("ACTION_STOP");

        PendingIntent pendingIntentStop = PendingIntent.getBroadcast(this, 0, intentStop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_collapsed);
        String addrs = logAllNetworkInterfaces();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("FTP Server")
                .setContentText("Running at " + getLocalIpAddress(this) + ":" + String.valueOf(Port))
                .setStyle(
                        new NotificationCompat.BigTextStyle()
                                .bigText("AoA Sir, AoA Sir, AoA Sir.\nAddresses:\n" + addrs + "\nPort: " + String.valueOf(Port)) // Expanded view
                                .setBigContentTitle("Running at: " + getLocalIpAddress(this) + ":" + String.valueOf(Port)) // Changes title when expanded
                        //.setSummaryText("Running at: " + getLocalIpAddress(this) + ":" + String.valueOf(Port)) // Adds top sub-text
                )
                .setContentIntent(actionPendingIntentTap)
                //.setSmallIcon(android.R.drawable.ic_dialog_info)
                .addAction(R.drawable.ic_notification, "Open App", pendingIntent)
                .addAction(R.drawable.ic_notification, "Stop", pendingIntentStop)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(null).setVibrate(null).setSilent(true)
                //.setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
                .setSmallIcon(R.drawable.ic_notification) // Required fallback icon
                //.setStyle(new NotificationCompat.DecoratedCustomViewStyle()) // Keeps system decor if desired
                //.setCustomContentView(notificationLayout)                  // Sets collapsed layout
                //.setCustomBigContentView(notificationLayout) // Set the expanded custom layout
                .setOngoing(true) // Makes it sticky
                .build();

        return notification;
    }

    private void updateNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_ID, buildNotification());
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    //NotificationManager.IMPORTANCE_DEFAULT
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setSound(null, null);
            serviceChannel.enableVibration(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        IsRunning = false;
        // 1. Clean up resources (threads, listeners, database connections)
        // 2. Save any unsaved persistent data
        // 3. (Optional) Broadcast to the app that the service was killed
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void stopService() {
        // 1. Remove the service from the foreground state
        stopForeground(STOP_FOREGROUND_REMOVE);

// 2. Terminate the service completely
        stopSelf();

        IsRunning = false;
        ftpServer.stopServer();
    }


    public class MyFtpServer {
        private FtpServer ftpServer;
        String path;

        public MyFtpServer(String path) {
            this.path = path;
        }

        public void startServer() {
            try {
                FtpServerFactory serverFactory = new FtpServerFactory();

                ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();

                connectionConfigFactory.setAnonymousLoginEnabled(true);
                serverFactory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());

                ListenerFactory factory = new ListenerFactory();

                // Set network port to 2121 to avoid permission restrictions
                factory.setPort(8021);
                serverFactory.addListener("default", factory.createListener());

                // Set up a custom user programmatically
                PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
                UserManager userManager = userManagerFactory.createUserManager();

                BaseUser anonUser = new BaseUser();
                anonUser.setName("anonymous");
                File homeDir = new File(path);
                anonUser.setHomeDirectory(homeDir.getAbsolutePath());
                List<Authority> authorities = new ArrayList<>();
                authorities.add(new WritePermission());
                anonUser.setAuthorities(authorities);

// Save user to your UserManager instance
                userManager.save(anonUser);
                BaseUser user = new BaseUser();
                user.setName("admin");
                user.setPassword("admin");

                // Map the home directory to the public external storage downloads folder

                user.setHomeDirectory(homeDir.getAbsolutePath());

                // Grant write permissions so clients can upload files
                authorities = new ArrayList<>();
                authorities.add(new WritePermission());
                user.setAuthorities(authorities);

                // Save user configuration and bind to server instance
                userManager.save(user);
                serverFactory.setUserManager(userManager);

                // Initialize and spin up the server thread
                ftpServer = serverFactory.createServer();
                ftpServer.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void stopServer() {
            if (ftpServer != null && !ftpServer.isStopped()) {
                ftpServer.stop();
            }
        }
    }


    public static class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Retrieve data sent from the action button
            String action = intent.getAction();

            if ("ACTION_TAP".equals(action)) {
                Toast.makeText(context, "Tap", Toast.LENGTH_SHORT).show();
            } else if (action.equals("ACTION_OPEN")) {


            } else if (action.equals("ACTION_STOP")) {
                Toast.makeText(context, "Stop", Toast.LENGTH_SHORT).show();
                StopRequested = true;
            }

        }
    }


    public static String getLocalIpAddress(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            Network activeNetwork = cm.getActiveNetwork();
            LinkProperties lp = cm.getLinkProperties(activeNetwork);
            if (lp != null) {
                for (LinkAddress linkAddress : lp.getLinkAddresses()) {
                    // Filter out IPv6 to get IPv4 (optional)
                    if (linkAddress.getAddress() instanceof Inet4Address) {
                        return linkAddress.getAddress().getHostAddress();
                    }
                }
            }
        }
        return "No IP Found";
    }

    public static String logAllNetworkInterfaces() {
        String ipaddresses = "";
        String ipv6addrs = "";
        try {

            // Retrieve all network interfaces on the device
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface networkInterface : interfaces) {
                // Skip interfaces that are inactive or loopback (127.0.0.1)
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }

                String interfaceName = networkInterface.getName();
                String displayName = networkInterface.getDisplayName();

                Log.d("NetworkUtils", "Interface: " + interfaceName + " (" + displayName + ")");

                // Loop through all IP addresses assigned to this specific interface
                List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress address : addresses) {
                    // Check if it's an IPv4 address (skip IPv6 if you only want IPv4)
                    if (!address.isLoopbackAddress()) {
                        String ipAddress = address.getHostAddress();

                        if (ipAddress.startsWith("fe80")) continue;
                        // Filter out zone indices often appended to IPv6 addresses (e.g., %wlan0)
                        if (ipAddress.contains("%")) {
                            ipAddress = ipAddress.substring(0, ipAddress.indexOf("%"));
                        }

                        Log.d("NetworkUtils", "   -> IP Address: " + ipAddress);
                        if (address instanceof Inet4Address)
                            ipaddresses += ipAddress + "\n";
                        else ipv6addrs += ipAddress + "\n";
                    }
                }
            }
        } catch (Exception e) {
            Log.e("NetworkUtils", "Error retrieving network interfaces", e);
        }
        return ipaddresses + ipv6addrs;
    }


    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    public void startMonitoring() {
        // Build a request to target networks with active internet transport capabilities
        this.connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                updateNotification();
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                updateNotification();
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                // Optional: Check if connection is unmetered (WiFi) or metered (Cellular)
                boolean isWifi = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
                updateNotification();
            }
        };

        // Register the callback to listen to live events
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    public void stopMonitoring() {
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }
}
