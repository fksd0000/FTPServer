package com.example.ftpserver;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mylibrary.ActionBar;
import com.example.mylibrary.AlertDialog;
import com.example.mylibrary.SettingsFragment;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    String TITLE = "FTP Server";
    String SettingsFileName = "Settings.xml";
    String SettingsFilePath = "";


    SwitchCompat serverEnable_switch;
    EditText edittext_path;
    EditText serverPort_edittext;
    TextView availableLocations_textview;




    int UPDATE_INTERVAL = 100;
    ActionBar actionBar = null;

    PreferencesFragment preferencesFragment;


    private final Handler handler = new Handler(Looper.getMainLooper());


    private final ActivityResultLauncher<Intent> allFilesAccessLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {

                if (Environment.isExternalStorageManager() == false) {
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
                    finish();
                    // Permission denied. Inform the user they won't receive updates.
                    //showToast("Notification permission denied.");
                }
            });

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
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.addCategory("android.intent.category.DEFAULT");
            String packagename=getPackageName();
            intent.setData(Uri.parse(String.format("package:%s", packagename)));
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getApplicationContext().getPackageName());

            startActivity(intent);

        }

        InitActionBar();


        Init();
//        preferencesFragment = new PreferencesFragment();
//        preferencesFragment.mainActivity=this;
//        getSupportFragmentManager().beginTransaction()
//                .replace(R.id.fragment_container, preferencesFragment, null)
//                .commit();


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


        actionBar = new ActionBar(getApplicationContext(), (ViewGroup)findViewById(R.id.actionbar_container));
        actionBar.getRootView().setBackgroundColor(ContextCompat.getColor(this, R.color.my_statusbar_color));
        actionBar.setTitle(TITLE);

        actionBar.hideMoreButton();

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
        button_.setOnClickListener(this::saveButton_onClick);

    }
    private void Init() {


        edittext_path = findViewById(R.id.edittext_path);
        serverEnable_switch = findViewById(R.id.serverEnable_switch);
        serverEnable_switch.setOnCheckedChangeListener(this::serverEnable_switch_onCheckedChange);
        serverPort_edittext = findViewById(R.id.serverPort_edittext);

        availableLocations_textview = findViewById(R.id.availableLocations_textview);
        LoadSettings();
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

            DisplaySettings();
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
    public void DisplaySettings() {
        serverPort_edittext.setText((String)com.example.mylibrary.Settings.HashMap.get("serverPort"));
        edittext_path.setText((String)com.example.mylibrary.Settings.HashMap.get("serverRootPath"));
        UpdateUI();
        populateLocalLocations();
    }





    private void UpdateUI() {
        if (serverEnable_switch != null) {
            serverEnable_switch.setOnCheckedChangeListener(null);
            serverEnable_switch.setChecked(FTPServerService.IsRunning);
            serverEnable_switch.setOnCheckedChangeListener(this::serverEnable_switch_onCheckedChange);

        }
        //if (FTPService.IsRunning) {

//            textview_running.setText("YES");
//            button_start.setEnabled(false);
//            button_stop.setEnabled(true);
//

  //      }else {
//            textview_running.setText("NO");
//            button_start.setEnabled(true);
//            button_stop.setEnabled(false);
    //    }

//        if (preferencesFragment != null && preferencesFragment.serverSwitch != null) {
//            if (FTPService.IsRunning) {
//                preferencesFragment.serverSwitch.setEnabled(true);
//            } else {
//                preferencesFragment.serverSwitch.setEnabled(false);
//            }
//        }
    }
    public void serverEnable_switch_onCheckedChange(CompoundButton buttonView, boolean isChecked) {
        serverEnable_switch.setEnabled(false);
        if (isChecked) {
            FTPServerService.Path=edittext_path.getText().toString();
            FTPServerService.Port = Integer.parseInt(serverPort_edittext.getText().toString());
            FTPServerService.State = 0;
            Intent serviceIntent = new Intent(this, FTPServerService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (FTPServerService.State != 0) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            serverEnable_switch.setEnabled(true);
                            if (FTPServerService.IsRunning) {
                                serverEnable_switch.setChecked(true);
                                Toast.makeText(MainActivity.this, "Started", Toast.LENGTH_SHORT).show();
                            } else {
                                serverEnable_switch.setChecked(false);
                                Toast.makeText(MainActivity.this, "An error occurred", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
                }
            });
            thread.start();
        } else {
            FTPServerService.StopRequested = true;
            Intent serviceIntent = new Intent(this, FTPServerService.class);
            stopService(serviceIntent);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (FTPServerService.IsRunning) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            serverEnable_switch.setEnabled(true);
                            Toast.makeText(MainActivity.this, "Stopped", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            thread.start();

        }
    }
    private void saveButton_onClick(View view) {

        try {
            //DisplaySettings();
            com.example.mylibrary.Settings.inMemoryDataStore.putString("serverPort", serverPort_edittext.getText().toString());
            com.example.mylibrary.Settings.inMemoryDataStore.putString("serverRootPath", edittext_path.getText().toString());

            com.example.mylibrary.Settings.Save();
            Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "An error occurred", Toast.LENGTH_SHORT).show();
        }

    }
    void populateLocalLocations() {
        StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
        File[] storageVolumes_ = ContextCompat.getExternalFilesDirs(this, null);

        for (File volume : storageVolumes_) {
            if (volume != null) {
                // This outputs the app-specific path on each physical volume
                Log.d("StorageLocation", "Available volume path: " + volume.getAbsolutePath());
            }
        }
        String paths = "";
        for (StorageVolume volume : storageVolumes) {
            String description = volume.getDescription(this); // e.g., "Internal Storage" or "SD Card"
            boolean isPrimary = volume.isPrimary();
            boolean isRemovable = volume.isRemovable();
            Log.d("Storage", volume.getDirectory().getAbsolutePath());
            paths += volume.getDirectory().getAbsolutePath() + "\n";
            Log.d("Storage", description + " (Primary: " + isPrimary + ", Removable: " + isRemovable + ")");

            //localLocations.add(description + " " + volume.getDirectory().getAbsolutePath());

        }
        availableLocations_textview.setText(paths);
    }


    private final Runnable periodicUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                // Perform your UI update work here
                UpdateUI();
            } finally {
                // Schedule the next execution at the specified interval
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        }
    };

}





//public void button_start_onClick(View view) {
//    FTPService.Path=edittext_path.getText().toString();
//    Intent serviceIntent = new Intent(this, FTPService.class);
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//        startForegroundService(serviceIntent);
//    } else {
//        startService(serviceIntent);
//    }
//
//
//}
//public void button_stop_onClick(View view) {
//    FTPService.StopRequested = true;
//    //Intent serviceIntent = new Intent(this, FTPService.class);
//    //stopService(serviceIntent);
//
//}
