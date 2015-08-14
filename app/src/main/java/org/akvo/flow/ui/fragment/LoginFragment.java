package org.akvo.flow.ui.fragment;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.akvo.flow.R;

public class LoginFragment extends Fragment implements View.OnClickListener {

    public interface LoginListener {
        void onLogin();
    }

    private LoginListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mListener = (LoginListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement LoginListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);
        v.findViewById(R.id.login_btn).setOnClickListener(this);

        return v;
    }

    @Override
    public void onClick(View view) {
        mListener.onLogin();
    }
}
