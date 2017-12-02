package com.github.polurival.wallpapers.ui;

import android.Manifest;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.polurival.wallpapers.MainActivity;
import com.github.polurival.wallpapers.R;
import com.github.polurival.wallpapers.inherit.PermissionsFragment;
import com.github.polurival.wallpapers.util.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Отображает сетку изображений на вкладке
 *
 * @author Polurival on 12.11.2017.
 */

public class WallpapersFragment extends Fragment implements PermissionsFragment {

    ArrayList<TumblrItem> tumblrItems;
    private ImageAdapter imageAdapter = null;

    private GridView listView;
    private LinearLayout rootLayout;

    RelativeLayout progressDialog;

    String perPage = "25";
    int currentPage;
    int total_posts;

    String baseUrl;

    Boolean initialLoad = true;
    Boolean isLoading = true;

    SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootLayout = (LinearLayout) inflater.inflate(R.layout.fragment_wallpapers, container, false);
        setHasOptionsMenu(true);

        // имя пользователя, со страницы которого будут загружаться картинки
        String username = this.getArguments().getStringArray(MainActivity.FRAGMENT_DATA)[0];
        baseUrl = "https://" + username + ".tumblr.com/api/read/json?type=photo&num=" + perPage + "&start=";

        listView = rootLayout.findViewById(R.id.gridview);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startImagePagerActivity(position);
            }
        });

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {

                if (imageAdapter == null)
                    return;

                if (imageAdapter.getCount() == 0)
                    return;

                int itemCount = visibleItemCount + firstVisibleItem;
                if (itemCount >= totalItemCount && !isLoading && (currentPage * Integer.parseInt(perPage)) <= total_posts) {

                    isLoading = true;
                    new InitialLoadGridView().execute(baseUrl);
                }
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }
        });

        mSwipeRefreshLayout = rootLayout.findViewById(R.id.swipe_refresh);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_light,
                android.R.color.holo_red_light, android.R.color.holo_green_light);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                if (!isLoading) {
                    initialLoad = true;
                    isLoading = true;
                    currentPage = 1;
                    tumblrItems.clear();
                    listView.setAdapter(null);
                    new InitialLoadGridView().execute(baseUrl);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.already_loading), Toast.LENGTH_LONG).show();
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }, 4000);

            }
        });

        return rootLayout;
    }

    /**
     * запускается загрузка изображений
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        new InitialLoadGridView().execute(baseUrl);
    }

    public void updateList() {
        if (initialLoad) {
            imageAdapter = new ImageAdapter(getActivity(), 0, tumblrItems);
            listView.setAdapter(imageAdapter);
            initialLoad = false;
        } else {
            imageAdapter.addAll(tumblrItems);
            imageAdapter.notifyDataSetChanged();
        }
        isLoading = false;
    }

    private void startImagePagerActivity(int position) {
        Intent intent = new Intent(getActivity(), TumblrPagerActivity.class);

        ArrayList<TumblrItem> underlying = new ArrayList<>();
        for (int i = 0; i < imageAdapter.getCount(); i++) {
            underlying.add(imageAdapter.getItem(i));
        }

        Bundle b = new Bundle();
        b.putParcelableArrayList(Constants.Extra.IMAGES, underlying);
        intent.putExtras(b);
        intent.putExtra(Constants.Extra.IMAGE_POSITION, position);
        startActivity(intent);
    }

    @Override
    public String[] requiredPermissions() {
        return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    }


    private class InitialLoadGridView extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            if (initialLoad) {
                progressDialog = rootLayout.findViewById(R.id.progressBarHolder);
            }
        }

        protected Void doInBackground(String... params) {
            String getUrl = params[0];
            getUrl = getUrl + Integer.toString((currentPage) * Integer.parseInt(perPage));
            currentPage = currentPage + 1;

            String jsonString = Helper.getDataFromUrl(getUrl);

            System.out.println("Return: " + jsonString);

            JSONObject json = null;
            try {
                jsonString = jsonString.replace("var tumblr_api_read = ", "");
                json = new JSONObject(jsonString);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (json == null) {
                return null;
            }

            try {
                String success = json.getString("posts-total");
                total_posts = Integer.parseInt(success);

                if (total_posts > 0) {

                    ArrayList<TumblrItem> images = new ArrayList<>();
                    JSONArray products = json.getJSONArray("posts");

                    for (int i = 0; i < products.length(); i++) {
                        JSONObject c = products.getJSONObject(i);

                        String id = c.getString("id");
                        String link = c.getString("url");
                        String url;
                        try {
                            // TODO: 12.11.2017 попробовать переделать на с.has(...)
                            url = c.getString("photo-url-1280");
                        } catch (JSONException e) {
                            try {
                                url = c.getString("photo-url-500");
                            } catch (JSONException r) {
                                try {
                                    url = c.getString("photo-url-250");
                                } catch (JSONException l) {
                                    url = null;
                                }
                            }
                        }

                        if (url != null) {
                            TumblrItem item = new TumblrItem(id, link, url);
                            images.add(item);
                        }
                    }

                    tumblrItems = images;
                } else {
                    Log.v("INFO", "No items found");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return (null);
        }

        protected void onPostExecute(Void unused) {
            if (tumblrItems != null) {
                updateList();
            }
            if (progressDialog.getVisibility() == View.VISIBLE) {
                progressDialog.setVisibility(View.GONE);
                Helper.revealView(listView, rootLayout);
            }
        }
    }
}
