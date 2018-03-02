package com.luck.picture.lib.entity;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created on 2017/7/28.
 * Author：yy
 * Description:拍照、拍视频数据
 */


public class CameraMedia implements Parcelable {
    private int type;
    private String url;
    private Bitmap bitmap;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeString(url);
        dest.writeParcelable(bitmap, flags);
    }

    public CameraMedia() {
    }

    protected CameraMedia(Parcel in) {
        this.url = in.readString();
        this.type = in.readInt();
        this.bitmap = in.readParcelable(Bitmap.class.getClassLoader());
    }

    public static final Creator<CameraMedia> CREATOR = new Creator<CameraMedia>() {
        @Override
        public CameraMedia createFromParcel(Parcel source) {
            return new CameraMedia(source);
        }

        @Override
        public CameraMedia[] newArray(int size) {
            return new CameraMedia[size];
        }
    };
}
