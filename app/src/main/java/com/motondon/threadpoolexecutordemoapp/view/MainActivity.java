package com.motondon.threadpoolexecutordemoapp.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.motondon.threadpoolexecutordemoapp.R;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
       Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(MainFragment.TAG);

        if (fragment == null) {
            fragment = new MainFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.main_container_id, fragment, MainFragment.TAG).commit();
        }
    }
}
