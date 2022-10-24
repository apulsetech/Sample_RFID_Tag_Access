package com.apulsetech.sample.bluetooth.rfid.tag_access_sample;

import android.os.Bundle;
import android.view.Menu;

import androidx.fragment.app.FragmentActivity;

import com.apulsetech.sample.bluetooth.rfid.tag_access_sample.R;

public class RfidLockKillActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rfid_activity_lock_kill);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_option, menu);
        return true;
    }


}
