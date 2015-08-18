package org.akvo.flow.activity;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.User;
import org.akvo.flow.ui.fragment.LoginFragment;

public class SetupActivity extends ActionBarActivity implements LoginFragment.LoginListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new LoginFragment()).commit();
    }

    @Override
    public void onLogin(User user) {
        FlowApp.getApp().setUser(user);
        setResult(RESULT_OK);
        finish();
    }

}
