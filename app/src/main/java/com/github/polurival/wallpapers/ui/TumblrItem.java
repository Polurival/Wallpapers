package com.github.polurival.wallpapers.ui;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Модель элемента списка получаемых с сервера и распарсеных наборов данных,
 * состоящих из id, link и url.
 * Эти данные используются для загрузки картинок.
 */

public class TumblrItem implements Parcelable {

    private String id;
    private String link;
    private String url;

    public TumblrItem() {
        super();
    }

    public TumblrItem(String id, String link, String url) {
        super();
        this.id = id;
        this.link = link;
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getId() {
        return id;
    }

    public String getLink() {
        return link;
    }


    public TumblrItem(Parcel source) {
        id = source.readString();
        link = source.readString();
        url = source.readString();
    }

    public int describeContents() {
        return this.hashCode();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(link);
        dest.writeString(url);
    }

    public static final Creator<TumblrItem> CREATOR = new Parcelable.Creator<TumblrItem>() {
        public TumblrItem createFromParcel(Parcel in) {
            return new TumblrItem(in);
        }

        public TumblrItem[] newArray(int size) {
            return new TumblrItem[size];
        }
    };
}
