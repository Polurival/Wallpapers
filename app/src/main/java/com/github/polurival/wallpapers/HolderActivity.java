package com.github.polurival.wallpapers;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.polurival.wallpapers.inherit.BackPressFragment;
import com.github.polurival.wallpapers.inherit.PermissionsFragment;
import com.github.polurival.wallpapers.util.Helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HolderActivity extends AppCompatActivity {

    private Class<? extends Fragment> queueItem;
    private String[] queueItemData;

    public static void startActivity(Activity activity, Class<? extends Fragment> fragment, String[] data) {
        Bundle bundle = new Bundle();
        bundle.putStringArray(MainActivity.FRAGMENT_DATA, data);
        bundle.putSerializable(MainActivity.FRAGMENT_CLASS, fragment);

        Intent intent = new Intent(activity, HolderActivity.class);
        intent.putExtras(bundle);
        activity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_holder);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Class<? extends Fragment> fragmentClass = (Class<? extends Fragment>) getIntent().getExtras().getSerializable(MainActivity.FRAGMENT_CLASS);
        String[] args = getIntent().getExtras().getStringArray(MainActivity.FRAGMENT_DATA);

        openFragment(fragmentClass, args);

        Helper.admobLoader(this, findViewById(R.id.adView));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.settings:
                openFragment(SettingsFragment.class, new String[0]);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);

        if (fragment instanceof BackPressFragment) {
            boolean handled = ((BackPressFragment) fragment).handleBackPress();
            if (!handled) {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    public void openFragment(Class<? extends Fragment> fragmentClass, String[] data) {
        if (!checkPermissionsHandleIfNeeded(fragmentClass, data)) {
            return;
        }
        try {
            Fragment fragment = fragmentClass.newInstance();

            Bundle bundle = new Bundle();
            bundle.putStringArray(MainActivity.FRAGMENT_DATA, data);
            fragment.setArguments(bundle);

            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean checkPermissionsHandleIfNeeded(Class<? extends Fragment> fragment, String[] data) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) return true;

        List<String> allPermissions = new ArrayList<>();
        if (PermissionsFragment.class.isAssignableFrom(fragment)) {
            try {
                allPermissions.addAll(Arrays.asList(((PermissionsFragment) fragment.newInstance()).requiredPermissions()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (allPermissions.size() > 1) {
            boolean allGranted = true;
            for (String permission : allPermissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                    allGranted = false;
            }

            if (!allGranted) {
                //TODO An explanation before asking
                requestPermissions(allPermissions.toArray(new String[0]), 1);
                queueItem = fragment;
                queueItemData = data;
                return false;
            }

            return true;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                boolean foundfalse = false;
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        foundfalse = true;
                    }
                }
                if (!foundfalse) {
                    openFragment(queueItem, queueItemData);
                } else {
                    Toast.makeText(HolderActivity.this, getResources().getString(R.string.permissions_required), Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
