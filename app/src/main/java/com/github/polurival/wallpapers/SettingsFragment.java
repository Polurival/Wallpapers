package com.github.polurival.wallpapers;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;

import preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment implements BillingProcessor.IBillingHandler {

    boolean HIDE_RATE_MY_APP = false;

    String start;
    String menu;
    BillingProcessor billingProcessor;
    Preference preferencePurchase;

    AlertDialog dialog;

    private static String PRODUCT_ID_BOUGHT = "item_1_bought";
    public static String SHOW_DIALOG = "show_dialog";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.activity_settings);

        // при нажатии пункта "Оценить приложение" создается ссылка на приложение в маркете
        // и интент для открытия ссылки.
        Preference preferenceRate = findPreference("rate");
        preferenceRate.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Uri uri = Uri.parse("market://details?id="
                        + getActivity().getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getActivity(),
                            R.string.not_open_playstore,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                return true;
            }
        });

        // Определяем пункт покупок, проверяем наличие лицензионного ключа для приложения,
        // создаем экземпляр класса BillingProcessor, который загружает список покупок из Google Play.
        // При нажатии пункта "Отключение рекламы" происходит покупка.
        // Если покупка уже произведена, устанавливается иконка-флажок, а настройка записывается в базу данных настроек.
        preferencePurchase = findPreference("purchase");
        String license = getResources().getString(R.string.google_play_license);
        if (!license.equals("")) {
            billingProcessor = new BillingProcessor(getActivity(), license, this);
            billingProcessor.loadOwnedPurchasesFromGoogle();

            preferencePurchase.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    billingProcessor.purchase(getActivity(), getProductId());
                    return true;
                }
            });

            if (getIsPurchased(getActivity())) {
                preferencePurchase.setIcon(R.drawable.ic_done_24px);
            }
        } else {
            PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("preferenceScreen");
            PreferenceCategory billing = (PreferenceCategory) findPreference("billing");
            preferenceScreen.removePreference(billing);
        }

        // Ниже идет построение и управление диалоговым окном,
        // которое отображается при попытке просмотра контента с ограниченным доступом
        // из панели навигации с предложением выполнить покупку.
        String[] extra = getArguments().getStringArray(MainActivity.FRAGMENT_DATA);
        if (null != extra && extra.length != 0 && extra[0].equals(SHOW_DIALOG)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setPositiveButton(R.string.settings_purchase, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    billingProcessor.purchase(getActivity(), getProductId());
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                }
            });
            builder.setTitle(getResources().getString(R.string.dialog_purchase_title));
            builder.setMessage(getResources().getString(R.string.dialog_purchase));

            dialog = builder.create();
            dialog.show();
        }

        if (HIDE_RATE_MY_APP) {
            PreferenceCategory other = (PreferenceCategory) findPreference("other");
            Preference preference = findPreference("rate");
            other.removePreference(preference);
        }
    }

    @Override
    public void onBillingInitialized() {
    }

    /**
     * Метод onProductPurchased выполняется при успешной покупке,
     * здесь вызывается метод setIsPurchased,
     * записывающий состояние в базу данных настроек,
     * и выдается тост с сообщением о том, что покупка подтверждена.
     *
     * @param productId
     * @param details
     */
    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        if (productId.equals(getProductId())) {
            setIsPurchased(true, getActivity());
            preferencePurchase.setIcon(R.drawable.ic_done_24px);
            Toast.makeText(getActivity(), getResources().getString(R.string.settings_purchase_success), Toast.LENGTH_LONG).show();
        }
        Log.v("INFO", "Purchase purchased");
    }

    /**
     * Метод onBillingError вызывается при ошибке покупки.
     *
     * @param errorCode
     * @param error
     */
    @Override
    public void onBillingError(int errorCode, Throwable error) {
        Toast.makeText(getActivity(), getResources().getString(R.string.settings_purchase_fail), Toast.LENGTH_LONG).show();
        Log.v("INFO", "Error");
    }

    /**
     * Метод onPurchaseHistoryRestored вызывается при загрузке истории покупок,
     * здесь проверяем наличие покупки, сохраняем настройку, устанавливаем иконку покупки,
     * закрываем диалог и выводим тост о восстановлении покупки.
     * Эти действия необходимы, например, при повторнной загрузке приложения,
     * когда факт покупки был ранее, но еще не сохранен в настройках приложения.
     */
    @Override
    public void onPurchaseHistoryRestored() {
        if (billingProcessor.isPurchased(getProductId())) {
            setIsPurchased(true, getActivity());
            Log.v("INFO", "Purchase actually restored");
            preferencePurchase.setIcon(R.drawable.ic_done_24px);
            if (dialog != null) dialog.cancel();
            Toast.makeText(getActivity(), getResources().getString(R.string.settings_restore_purchase_success), Toast.LENGTH_LONG).show();
        }
        Log.v("INFO", "Purchase restored called");
    }

    public void setIsPurchased(boolean purchased, Context c) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(c);

        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean(PRODUCT_ID_BOUGHT, purchased);
        editor.apply();
    }

    public static boolean getIsPurchased(Context c) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(c);

        boolean prefson = prefs.getBoolean(PRODUCT_ID_BOUGHT, false);

        return prefson;
    }

    private String getProductId() {
        return getResources().getString(R.string.product_id);
    }


    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        billingProcessor.handleActivityResult(requestCode, resultCode, intent);
    }


    @Override
    public void onDestroy() {
        if (billingProcessor != null)
            billingProcessor.release();

        super.onDestroy();
    }
}
