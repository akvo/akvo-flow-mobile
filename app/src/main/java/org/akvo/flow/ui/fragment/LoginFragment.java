package org.akvo.flow.ui.fragment;

import android.app.Activity;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.User;

public class LoginFragment extends DialogFragment implements View.OnClickListener {

    public interface UserListener {
        void onUserUpdated(User user);
    }

    private EditText mUsername;
    private UserListener mListener;

    private String mTitle;
    private User mUser;

    public static LoginFragment newInstance(User user, String title) {
        LoginFragment f = new LoginFragment();
        Bundle args = new Bundle();
        args.putSerializable("user", user);
        args.putString("title", title);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUser = (User) getArguments().getSerializable("user");
            mTitle = getArguments().getString("title");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mListener = (UserListener)activity;
        } catch (ClassCastException e) {
            // Allow null listener
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);
        v.findViewById(R.id.login_btn).setOnClickListener(this);

        mUsername = (EditText) v.findViewById(R.id.username);

        if (mUser != null) {
            mUsername.setText(mUser.getName());
        }

        if (mTitle != null) {
            getDialog().setTitle(mTitle);
        }

        return v;
    }

    @Override
    public void onClick(View view) {
        // TODO: Validate
        String username = mUsername.getText().toString();
        Long id = mUser != null ? mUser.getId() : null;

        SurveyDbAdapter db = new SurveyDbAdapter(getActivity()).open();
        id = db.createOrUpdateUser(id, username);
        db.close();

        mUser = new User(id, username);

        User loggedUser = FlowApp.getApp().getUser();
        if (loggedUser != null && loggedUser.getId() == id) {
            loggedUser.setName(username);
        }

        if (mListener != null) {
            mListener.onUserUpdated(mUser);
        }

        dismiss();
    }
}
