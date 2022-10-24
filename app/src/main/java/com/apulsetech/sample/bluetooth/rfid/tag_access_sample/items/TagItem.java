package com.apulsetech.sample.bluetooth.rfid.tag_access_sample.items;

import androidx.annotation.NonNull;

import java.util.Locale;

public class TagItem {

    public String tag;
    private int count;

    public TagItem(String tag) {
        this.tag = tag;
        this.count = 1;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void increamentCount() {
        this.count++;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.US,
                "[%s], %d", this.tag, this.count);
    }
}
