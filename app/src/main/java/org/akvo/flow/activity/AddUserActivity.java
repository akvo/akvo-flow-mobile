package org.akvo.flow.activity;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.User;
import org.akvo.flow.ui.fragment.LoginFragment;

public class AddUserActivity extends ActionBarActivity implements LoginFragment.UserListener {

    public static final String EXTRA_FIRST_RUN = "first_run";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final boolean firstRun = getIntent().getBooleanExtra(EXTRA_FIRST_RUN, false);
        if (firstRun) {
            // Hide Action Bar and logo
            supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        } else if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setIcon(R.drawable.ic_arrow_back_white_48dp);
        }

        setContentView(R.layout.add_user_activity);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content, LoginFragment.newInstance(null, null))
                .commit();
    }

    @Override
    public void onUserUpdated(User user) {
        FlowApp.getApp().setUser(user);
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
