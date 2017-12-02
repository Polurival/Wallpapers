package com.github.polurival.wallpapers.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.polurival.wallpapers.R;
import com.github.polurival.wallpapers.util.Helper;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import jp.wasabeef.picasso.transformations.BlurTransformation;

/**
 * Экран для отображения открываемой картинки
 *
 * @author Polurival on 12.11.2017.
 */

public class TumblrPagerActivity extends Activity {

    private static final String STATE_POSITION = "STATE_POSITION";

    private ViewPager imagePager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager);

        Bundle bundle = getIntent().getExtras();
        assert bundle != null;
        ArrayList<TumblrItem> tumblrItems = bundle.getParcelableArrayList(Constants.Extra.IMAGES);
        int pagerPosition = bundle.getInt(Constants.Extra.IMAGE_POSITION, 0);

        if (savedInstanceState != null) {
            pagerPosition = savedInstanceState.getInt(STATE_POSITION);
        }

        imagePager = findViewById(R.id.pager);
        if (tumblrItems != null) {
            imagePager.setAdapter(new ImagePagerAdapter(tumblrItems));
            imagePager.setCurrentItem(pagerPosition);
        }

        Helper.admobLoader(this, findViewById(R.id.adView));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_POSITION, imagePager.getCurrentItem());
    }

    private class ImagePagerAdapter extends PagerAdapter {

        private ArrayList<TumblrItem> images;

        ImagePagerAdapter(ArrayList<TumblrItem> images) {
            this.images = images;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return images.size();
        }


        /**
         * Создание вью с картинкой, фоновым изображением и кнопками управления
         */
        @Override
        public Object instantiateItem(ViewGroup view, final int position) {
            View imageLayout = getLayoutInflater().inflate(R.layout.activity_pager_image, view, false);
            assert imageLayout != null;
            final ImageView imageView = imageLayout.findViewById(R.id.image);
            final ImageView imageViewBg = imageLayout.findViewById(R.id.imageBg);

            final ImageButton btnShare = imageLayout.findViewById(R.id.btnShare);
            final ImageButton btnSet = imageLayout.findViewById(R.id.btnSet);
            final ImageButton btnSave = imageLayout.findViewById(R.id.btnSave);

            final ProgressBar spinner = imageLayout.findViewById(R.id.loading);
            spinner.setVisibility(View.VISIBLE);

            Picasso.with(TumblrPagerActivity.this).load(images.get(position).getUrl())
                    .into(imageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            spinner.setVisibility(View.GONE);

                            imageView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    btnSave.setVisibility(View.VISIBLE);
                                    btnShare.setVisibility(View.VISIBLE);
                                    btnSet.setVisibility(View.VISIBLE);
                                }
                            });

                            btnSave.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String path = Environment.getExternalStorageDirectory().toString();
                                    OutputStream fOut = null;
                                    File file = new File(path, "wallpaper_" + images.get(position).getId() + ".jpg");
                                    try {
                                        fOut = new FileOutputStream(file);
                                        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 99, fOut);
                                        fOut.flush();
                                        fOut.close();

                                        MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());

                                        String saved = getResources().getString(R.string.saved);
                                        Toast.makeText(getBaseContext(), saved + " " + file.toString(), Toast.LENGTH_LONG).show();
                                    } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                            btnShare.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String appValue = getResources().getString(R.string.share_begin);
                                    String applicationName = getResources().getString(R.string.app_name);

                                    Intent shareIntent = new Intent();
                                    shareIntent.setType("text/plain");
                                    shareIntent.setAction(Intent.ACTION_SEND);
                                    shareIntent.putExtra(Intent.EXTRA_TEXT, appValue + " " + applicationName + ": " + images.get(position).getLink());
                                    TumblrPagerActivity.this.startActivity(Intent.createChooser(shareIntent, "Share"));
                                }
                            });

                            btnSet.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(TumblrPagerActivity.this);
                                    builder.setMessage(getResources().getString(R.string.set_confirm))
                                            .setCancelable(true)
                                            .setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    try {
                                                        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                                                        WallpaperManager myWallpaperManager = WallpaperManager.getInstance(TumblrPagerActivity.this);
                                                        myWallpaperManager.setBitmap(bitmap);
                                                        Toast.makeText(TumblrPagerActivity.this, getResources().getString(R.string.set_success), Toast.LENGTH_SHORT).show();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }

                                                }
                                            });
                                    AlertDialog alert = builder.create();
                                    alert.show();
                                }
                            });
                        }

                        @Override
                        public void onError() {
                            spinner.setVisibility(View.GONE);
                        }
                    });

            Picasso.with(TumblrPagerActivity.this).load(images.get(position).getUrl())
                    .transform(new BlurTransformation(TumblrPagerActivity.this))
                    .into(imageViewBg);

            view.addView(imageLayout, 0);
            return imageLayout;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }
    }
}