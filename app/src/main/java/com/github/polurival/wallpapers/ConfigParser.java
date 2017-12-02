package com.github.polurival.wallpapers;

import android.app.Activity;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.github.polurival.wallpapers.drawer.NavItem;
import com.github.polurival.wallpapers.drawer.SimpleMenu;
import com.github.polurival.wallpapers.drawer.SimpleSubMenu;
import com.github.polurival.wallpapers.ui.WallpapersFragment;
import com.github.polurival.wallpapers.util.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Здесь мы в фоновом потоке получаем JSON, далее в потоке пользовательского интерфейса создаем меню,
 * или используем ранее созданное.
 * В цикле заполняем все пункты меню, определяем и устанавливаем разделители и подменю (если есть).
 * Далее определяем, этот пункт меню доступен только через покупки в приложении, или нет,
 * в зависимости от значения поля «iap» в файле config.json.
 * Затем, если этот элемент принадлежит подменю, добавляем его в подменю, иначе в верхнее меню.
 * В методе saveJSONToCache кешируем результат работы парсера — создаем объект JSON и сохраняем его.
 * В методе getJSONFromCache загружаем кешированный файл меню.
 *
 * @author Polurival on 04.11.2017.
 */

public class ConfigParser extends AsyncTask<Void, Void, Void> {

    private String sourceLocation;
    private final WeakReference<Activity> context;
    private SimpleMenu menu;
    private CallBack callback;

    private boolean facedException;

    private static JSONArray jsonMenu = null;

    private static String CACHE_FILE = "menuCache.srl";
    private static final long MAX_FILE_AGE = 60 * 60 * 24;

    public ConfigParser(String sourceLocation, SimpleMenu menu, Activity context, CallBack callback) {
        this.sourceLocation = sourceLocation;
        this.context = new WeakReference<>(context);
        this.menu = menu;
        this.callback = callback;
    }

    @Override
    protected Void doInBackground(Void... args) {

        if (jsonMenu == null)
            try {

                if (sourceLocation.contains("http")) {
                    jsonMenu = getJSONFromCache();
                    if (getJSONFromCache() == null) {
                        Log.v("INFO", "Loading Menu Config from url.");
                        String jsonStr = Helper.getDataFromUrl(sourceLocation);
                        jsonMenu = new JSONArray(jsonStr);
                        saveJSONToCache(jsonStr);
                    } else {
                        Log.v("INFO", "Loading Menu Config from cache.");
                    }
                } else {
                    if (context.get() != null) {
                        String jsonStr = Helper.loadJSONFromAsset(context.get(), sourceLocation);
                        jsonMenu = new JSONArray(jsonStr);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        if (jsonMenu != null && context.get() != null) {

            final JSONArray jsonMenuFinal = jsonMenu;

            context.get().runOnUiThread(new Runnable() {
                public void run() {

                    try {
                        SimpleSubMenu subMenu = null;

                        for (int i = 0; i < jsonMenuFinal.length(); i++) {
                            JSONObject jsonMenuItem = jsonMenuFinal.getJSONObject(i);

                            String menuTitle = jsonMenuItem.getString("title");

                            int menuDrawableResource = 0;
                            if (jsonMenuItem.has("drawable") &&
                                    jsonMenuItem.getString("drawable") != null
                                    && !jsonMenuItem.getString("drawable").isEmpty()
                                    && !jsonMenuItem.getString("drawable").equals("0"))
                                menuDrawableResource = getDrawableByName(jsonMenuItem.getString("drawable"));

                            if (jsonMenuItem.has("submenu")
                                    && jsonMenuItem.getString("submenu") != null
                                    && !jsonMenuItem.getString("submenu").isEmpty()) {
                                String menuSubMenu = jsonMenuItem.getString("submenu");

                                if (subMenu == null || !subMenu.getSubMenuTitle().equals(menuSubMenu))
                                    subMenu = new SimpleSubMenu(menu, menuSubMenu);
                            } else {
                                subMenu = null;
                            }

                            boolean requiresIap = false;
                            if (jsonMenuItem.has("iap")
                                    && jsonMenuItem.getBoolean("iap")) {
                                requiresIap = true;
                            }

                            List<NavItem> menuTabs = new ArrayList<NavItem>();

                            JSONArray jsonTabs = jsonMenuItem.getJSONArray("tabs");

                            for (int j = 0; j < jsonTabs.length(); j++) {
                                JSONObject jsonTab = jsonTabs.getJSONObject(j);

                                menuTabs.add(navItemFromJSON(jsonTab));
                            }

                            if (subMenu != null)
                                subMenu.add(menuTitle, menuDrawableResource, menuTabs, requiresIap);
                            else
                                menu.add(menuTitle, menuDrawableResource, menuTabs, requiresIap);
                        }

                    } catch (final JSONException e) {
                        e.printStackTrace();
                        Log.e("INFO", "JSON was invalid");
                        facedException = true;
                    }
                }
            });
        } else {
            Log.e("INFO", "JSON Could not be retrieved");
            facedException = true;
        }

        return null;
    }

    public static NavItem navItemFromJSON(JSONObject jsonTab) throws JSONException {
        String tabTitle = jsonTab.getString("title");

        Class<? extends Fragment> tabClass = WallpapersFragment.class;

        JSONArray args = jsonTab.getJSONArray("arguments");
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < args.length(); i++) {
            list.add(args.getString(i));
        }

        NavItem item = new NavItem(tabTitle, tabClass, list.toArray(new String[0]));

        if (jsonTab.has("image")
                && jsonTab.getString("image") != null
                && !jsonTab.getString("image").isEmpty()) {
            item.setCategoryImageUrl(jsonTab.getString("image"));
        }

        return item;
    }

    @Override
    protected void onPostExecute(Void args) {
        if (callback != null) {
            callback.configLoaded(facedException);
        }
    }

    public int getDrawableByName(String name) {
        if (context.get() == null) {
            Log.e("INFO", "Activity is Null, could not get Drawable resource Id");
            return 0;
        }
        Resources resources = context.get().getResources();
        return resources.getIdentifier(name, "drawable", context.get().getPackageName());
    }

    public interface CallBack {
        void configLoaded(boolean success);
    }


    public void saveJSONToCache(String json) {
        try {
            if (context.get() != null) {
                ObjectOutput out = new ObjectOutputStream(new FileOutputStream(new File(context.get().getCacheDir(), "") + CACHE_FILE));
                out.writeObject(json);
                out.close();
            } else {
                Log.e("INFO", "Activity is Null, could not save JSON to Cache");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONArray getJSONFromCache() {
        try {
            if (context.get() != null) {
                File cacheFile = new File(new File(context.get().getCacheDir(), "") + CACHE_FILE);
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(cacheFile));
                String jsonArrayRaw = (String) in.readObject();
                in.close();

                if (cacheFile.lastModified() + MAX_FILE_AGE > System.currentTimeMillis())
                    return new JSONArray(jsonArrayRaw);
                else
                    return null;
            } else {
                Log.e("INFO", "Activity is Null, could not get JSON from Cache");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
}
