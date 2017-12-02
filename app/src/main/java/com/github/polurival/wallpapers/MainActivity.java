package com.github.polurival.wallpapers;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.github.polurival.wallpapers.drawer.MenuItemCallback;
import com.github.polurival.wallpapers.drawer.NavItem;
import com.github.polurival.wallpapers.drawer.SimpleMenu;
import com.github.polurival.wallpapers.drawer.TabAdapter;
import com.github.polurival.wallpapers.inherit.BackPressFragment;
import com.github.polurival.wallpapers.inherit.PermissionsFragment;
import com.github.polurival.wallpapers.util.DisableableViewPager;
import com.github.polurival.wallpapers.util.Helper;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Polurival on 04.11.2017.
 */

public class MainActivity extends AppCompatActivity implements MenuItemCallback, ConfigParser.CallBack {

    private NavigationView navigationView;
    private static SimpleMenu menu;

    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private TabLayout tabLayout;
    private DisableableViewPager viewPager;

    public static String FRAGMENT_DATA = "transaction_data";
    public static String FRAGMENT_CLASS = "transaction_target";

    public static boolean TABLET_LAYOUT = true;

    List<NavItem> queueItem;
    MenuItem queueMenuItem;

    private TabAdapter adapter;
    private int interstitialCount = -1;

    /**
     * Проверяется корректность загрузки конфигурации из json-файла.
     *
     * @param facedException есть ли исключение
     */
    @Override
    public void configLoaded(boolean facedException) {
        if (facedException || menu.getFirstMenuItem() == null) {
            if (Helper.isOnlineShowDialog(MainActivity.this))
                Toast.makeText(this, R.string.invalid_configuration, Toast.LENGTH_LONG).show();
        } else {
            menuItemClicked(menu.getFirstMenuItem().getValue(), menu.getFirstMenuItem().getKey(), false);
        }
    }

    /**
     * Инициализируем рекламный баннер и определяем конфигурацию экрана.
     *
     * @param savedInstanceState бандл
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileAds.initialize(this, getString(R.string.admob_app_id));

        if (useTabletMenu()) {
            setContentView(R.layout.activity_main_tablet);
            Helper.setStatusBarColor(MainActivity.this,
                    ContextCompat.getColor(this, R.color.myPrimaryDarkColor));
        } else {
            setContentView(R.layout.activity_main);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            if (!useTabletMenu())
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            else {
                getSupportActionBar().setDisplayShowHomeEnabled(false);
            }
        }

        if (!useTabletMenu()) {
            drawer = findViewById(R.id.drawer);
            toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();
        }

        tabLayout = findViewById(R.id.tabs);
        viewPager = findViewById(R.id.viewpager);

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(FRAGMENT_CLASS)) {
            try {
                Class<? extends Fragment> fragmentClass = (Class<? extends Fragment>) getIntent().getExtras().getSerializable(FRAGMENT_CLASS);
                if (fragmentClass != null) {
                    String[] extra = getIntent().getExtras().getStringArray(FRAGMENT_DATA);
                    HolderActivity.startActivity(this, fragmentClass, extra);
                    finish();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Menu items
        navigationView = findViewById(R.id.nav_view);
        menu = new SimpleMenu(navigationView.getMenu(), this);
        if (Config.USE_HARDCODED_CONFIG) {
            Config.configureMenu(menu, this);
        } else if (!Config.CONFIG_URL.isEmpty() && Config.CONFIG_URL.contains("http"))
            new ConfigParser(Config.CONFIG_URL, menu, this, this).execute();
        else
            new ConfigParser("config.json", menu, this, this).execute();
        tabLayout.setupWithViewPager(viewPager);

        if (!useTabletMenu()) {
            drawer.setStatusBarBackgroundColor(
                    ContextCompat.getColor(this, R.color.myPrimaryDarkColor));
        }

        applyDrawerLocks();

        Helper.admobLoader(this, findViewById(R.id.adView));
        Helper.updateAndroidSecurityProvider(this);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                onTabBecomesActive(position);
            }
        });
    }

    @Override
    public void onBackPressed() {
        Fragment activeFragment = null;
        if (adapter != null) {
            activeFragment = adapter.getCurrentFragment();
        }

        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (activeFragment instanceof BackPressFragment) {
            boolean handled = ((BackPressFragment) activeFragment).handleBackPress();
            if (!handled) {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Проверяем разрешения
     *
     * @param requestCode  код запроса
     * @param permissions  разрешения
     * @param grantResults признаки включения разрешений
     */
    @SuppressLint("WrongConstant")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                boolean foundFalse = false;
                int res;
                for (String permission : permissions) {
                    res = checkCallingOrSelfPermission(permission);
                    if (!(res == PackageManager.PERMISSION_GRANTED)) {
                        foundFalse = true;
                    }
                }

