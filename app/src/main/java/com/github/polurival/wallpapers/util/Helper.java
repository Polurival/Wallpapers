package com.github.polurival.wallpapers.util;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewAnimationUtils;

import com.github.polurival.wallpapers.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.security.ProviderInstaller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Polurival on 04.11.2017.
 */

public class Helper {

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo ni = null;
        if (cm != null) {
            ni = cm.getActiveNetworkInfo();
        }
        return ni != null && ni.isConnected();
    }

    public static boolean isOnlineShowDialog(Activity c) {
        return isOnline(c);
    }

    public static void setStatusBarColor(Activity mActivity, int color) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mActivity.getWindow().setStatusBarColor(color);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String loadJSONFromAsset(Context context, String name) {
        String json;
        try {
            InputStream is = context.getAssets().open(name);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return json;
    }

    public static String getDataFromUrl(String url) {

        StringBuilder chain = new StringBuilder();
        try {
            URL urlCon = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlCon.openConnection();
            connection.setRequestProperty("User-Agent", "Android");
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();

            int status = connection.getResponseCode();
            if ((status != HttpURLConnection.HTTP_OK) && (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER)) {


                String newUrl = connection.getHeaderField("Location");

                String cookies = connection.getHeaderField("Set-Cookie");


                connection = (HttpURLConnection) new URL(newUrl).openConnection();
                connection.setRequestProperty("Cookie", cookies);
                connection.setRequestProperty("User-Agent", "Android");
                connection.setRequestMethod("GET");
                connection.setDoInput(true);

                System.out.println("Redirect to URL : " + newUrl);
            }

            InputStream inputStream = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = rd.readLine()) != null) {
                chain.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return chain.toString();
    }

    /**
     * Проверяет наличие идентификатора рекламного баннера в строковых ресурсах и загружает рекламу
     */
    public static void admobLoader(Context context, View AdmobView) {
        String adId = context.getResources().getString(R.string.admob_banner_id);
        if (!adId.equals("")) {
            AdView adView = (AdView) AdmobView;
            adView.setVisibility(View.VISIBLE);

            AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
            adRequestBuilder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
            adView.loadAd(adRequestBuilder.build());
        }
    }

    /**
     * Отображает вью на экране, предварительно проигрывая анимацию открытия фрагмента
     */
    public static void revealView(View toBeRevealed, View frame) {

        if (ViewCompat.isAttachedToWindow(toBeRevealed)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

                int cx = (frame.getLeft() + frame.getRight()) / 2;
                int cy = (frame.getTop() + frame.getBottom()) / 2;

                int finalRadius = Math.max(frame.getWidth(), frame.getHeight());

                Animator anim = ViewAnimationUtils.createCircularReveal(
                        toBeRevealed, cx, cy, 0, finalRadius);

                toBeRevealed.setVisibility(View.VISIBLE);
                anim.start();
            } else {
                toBeRevealed.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Установка сертификатов для поддержки HTTPS на старых устройствах
     */
    public static void updateAndroidSecurityProvider(Activity callingActivity) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            try {
                ProviderInstaller.installIfNeeded(callingActivity);
            } catch (GooglePlayServicesRepairableException e) {

                GooglePlayServicesUtil.getErrorDialog(e.getConnectionStatusCode(), callingActivity, 0);
            } catch (GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
            }
        }
    }
}
