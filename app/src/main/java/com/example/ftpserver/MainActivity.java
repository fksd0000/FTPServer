package com.example.ftpserver;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button button =null;
    private final ActivityResultLauncher<Intent> allFilesAccessLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {

                if (Environment.isExternalStorageManager() == false) {
                    finish();
                }
            }
    );
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        if (Environment.isExternalStorageManager()) {
            // You have full storage access. Proceed with file operations.
        } else {
            // Request the permission from the user.
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.addCategory("android.intent.category.DEFAULT");
            String packagename=getPackageName();
            intent.setData(Uri.parse(String.format("package:%s", packagename)));
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getApplicationContext().getPackageName());

            startActivity(intent);

        }

        click();
    }
    public void click() {
        StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
        File[] storageVolumes_ = ContextCompat.getExternalFilesDirs(getApplicationContext(), null);

        for (File volume : storageVolumes_) {
            if (volume != null) {
                // This outputs the app-specific path on each physical volume
                Log.d("StorageLocation", "Available volume path: " + volume.getAbsolutePath());
            }
        }
        for (StorageVolume volume : storageVolumes) {
            String description = volume.getDescription(this); // e.g., "Internal Storage" or "SD Card"
            boolean isPrimary = volume.isPrimary();
            boolean isRemovable = volume.isRemovable();
            Log.d("Storage", volume.getDirectory().getAbsolutePath());
            Log.d("Storage", description + " (Primary: " + isPrimary + ", Removable: " + isRemovable + ")");
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                MyFtpServer ftpServer = new MyFtpServer();
                ftpServer.startServer();
            }
        });
        thread.start();

    }


    public class MyFtpServer {
        private FtpServer ftpServer;

        public void startServer() {
            try {
                FtpServerFactory serverFactory = new FtpServerFactory();
                ListenerFactory factory = new ListenerFactory();

                // Set network port to 2121 to avoid permission restrictions
                factory.setPort(8021);
                serverFactory.addListener("default", factory.createListener());

                // Set up a custom user programmatically
                PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
                UserManager userManager = userManagerFactory.createUserManager();

                BaseUser user = new BaseUser();
                user.setName("admin");
                user.setPassword("admin");

                // Map the home directory to the public external storage downloads folder
                File homeDir = new File("/storage/16BE-0AF2/Torrents");
                user.setHomeDirectory(homeDir.getAbsolutePath());

                // Grant write permissions so clients can upload files
                List<Authority> authorities = new ArrayList<>();
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

}