                if (!foundFalse) {
                    menuItemClicked(queueItem, queueMenuItem, false);
                } else {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.permissions_required), Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Проверяем настройку отображения меню при первом старте приложения,
     * обрабатываем нажатия пунктов меню и поведение панели навигации.
     *
     * @param actions          список моделей меню
     * @param item             элемент меню
     * @param requiresPurchase признак платного меню
     */
    @Override
    public void menuItemClicked(List<NavItem> actions, MenuItem item, boolean requiresPurchase) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean openOnStart = prefs.getBoolean("menuOpenOnStart", false);
        if (drawer != null) {
            if (openOnStart && !useTabletMenu() && adapter == null) {
                drawer.openDrawer(GravityCompat.START);
            } else {
                drawer.closeDrawer(GravityCompat.START);
            }
        }

        if (!checkPermissionsHandleIfNeeded(actions, item)) {
            return;
        }

        if (item != null) {
            for (MenuItem menuItem : menu.getMenuItems()) {
                menuItem.setChecked(false);
            }
            item.setChecked(true);
        }

        adapter = new TabAdapter(getSupportFragmentManager(), actions, this);
        viewPager.setAdapter(adapter);

        if (actions.size() == 1) {
            tabLayout.setVisibility(View.GONE);
            viewPager.setPagingEnabled(false);
        } else {
            tabLayout.setVisibility(View.VISIBLE);
            viewPager.setPagingEnabled(true);
        }

        showInterstitial(false);
        onTabBecomesActive(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null)
            for (Fragment frag : fragments)
                if (frag != null)
                    frag.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Перерисовать всё, в случае смены конфигурации экрана
     *
     * @param newConfig контейнер информации о конфигурации экрана
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
    }

    /**
     * Проверяет, нужно ли отображать меню для планшетов, поскольку используется отдельный макет.
     *
     * @return признак планшета
     */
    public boolean useTabletMenu() {
        return (getResources().getBoolean(R.bool.isWideTablet) && TABLET_LAYOUT);
    }

    /**
     * Применяет соответствующие блокировки к панели навигации.
     */
    public void applyDrawerLocks() {
        if (drawer == null) {
            if (Config.HIDE_DRAWER)
                navigationView.setVisibility(View.GONE);
            return;
        }
        if (Config.HIDE_DRAWER) {
            toggle.setDrawerIndicatorEnabled(false);
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    /**
     * Метод для управления отображением межстраничного баннера
     */
    private void onTabBecomesActive(int position) {
        if (position != 0) {
            showInterstitial(true);
        }
    }

    /**
     * Метод для отображения межстраничного баннера
     */
    private void showInterstitial(boolean fromPager) {
        if (getResources().getString(R.string.admob_interstitial_id).length() == 0) {
            return;
        }

        if (interstitialCount == (Config.INTERSTITIAL_INTERVAL - 1)) {
            AdRequest adRequestInter = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .build();
            final InterstitialAd interstitialAd = new InterstitialAd(this);
            interstitialAd.setAdUnitId(getResources().getString(R.string.admob_interstitial_id));
            interstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    interstitialAd.show();
                }
            });
            interstitialAd.loadAd(adRequestInter);

            interstitialCount = 0;
        } else {
            interstitialCount++;
        }
    }

    /**
     * Метод проверяет, имеет ли элемент достаточные разрешения прежде чем быть открытым.
     * Принимает вкладки для проверки и возвращает true, если элемент безопасен для открытия
     */
    private boolean checkPermissionsHandleIfNeeded(List<NavItem> tabs, MenuItem item) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return true;
        }

        List<String> allPermissions = new ArrayList<>();
        for (NavItem tab : tabs) {
            if (PermissionsFragment.class.isAssignableFrom(tab.getFragment())) {
                try {
                    allPermissions.addAll(Arrays.asList(((PermissionsFragment) tab.getFragment().newInstance()).requiredPermissions()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (allPermissions.size() > 1) {
            boolean allGranted = true;
            for (String permission : allPermissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                }
            }

            if (!allGranted) {
                requestPermissions(allPermissions.toArray(new String[0]), 1);
                queueItem = tabs;
                queueMenuItem = item;
                return false;
            }

            return true;
        }

        return true;
    }
}
