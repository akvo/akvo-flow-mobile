package org.akvo.flow.activity;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.ui.fragment.LoginFragment;
import org.akvo.flow.ui.fragment.SurveysListFragment;

public class SetupActivity extends ActionBarActivity implements LoginFragment.LoginListener,
        SurveysListFragment.SurveyListListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startActivity(new Intent(this, SurveyActivity.class));
        finish();
        //displayLogin();
    }

    private void displayLogin() {
        setTitle("Add user");

        Fragment f = new LoginFragment();
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, f).commit();
    }

    private void displaySurveys() {
        setTitle("Select survey");

        Fragment f = new SurveysListFragment();
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, f).commit();
    }

    @Override
    public void onLogin() {
        displaySurveys();
    }

    @Override
    public void onSurveySelected(SurveyGroup sg) {
        // TODO
    }

}
