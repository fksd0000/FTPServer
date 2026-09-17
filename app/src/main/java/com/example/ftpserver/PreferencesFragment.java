package com.example.ftpserver;


import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.example.mylibrary.Settings;

import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreferencesFragment extends PreferenceFragmentCompat
{

    MainActivity mainActivity;
    List<String> localLocations = new ArrayList<>();
    public SwitchPreference serverSwitch;
    ListPreference serverRootLocationListPreference;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {



        View view = super.onCreateView(inflater, container, savedInstanceState);
        // Set an opaque background color (e.g., white or dark solid color)
        view.setBackgroundColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.white));

        return view;
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        getPreferenceManager().setSharedPreferencesName(null);

        PreferenceManager preferenceManager = getPreferenceManager();

        preferenceManager.setPreferenceDataStore(com.example.mylibrary.Settings.inMemoryDataStore);

        setPreferencesFromResource(R.xml.preferences_fragment, rootKey);

        serverRootLocationListPreference = findPreference("serverRootLocation");

        populateLocalLocations();
        mainActivity.actionBar.addOptionsItem("SAVE");
        Button button = (Button)mainActivity.actionBar.options.get(0);
        button.setOnClickListener(this::saveButton_onClick);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }




    private void saveButton_onClick(View view) {

        Settings.inMemoryDataStore.putBoolean("serverEnable", false);
//        int serverPort = Settings.inMemoryDataStore.getInt("serverPort", 8021);
//        String serverRootLocation = Settings.inMemoryDataStore.getString("serverRootLocation", "");
//        String serverRootPath = Settings.inMemoryDataStore.getString("serverRootPath", "");

        try {
            Settings.Save();
            Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(requireContext(), "An error occurred", Toast.LENGTH_SHORT).show();
        }

    }

    void populateLocalLocations() {
        StorageManager storageManager = (StorageManager) requireActivity().getSystemService(Context.STORAGE_SERVICE);
        List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
        File[] storageVolumes_ = ContextCompat.getExternalFilesDirs(getContext(), null);

        for (File volume : storageVolumes_) {
            if (volume != null) {
                // This outputs the app-specific path on each physical volume
                Log.d("StorageLocation", "Available volume path: " + volume.getAbsolutePath());
            }
        }
        String paths = "";
        for (StorageVolume volume : storageVolumes) {
            String description = volume.getDescription(requireContext()); // e.g., "Internal Storage" or "SD Card"
            boolean isPrimary = volume.isPrimary();
            boolean isRemovable = volume.isRemovable();
            Log.d("Storage", volume.getDirectory().getAbsolutePath());
            paths += volume.getDirectory().getAbsolutePath() + "\n";
            Log.d("Storage", description + " (Primary: " + isPrimary + ", Removable: " + isRemovable + ")");
            localLocations.add(description + " " + volume.getDirectory().getAbsolutePath());

        }

        serverRootLocationListPreference.setEntries(localLocations.toArray(new CharSequence[0]));
        serverRootLocationListPreference.setEntryValues(localLocations.toArray(new CharSequence[0]));
    }
}