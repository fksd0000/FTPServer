package com.example.ftpserver;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mylibrary.ActionBar;
import com.example.mylibrary.AlertDialog;
import com.example.mylibrary.ForegroundService;
import com.example.mylibrary.Settings;
import com.example.mylibrary.SettingsFragment;
import com.google.android.material.materialswitch.MaterialSwitch;


import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.session.SessionContext;
import org.apache.sshd.common.util.security.SecurityProviderChoice;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    String TITLE = "FTP Server";
    String SettingsFileName = "Settings.xml";
    String SettingsFilePath = "";
    String SSH_DIR = "/";


    SwitchCompat serverEnable_switch;
    CheckBox sftpEnable_checkBox;
    CheckBox ftpEnable_checkBox;
    EditText sftpPort_editText;
    EditText ftpPort_editText;
    EditText username_editText;
    EditText password_editText;
    CheckBox anonymous_checkBox;
    EditText rootPath_editText;
    TextView availableLocations_textView;
    EditText symlinkLocation_editText;
    EditText symlinkTarget_editText;
    Button createSymlink_button;
    Button deleteSymlink_button;
    CheckBox symlinkPath_checkBox;


    int UPDATE_INTERVAL = 100;
    ActionBar actionBar = null;

    PreferencesFragment preferencesFragment;


    private final Handler handler = new Handler(Looper.getMainLooper());
    ForegroundService foregroundService;











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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // Already granted, no action needed
            } else {
                // Directly request the permission

                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (Environment.isExternalStorageManager()) {
            // You have full storage access. Proceed with file operations.
        } else {
            // Request the permission from the user.
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.addCategory("android.intent.category.DEFAULT");
            String packagename=getPackageName();
            intent.setData(Uri.parse(String.format("package:%s", packagename)));
            intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getApplicationContext().getPackageName());

            startActivity(intent);

        }

        InitActionBar();
        Init();
    }
    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(periodicUpdateRunnable, UPDATE_INTERVAL);
    }
    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(periodicUpdateRunnable);
    }



    private void InitActionBar() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        Window window = getWindow();
        var decorView = window.getDecorView();
        //        //getWindow().setBackgroundDrawable(new ColorDrawable(0xff0000));
        //        // clear FLAG_TRANSLUCENT_STATUS flag:
        //        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        //        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // finally change the color
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        window.setStatusBarColor(ContextCompat.getColor(this,R.color.my_statusbar_color_));
        window.setNavigationBarColor(ContextCompat.getColor(this,R.color.my_statusbar_color_));
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);


        actionBar = findViewById(R.id.actionbar);
        actionBar.setTitle(TITLE);

        actionBar.Callbacks = new ActionBar.OnCallbacks() {
            @Override
            public void onBack(View v) {
                getSupportFragmentManager().popBackStackImmediate();
            }

            @Override
            public void onOptions(View v) {

            }
        };
        actionBar.addOptionsItem("SETTINGS");
        Button button = (Button)actionBar.options.get(0);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowSettings();
            }
        });

        actionBar.addOptionsItem("SAVE");
        Button button_ = (Button)actionBar.options.get(1);
        button_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SaveSettings();
                LoadSettings();
            }
        });

    }
    private void Init() {

        serverEnable_switch = findViewById(R.id.serverEnable_switch);
        serverEnable_switch.setOnCheckedChangeListener(this::serverEnable_switch_onCheckedChange);

        sftpEnable_checkBox = findViewById(R.id.sftpServerEnable_checkBox);
        ftpEnable_checkBox = findViewById(R.id.ftpServerEnable_checkBox);
        sftpPort_editText = findViewById(R.id.sftpPort);
        ftpPort_editText = findViewById(R.id.ftpPort);
        username_editText = findViewById(R.id.serverUsername);
        password_editText = findViewById(R.id.serverPassword);
        anonymous_checkBox = findViewById(R.id.anonymous_checkBox);
        rootPath_editText = findViewById(R.id.serverPath_editText);
        availableLocations_textView = findViewById(R.id.availableLocations_textview);

        symlinkLocation_editText = findViewById(R.id.symlinkLocation_editText);
        symlinkTarget_editText = findViewById(R.id.symlinkTarget_editText);
        createSymlink_button = findViewById(R.id.createSymlink_button);
        deleteSymlink_button = findViewById(R.id.deleteSymlink_button);
        symlinkPath_checkBox = findViewById(R.id.symlinkPath_checkBox);

        createSymlink_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String name = symlinkLocation_editText.getText().toString();
                    String target = symlinkTarget_editText.getText().toString();

                    java.nio.file.Files.createSymbolicLink(Paths.get(getDataDir().getPath(), name),
                            Paths.get(target));
                    PopulateLocalLocations();
                    Toast.makeText(MainActivity.this, "Done", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    new AlertDialog().show(MainActivity.this, "Error", e.toString(), "OK", null, null);
                }
            }
        });
        deleteSymlink_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {    String name = symlinkLocation_editText.getText().toString();
                    String target = symlinkTarget_editText.getText().toString();

                    java.nio.file.Files.delete(Paths.get(getDataDir().getPath(), name));
                    Toast.makeText(MainActivity.this, "Done", Toast.LENGTH_SHORT).show();
                    PopulateLocalLocations();
                } catch (Exception e) {
                    e.printStackTrace();
                    new AlertDialog().show(MainActivity.this, "Error", e.toString(), "OK", null, null);
                }
            }
        });
        LoadSettings();

        configureServiceViews();


        SSH_DIR = getDataDir().toString();
    }






    public void ShowSettings() {


        SettingsFragment settingsFragment =new com.example.mylibrary.SettingsFragment();
        settingsFragment.filePath = new File(getDataDir(), SettingsFileName).getPath();
        settingsFragment.folderPath = getDataDir().getPath();
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container, settingsFragment , "settings_frag")
                .addToBackStack(null)
                .commit();

        actionBar.StartActionMode();

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                        super.onFragmentDestroyed(fm, f);

                        // Check if the exiting fragment is the target child
                        if (f instanceof SettingsFragment) {
                            // Execute your parent-side logic here
                            actionBar.StopActionMode();
                            LoadSettings();
                        }
                    }
                },
                false // Set to true to recursively monitor nested child fragments
        );
    }
    public void LoadSettings() {
        try {
            SettingsFilePath = new File(getDataDir(), SettingsFileName).getPath();
            File file = new File(SettingsFilePath);
            if (file.exists() == false) com.example.mylibrary.Settings.CopyFromAssets(this, SettingsFilePath, SettingsFileName);

            com.example.mylibrary.Settings.Load(SettingsFilePath);

            sftpEnable_checkBox.setChecked(Settings.PreferencesStore.getBoolean("sftpEnable", false));
            ftpEnable_checkBox.setChecked(Settings.PreferencesStore.getBoolean("ftpEnable", false));
            sftpPort_editText.setText(String.valueOf(Settings.PreferencesStore.getInt("SftpPort", 8022)));
            ftpPort_editText.setText(Settings.PreferencesStore.getString("FtpPort", "8021,8030-8039"));
            username_editText.setText(Settings.PreferencesStore.getString("Username", "admin"));
            password_editText.setText(Settings.PreferencesStore.getString("Password", "admin"));
            anonymous_checkBox.setChecked(Settings.PreferencesStore.getBoolean("FtpAnonymousEnabled", true));
            rootPath_editText.setText(Settings.PreferencesStore.getString("Path", "/"));
            symlinkPath_checkBox.setChecked(Settings.PreferencesStore.getBoolean("SymlinkRootEnabled", false));


            PopulateLocalLocations();
        } catch (Exception e) {
            e.printStackTrace();

            AlertDialog.show(this, "Error", "An error occurred while reading config\n" + "" +
                    "Fix config file.\n" +
                    "Details:\n" + e.getMessage(), "OK", null, null, new AlertDialog.Callback() {
                @Override
                public void onResult(int result) {
                    ShowSettings();
                }
            });

        }
    }
    public void SaveSettings() {
        try {
            Settings.PreferencesStore.putBoolean("sftpEnable", sftpEnable_checkBox.isChecked());
            Settings.PreferencesStore.putBoolean("ftpEnable", ftpEnable_checkBox.isChecked());
            Settings.PreferencesStore.putInt("SftpPort", Integer.parseInt(sftpPort_editText.getText().toString()));
            Settings.PreferencesStore.putString("FtpPort", ftpPort_editText.getText().toString());
            Settings.PreferencesStore.putString("Username", username_editText.getText().toString());
            Settings.PreferencesStore.putString("Password", password_editText.getText().toString());
            Settings.PreferencesStore.putBoolean("FtpAnonymousEnabled", anonymous_checkBox.isChecked());
            Settings.PreferencesStore.putString("Path", rootPath_editText.getText().toString());
            Settings.PreferencesStore.putBoolean("SymlinkRootEnabled", symlinkPath_checkBox.isChecked());

            com.example.mylibrary.Settings.Save();
            Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "An error occurred", Toast.LENGTH_SHORT).show();
        }
    }




    private void configureServiceViews() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                serverEnable_switch.setChecked(ForegroundService.IsRunning);
            }
        });

    }
    public void serverEnable_switch_onCheckedChange(CompoundButton buttonView, boolean isChecked) {

        buttonView.setEnabled(false);

        if (isChecked && ForegroundService.IsRunning == false) {

            ForegroundService.Callbacks = new ForegroundService.Callbacks() {
                @Override
                public void onStart() {
                    configureServiceViews();
                }

                @Override
                public void onStop() {
                    configureServiceViews();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            serverEnable_switch.setEnabled(true);
                        }
                    });

                }

                @Override
                public void onMainTask() {
                    MainTask();
                }

                @Override
                public void onMonitoringTask() {

                }

                @Override
                public Notification onBuildNotification(Context context, String s) {
                    return buildNotification(context, s);
                }
            };


            Intent serviceIntent = new Intent(this, com.example.mylibrary.ForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

        } else if (!isChecked) {
            synchronized (ForegroundService.WaitObject) {
                ForegroundService.WaitObject.notify();
            }
        }
    }
    private void MainTask() {

        try {

            StartSftpServer();
            StartFtpServer();
            startMonitoring();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Started", Toast.LENGTH_SHORT).show();
                    serverEnable_switch.setEnabled(true);
                }
            });


            synchronized (ForegroundService.WaitObject) {
                ForegroundService.WaitObject.wait();
            }

            stopMonitoring();
            StopSftpServer();
            StopFtpServer();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Stopped", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    serverEnable_switch.setEnabled(true);
                }
            });
        } finally {
        }

    }




    private void PopulateLocalLocations() {
        StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
        File[] storageVolumes_ = ContextCompat.getExternalFilesDirs(this, null);

        for (File volume : storageVolumes_) {
            if (volume != null) {
                // This outputs the app-specific path on each physical volume
                Log.d("StorageLocation", "Available volume path: " + volume.getAbsolutePath());
            }
        }
        String paths = "Roots:\n";
        List<String> locations = new ArrayList<>();
        for (StorageVolume volume : storageVolumes) {
            String description = volume.getDescription(this); // e.g., "Internal Storage" or "SD Card"
            boolean isPrimary = volume.isPrimary();
            boolean isRemovable = volume.isRemovable();
            Log.d("Storage", volume.getDirectory().getAbsolutePath());
            paths += volume.getDirectory().getAbsolutePath() + "\n";
            Log.d("Storage", description + " (Primary: " + isPrimary + ", Removable: " + isRemovable + ")");
            locations.add(volume.getDirectory().getAbsolutePath());
            //localLocations.add(description + " " + volume.getDirectory().getAbsolutePath());

        }
        paths += "App dir:   \n" + getDataDir().getPath() +"\n";
        File file = getDataDir().toPath().toFile();
        for (File f : file.listFiles()) {
            //if (f.isDirectory()) {
                paths += f.getName() + "\n";
            //}
        }

        try {
            availableLocations_textView.setText(paths);
            rootPath_editText.setText(locations.get(0));
            symlinkLocation_editText.setText("");
            symlinkTarget_editText.setText(locations.size() > 1 ? locations.get(1) : "/");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }



    private SshServer sshd;
    private FtpServer ftpd;
    Object wait_object = new Object();
    int sftpPort;
    int ftpPort;
    String ftpDataPorts;
    private void StartSftpServer() throws Exception {

        boolean symlink = Settings.PreferencesStore.getBoolean("SymlinkRootEnabled", false);
        String path = getDataDir().getPath();
        if (!symlink)
            path = Settings.PreferencesStore.getString("Path", "/");
        int port = Settings.PreferencesStore.getInt("SftpPort", 8022);
        String username = Settings.PreferencesStore.getString("Username", "admin");
        String password = Settings.PreferencesStore.getString("Password", "admin");

        sftpPort = port;
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.addProvider(new BouncyCastleProvider());

        // Add EdDSA provider explicitly for ED25519 signature handling
        //Security.addProvider(new EdDSASecurityProvider());

        //                if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
//                    Security.addProvider(new BouncyCastleProvider());
//                }
        Security.removeProvider("BC");

        //// 2. Insert the full, modern Bouncy Castle provider at position 1
        //                // It uses 1-based indexing.
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        System.setProperty("org.apache.sshd.security.provider.choice", "bc");
        System.setProperty("org.apache.sshd.security.providers", "bc");;
        SecurityUtils.setDefaultProviderChoice(SecurityProviderChoice.toSecurityProviderChoice("BC"));
        System.setProperty("org.bouncycastle.emulate.oracle", "true");
        System.setProperty("user.home", path);
        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);
        //sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get(path, "hostkey_unknown.ser")));
        File privateKeyFile = new File(SSH_DIR, "hostkey_rsa.pem");
        SimpleGeneratorHostKeyProvider rsaProvider = new SimpleGeneratorHostKeyProvider(privateKeyFile.toPath());
        // setting it to ssh_rsa causes it to fail
        //rsaProvider.setAlgorithm(KeyPairProvider.SSH_RSA); // Explicitly enforce RSA
        rsaProvider.setAlgorithm("RSA");
        rsaProvider.setKeySize(4096);
        // has native support for ecdsa
        privateKeyFile = new File(SSH_DIR, "hostkey_ecdsa.pem");
        SimpleGeneratorHostKeyProvider ecdsaProvider = new SimpleGeneratorHostKeyProvider(privateKeyFile.toPath());
        privateKeyFile = new File(SSH_DIR, "hostkey_ed25519.pem");
        // Fails doesn't have native support for ed25519
        //SimpleGeneratorHostKeyProvider ed25519Provider = new SimpleGeneratorHostKeyProvider(privateKeyFile.toPath());
        //ed25519Provider.setAlgorithm(KeyPairProvider.SSH_ED25519);
        //ed25519Provider.setAlgorithm("Ed25519");

        KeyPairProvider combinedProvider_ = new KeyPairProvider() {
            @Override
            public Iterable<KeyPair> loadKeys(SessionContext session) {
                List<KeyPair> keys = new ArrayList<>();

                // Extract keys from both providers safely
                for (KeyPair kp : rsaProvider.loadKeys(session)) { keys.add(kp); }
                for (KeyPair kp : ecdsaProvider.loadKeys(session)) { keys.add(kp); }
                //for (KeyPair kp : ed25519Provider.loadKeys(session)) { keys.add(kp); }
                return keys;
            }
        };
        sshd.setKeyPairProvider(combinedProvider_);


        sshd.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory.Builder().build()));
        sshd.setPasswordAuthenticator((user, pass, session) ->
                user.equals(username) && pass.equals(password));
        sshd.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get(path)));


        sshd.start();
        System.out.println("Loaded keys: " + sshd.getKeyPairProvider().loadKeys(null));

    }
    private void StopSftpServer() {
        try {
            if (sshd != null && sshd.isStarted()) {
                sshd.stop(true);
                sshd = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void StartFtpServer() throws Exception {
        boolean symlink = Settings.PreferencesStore.getBoolean("SymlinkRootEnabled", false);
        String path = getDataDir().getPath();
        if (!symlink)
            path = Settings.PreferencesStore.getString("Path", "/");
        String port_s = Settings.PreferencesStore.getString("FtpPort", "8021,8030-8039");
        String username = Settings.PreferencesStore.getString("Username", "admin");
        String password = Settings.PreferencesStore.getString("Password", "admin");
        boolean anonymous = Settings.PreferencesStore.getBoolean("FtpAnonymousEnabled", true);

        port_s = port_s.replace(" ", "");
        String[] port_s_f = port_s.split(",");
        String port_s_passive = Arrays.stream(port_s_f)
                .skip(1)
                .collect(Collectors.joining(","));
        port_s_passive = port_s.substring(port_s_f[0].length() + 1);
        int port = Integer.parseInt(port_s_f[0]);
        int port_passive_start = Integer.parseInt(port_s_f[1].split("-")[0]);
        int port_passive_end = Integer.parseInt(port_s_f[1].split("-")[1]);

        ftpPort = port;
        ftpDataPorts = port_s_passive;

        FtpServerFactory serverFactory = new FtpServerFactory();
        ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();

        serverFactory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());

        if (anonymous) {
            connectionConfigFactory.setAnonymousLoginEnabled(true);
            connectionConfigFactory.setMaxAnonymousLogins(10);
        }

        serverFactory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());

        ListenerFactory factory = new ListenerFactory();
        factory.setPort(port);
        DataConnectionConfigurationFactory dataConnectionFactory = new DataConnectionConfigurationFactory();
        // Set passive ports range, single port (e.g., "20020"), or list (e.g., "20020,20030,20040-20050")
        dataConnectionFactory.setPassivePorts(port_s_passive);
        // Disable active, only enable passive, to disable passive, set passive ports to empty string.
        dataConnectionFactory.setActiveEnabled(false);



        factory.setDataConnectionConfiguration(dataConnectionFactory.createDataConnectionConfiguration());
        serverFactory.addListener("default", factory.createListener());


        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        UserManager userManager = userManagerFactory.createUserManager();

        List<Authority>authorities = new ArrayList<>();
        authorities.add(new WritePermission());

        if (anonymous) {
            BaseUser anonUser = new BaseUser();
            anonUser.setName("anonymous");
            File homeDir = new File(path);
            anonUser.setHomeDirectory(homeDir.getAbsolutePath());
            anonUser.setAuthorities(authorities);
            userManager.save(anonUser);
        }


        BaseUser user = new BaseUser();
        user.setName(username);
        user.setPassword(password);
        user.setHomeDirectory(path);
        user.setAuthorities(authorities);
        userManager.save(user);

        serverFactory.setUserManager(userManager);

        // Initialize and spin up the server thread
        ftpd = serverFactory.createServer();
        ftpd.start();

    }
    private void StopFtpServer() {
        if (ftpd != null && !ftpd.isStopped()) {
            ftpd.stop();
        }
    }













    private final Runnable periodicUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                // Perform your UI update work here
                //UpdateUI();
            } finally {
                // Schedule the next execution at the specified interval
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        }
    };



    private final ActivityResultLauncher<Intent> allFilesAccessLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {

                if (Environment.isExternalStorageManager() == false) {
                    new AlertDialog().show(MainActivity.this, "Error", "Permission is " +
                            "required for this app to work.\nExitting.", "OK", null, null, null);
                    finish();
                }
            }
    );
    // Register the permissions callback to handle the response
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted! You can now show notifications.
                    //showToast("Notification permission granted!");
                } else {
                    new AlertDialog().show(MainActivity.this, "Error", "Permission is " +
                            "required for this app to work.\nExitting.", "OK", null, null, null);
                    finish();
                    // Permission denied. Inform the user they won't receive updates.
                    //showToast("Notification permission denied.");
                }
            });





    private Notification buildNotification(Context context, String CHANNEL_ID) {

        // Setup the Intent targeting a BroadcastReceiver or an Activity
        Intent actionIntentTap = new Intent(this, MainActivity.NotificationReceiver.class);
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

        Intent intentStop = new Intent(this, MainActivity.NotificationReceiver.class);
        intentStop.setAction("ACTION_STOP");

        PendingIntent pendingIntentStop = PendingIntent.getBroadcast(this, 0, intentStop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_collapsed);
        String addrs = logAllNetworkInterfaces();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("FTP Server")
                .setContentText("Running at " + getLocalIpAddress(this))
                .setStyle(
                        new NotificationCompat.BigTextStyle()
                                .bigText("Addresses:\n" + addrs +
                                        "\nSFTP Port: " + String.valueOf(sftpPort) +
                                        "\nFTP Port: " + String.valueOf(ftpPort) +
                                        "\nFTP Data Ports: " + ftpDataPorts) // Expanded view
                                .setBigContentTitle("Running at: " + getLocalIpAddress(this)) // Changes title when expanded
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
                synchronized (ForegroundService.WaitObject) {
                    ForegroundService.WaitObject.notify();
                }
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
                if (ForegroundService.RunningInstance != null)
                    ForegroundService.RunningInstance.updateNotification();
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                if (ForegroundService.RunningInstance != null)
                    ForegroundService.RunningInstance.updateNotification();
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                // Optional: Check if connection is unmetered (WiFi) or metered (Cellular)
                boolean isWifi = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
                if (ForegroundService.RunningInstance != null)
                    ForegroundService.RunningInstance.updateNotification();
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





//public class VirtualFtpFile implements FtpFile {
//    private final String virtualPath;
//    private final File physicalFile;
//    private final Map<String, File> virtualRoutes;
//    private final boolean isRoot;
//
//    // Constructor for the virtual root '/'
//    public VirtualFtpFile(Map<String, File> virtualRoutes) {
//        this.virtualPath = "/";
//        this.physicalFile = null;
//        this.virtualRoutes = virtualRoutes;
//        this.isRoot = true;
//    }
//
//    // Constructor for concrete folders/files inside the roots
//    public VirtualFtpFile(String virtualPath, File physicalFile, Map<String, File> virtualRoutes) {
//        this.virtualPath = virtualPath;
//        this.physicalFile = physicalFile;
//        this.virtualRoutes = virtualRoutes;
//        this.isRoot = false;
//    }
//
//    @Override
//    public String getAbsolutePath() { return virtualPath; }
//
//    @Override
//    public boolean isDirectory() { return isRoot || physicalFile.isDirectory(); }
//
//    @Override
//    public boolean isFile() { return !isRoot && physicalFile.isFile(); }
//
//    @Override
//    public boolean doesExist() { return isRoot || physicalFile.exists(); }
//
//    @Override
//    public List<FtpFile> listFiles() {
//        List<FtpFile> files = new ArrayList<>();
//        if (isRoot) {
//            // Display the virtual directories under '/'
//            for (Map.Entry<String, File> entry : virtualRoutes.entrySet()) {
//                files.add(new VirtualFtpFile("/" + entry.getKey(), entry.getValue(), virtualRoutes));
//            }
//        } else if (physicalFile.isDirectory()) {
//            // Standard directory listing inside a physical path
//            File[] children = physicalFile.listFiles();
//            if (children != null) {
//                for (File child : children) {
//                    String childVirtualPath = virtualPath + "/" + child.getName();
//                    files.add(new VirtualFtpFile(childVirtualPath, child, virtualRoutes));
//                }
//            }
//        }
//        return files;
//    }
//
//    // --- Delegate standard file actions to physical targets if not root ---
//    @Override public String getName() { return isRoot ? "/" : physicalFile.getName(); }
//    @Override public boolean isHidden() { return !isRoot && physicalFile.isHidden(); }
//    @Override public boolean isReadable() { return isRoot || physicalFile.canRead(); }
//    @Override public boolean isWritable() { return !isRoot && physicalFile.canWrite(); }
//    @Override public boolean isRemovable() { return !isRoot && physicalFile.canWrite(); }
//    @Override public String getOwnerName() { return "ftp"; }
//    @Override public String getGroupName() { return "ftp"; }
//    @Override public int getLinkCount() { return isDirectory() ? 3 : 1; }
//    @Override public long getLastModified() { return isRoot ? System.currentTimeMillis() : physicalFile.lastModified(); }
//    @Override public boolean setLastModified(long time) { return !isRoot && physicalFile.setLastModified(time); }
//    @Override public long getSize() { return isRoot ? 0 : physicalFile.length(); }
//    @Override public Object getPhysicalFile() { return physicalFile; }
//    @Override public boolean mkdir() { return !isRoot && physicalFile.mkdir(); }
//    @Override public boolean delete() { return !isRoot && physicalFile.delete(); }
//    @Override public boolean move(FtpFile dest) { return !isRoot && physicalFile.renameTo((File) dest.getPhysicalFile()); }
//    @Override public OutputStream createOutputStream(long offset) throws IOException { return new java.io.FileOutputStream(physicalFile); }
//    @Override public InputStream createInputStream(long offset) throws IOException { return new java.io.FileInputStream(physicalFile); }
//}
//public class FtpVfs implements FileSystemView {
//    private final Map<String, File> virtualRoutes;
//    private String currentVirtualDir = "/";
//
//    public FtpVfs(Map<String, File> virtualRoutes) {
//        this.virtualRoutes = virtualRoutes;
//    }
//
//    @Override
//    public FtpFile getHomeDirectory() throws FtpException {
//        return new VirtualFtpFile(virtualRoutes);
//    }
//
//    @Override
//    public FtpFile getWorkingDirectory() throws FtpException {
//        return getFile(currentVirtualDir);
//    }
//
//    @Override
//    public boolean changeWorkingDirectory(String dir) throws FtpException {
//        // Simple absolute / relative normalization
//        String target = dir.startsWith("/") ? dir : (currentVirtualDir.equals("/") ? "/" + dir : currentVirtualDir + "/" + dir);
//
//        FtpFile file = getFile(target);
//        if (file.doesExist() && file.isDirectory()) {
//            this.currentVirtualDir = file.getAbsolutePath();
//            return true;
//        }
//        return false;
//    }
//
//    @Override
//    public FtpFile getFile(String path) throws FtpException {
//        // Clean path layout strings
//        if (path == null || path.equals("/") || path.isEmpty()) {
//            return new VirtualFtpFile(virtualRoutes);
//        }
//
//        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
//
//        // Find which virtual mapping matches the path prefix
//        for (Map.Entry<String, File> entry : virtualRoutes.entrySet()) {
//            String prefix = entry.getKey();
//            if (cleanPath.equals(prefix)) {
//                return new VirtualFtpFile("/" + prefix, entry.getValue(), virtualRoutes);
//            } else if (cleanPath.startsWith(prefix + "/")) {
//                String subPath = cleanPath.substring(prefix.length() + 1);
//                File targetPhysicalFile = new File(entry.getValue(), subPath);
//                return new VirtualFtpFile("/" + prefix + "/" + subPath, targetPhysicalFile, virtualRoutes);
//            }
//        }
//
//        // Path doesn't belong to any registered virtual mapping
//        return new VirtualFtpFile("/" + cleanPath, new File("/nonexistent-path"), virtualRoutes);
//    }
//
//    @Override
//    public boolean isRandomAccessible() throws FtpException {
//        return false;
//    }
//
//    @Override
//    public void dispose() {
//        // Cleanup resources if necessary
//    }
//}
//public class MultiDirectoryFileSystemFactory implements FileSystemFactory {
//
//    private final Map<String, Path> virtualToPhysicalMap = new ConcurrentHashMap<>();
//
//    /**
//     * Map a virtual folder name to a concrete host directory.
//     * @param virtualFolderName e.g., "shared-docs"
//     * @param physicalPath e.g., Paths.get("/var/data/documents")
//     */
//    public void registerVirtualDirectory(String virtualFolderName, Path physicalPath) {
//        if (virtualFolderName.contains("/") || virtualFolderName.contains("\\")) {
//            throw new IllegalArgumentException("Virtual folder name cannot contain path separators");
//        }
//        this.virtualToPhysicalMap.put(virtualFolderName, physicalPath);
//    }
//
//    @Override
//    public Path getUserHomeDir(SessionContext session) throws IOException {
//        return null;
//    }
//
//    @Override
//    public FileSystem createFileSystem(SessionContext session) throws IOException {
//        // We initialize the base filesystem (usually the host machine standard default view)
//        FileSystem defaultFs = FileSystems.getDefault();
//
//        // Return a proxy/delegating FileSystem tailored to routing our custom map
//        return new MultiDirectoryFileSystem(defaultFs, virtualToPhysicalMap);
//    }
//}
//public class MultiDirectoryFileSystem extends FileSystem {
//
//    private final FileSystem delegate;
//    private final Map<String, Path> routingMap;
//
//    public MultiDirectoryFileSystem(FileSystem delegate, Map<String, Path> routingMap) {
//        this.delegate = delegate;
//        this.routingMap = routingMap;
//    }
//
//    @Override
//    public FileSystemProvider provider() {
//        return delegate.provider(); // Re-use the underlying native OS provider operations
//    }
//
//    @Override
//    public Path getPath(String first, String... more) {
//        // Clean up incoming path references (e.g., standardizing user lookups from SFTP client)
//        String cleanFirst = first.replace("\\", "/");
//        if (cleanFirst.startsWith("/")) {
//            cleanFirst = cleanFirst.substring(1);
//        }
//
//        // If checking the root list, or resolving paths via virtual folders
//        for (Map.Entry<String, Path> entry : routingMap.entrySet()) {
//            if (cleanFirst.startsWith(entry.getKey())) {
//                Path physicalBase = entry.getValue();
//                String subPath = cleanFirst.substring(entry.getKey().length());
//                if (subPath.startsWith("/")) {
//                    subPath = subPath.substring(1);
//                }
//
//                // Append remaining directories if user targets a subfolder nested deep within
//                if (more.length > 0) {
//                    return physicalBase.resolve(subPath).getFileSystem().getPath(physicalBase.resolve(subPath).toString(), more);
//                }
//                return physicalBase.resolve(subPath);
//            }
//        }
//
//        // Default fallback loop back to standard path generation
//        return delegate.getPath(first, more);
//    }
//
//    // --- Standard Delegate Boilerplate Methods ---
//    @Override public void close() throws IOException { delegate.close(); }
//    @Override public boolean isOpen() { return delegate.isOpen(); }
//    @Override public boolean isReadOnly() { return delegate.isReadOnly(); }
//    @Override public String getSeparator() { return delegate.getSeparator(); }
//    @Override public Iterable<Path> getRootDirectories() { return delegate.getRootDirectories(); }
//    @Override public Iterable<FileStore> getFileStores() { return delegate.getFileStores(); }
//    @Override public Set<String> supportedFileAttributeViews() { return delegate.supportedFileAttributeViews(); }
//    @Override public PathMatcher getPathMatcher(String syntaxAndPattern) { return delegate.getPathMatcher(syntaxAndPattern); }
//    @Override public UserPrincipalLookupService getUserPrincipalLookupService() { return delegate.getUserPrincipalLookupService(); }
//    @Override public WatchService newWatchService() throws IOException { return delegate.newWatchService(); }
//}
//
