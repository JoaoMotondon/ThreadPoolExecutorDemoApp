package com.motondon.threadpoolexecutordemoapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.motondon.threadpoolexecutordemoapp.R;
import com.motondon.threadpoolexecutordemoapp.common.Constants;
import com.motondon.threadpoolexecutordemoapp.service.DummyService;

public class CurrentTestActivity extends AppCompatActivity {

    private static final String TAG = CurrentTestActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_test);

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(CurrentTestFragment.TAG);

        if (fragment == null) {
            Intent i = getIntent();

            if (i == null) {
                throw new IllegalStateException("This should never happen. Expect NUMBER_OF_REQUESTED_TASKS parameter.");
            }

            fragment = new CurrentTestFragment();

            // Pass args from the MainFragment to the CurrentTestFragment. There is no need to extract it, just pass it along.
            fragment.setArguments(i.getExtras());

            getSupportFragmentManager().beginTransaction().replace(R.id.current_test_activity_container_id, fragment, CurrentTestFragment.TAG).commit();
        }

        // Code to enable Up/Home button on the status bar
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");

        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:

                // Call DummyService by passing DESTROY action. This will call stopSelf()
                Intent i = new Intent(getApplicationContext(), DummyService.class);
                i.setAction(Constants.DESTROY);
                startService(i);


                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}