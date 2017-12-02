package com.github.polurival.wallpapers.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.github.polurival.wallpapers.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Адаптер для заполнения сетки изображений на вкладке
 *
 * @author Polurival on 12.11.2017.
 */

public class ImageAdapter extends ArrayAdapter<TumblrItem> {

    private ArrayList<TumblrItem> listData;

    public ImageAdapter(Context context, Integer something, ArrayList<TumblrItem> listData) {
        super(context, something, listData);
        this.listData = listData;
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public TumblrItem getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final ViewHolder holder;
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_row, parent, false);
            holder = new ViewHolder();
            assert view != null;
            holder.imageView = view.findViewById(R.id.image);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        Picasso.with(getContext())
                .load(listData.get(position).getUrl())
                .placeholder(R.drawable.placeholder)
                .fit()
                .centerCrop()
                .into(holder.imageView);

        return view;
    }

    private static class ViewHolder {
        ImageView imageView;
    }
}
