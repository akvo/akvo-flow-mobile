package org.akvo.flow.ui.fragment;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.akvo.flow.R;
import org.akvo.flow.activity.AddUserActivity;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.User;

public class LoginFragment extends Fragment implements View.OnClickListener {

    public interface LoginListener {
        void onLogin(User user);
    }

    private EditText mUsername;

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

        mUsername = (EditText) v.findViewById(R.id.username);

        if (!getActivity().getIntent().getBooleanExtra(AddUserActivity.EXTRA_FIRST_RUN, false)) {
            v.findViewById(R.id.logo).setVisibility(View.GONE);
        }

        return v;
    }

    @Override
    public void onClick(View view) {
        // TODO: Validate
        String username = mUsername.getText().toString();

        SurveyDbAdapter db = new SurveyDbAdapter(getActivity()).open();
        final long id = db.createOrUpdateUser(null, username, null);
        db.close();

        mListener.onLogin(new User(id, username, null));
    }
}
