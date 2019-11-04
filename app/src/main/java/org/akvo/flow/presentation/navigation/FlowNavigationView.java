/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.akvo.flow.presentation.navigation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.google.android.material.internal.NavigationMenuView;
import com.google.android.material.navigation.NavigationView;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.SnackBarManager;

import java.util.List;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FlowNavigationView extends NavigationView implements IFlowNavigationView {

    private TextView currentUserTextView;
    private TextView surveyTitleTextView;
    private RecyclerView surveysRecyclerView;
    private RecyclerView usersRecyclerView;
    private DrawerNavigationListener drawerNavigationListener;
    private SurveyAdapter surveyAdapter;
    private UserAdapter usersAdapter;
    private Drawable hideUsersDrawable;
    private Drawable showUsersDrawable;
    private View userHeader;

    @Inject
    FlowNavigationPresenter presenter;

    @Inject
    SnackBarManager snackBarManager;

    public FlowNavigationView(Context context) {
        this(context, null);
    }

    public FlowNavigationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        initialiseInjector();
        presenter.setView(this);
        getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        initViews();
                        setNavigationItemListener();
                        initCurrentUserText();
                        initUserList();
                        initSurveyList();
                        presenter.load();
                    }
                });
    }

    private void initViews() {
        currentUserTextView = findViewById(R.id.current_user_name);
        surveyTitleTextView = findViewById(R.id.surveys_title_tv);
        surveysRecyclerView = findViewById(R.id.surveys_rv);
        usersRecyclerView = findViewById( R.id.users_rv);
        userHeader = findViewById(R.id.user_header);
        NavigationMenuView navigationMenuView = (NavigationMenuView) getChildAt(0);
        if (navigationMenuView != null) {
            navigationMenuView.setVerticalScrollBarEnabled(false);
        }
    }

    private void initialiseInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    private ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getContext().getApplicationContext()).getApplicationComponent();
    }

    private void initUserList() {
        final Context context = getContext();
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        usersAdapter = new UserAdapter(context);
        usersRecyclerView.setAdapter(usersAdapter);
        usersRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(context,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View childView, int position) {
                        presenter.onUserSelected(usersAdapter.getItem(position));
                    }

                    @Override
                    public void onItemLongPress(View childView, int position) {
                        presenter.onUserLongPress(usersAdapter.getItem(position));
                    }
                })
        );
    }

    private void setNavigationItemListener() {
        findViewById(R.id.settings_tv).setOnClickListener(v -> {
            if (drawerNavigationListener != null) {
                drawerNavigationListener.navigateToSettings();
            }
        });

        findViewById(R.id.help_tv).setOnClickListener(v -> {
            if (drawerNavigationListener != null) {
                drawerNavigationListener.navigateToHelp();
            }
        });

        findViewById(R.id.about_tv).setOnClickListener(v -> {
            if (drawerNavigationListener != null) {
                drawerNavigationListener.navigateToAbout();
            }
        });
        findViewById(R.id.offline_maps_tv).setOnClickListener(v -> {
            drawerNavigationListener.navigateToOfflineMaps();
        });
    }

    private void initSurveyList() {
        final Context context = getContext();
        surveysRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        surveyAdapter = new SurveyAdapter(context);
        surveysRecyclerView.setAdapter(surveyAdapter);
        surveysRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(context,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View childView, int position) {
                        onSurveyItemTap(position);
                    }

                    @Override
                    public void onItemLongPress(View childView, int position) {
                        onSurveyItemLongPress(position, surveyAdapter);
                    }
                })
        );
    }

    private void initCurrentUserText() {
        hideUsersDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_expand_less);
        showUsersDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_expand_more);
        userHeader.setOnClickListener(v -> {
            if (surveysRecyclerView.getVisibility() == VISIBLE) {
                updateTextViewDrawable(hideUsersDrawable);
                surveyTitleTextView.setVisibility(GONE);
                surveysRecyclerView.setVisibility(GONE);
                usersRecyclerView.setVisibility(VISIBLE);
            } else {
                updateTextViewDrawable(showUsersDrawable);
                surveyTitleTextView.setVisibility(VISIBLE);
                surveysRecyclerView.setVisibility(VISIBLE);
                usersRecyclerView.setVisibility(GONE);

            }
        });
        userHeader.setOnLongClickListener(v -> {
            presenter.onCurrentUserLongPress();
            return true;
        });
    }

    private void updateTextViewDrawable(Drawable drawable) {
        currentUserTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
    }

    private void onSurveyItemLongPress(int position, SurveyAdapter adapter) {
        ViewSurvey viewSurvey = adapter.getItem(position);
        if (viewSurvey != null) {
            DialogFragment dialogFragment = SurveyDeleteConfirmationDialog
                    .newInstance(viewSurvey);
            dialogFragment.show(getSupportFragmentManager(), SurveyDeleteConfirmationDialog.TAG);
        }
    }

    private void onSurveyItemTap(int position) {
        presenter.onSurveyItemTap(surveyAdapter.getItem(position));
    }

    public void setDrawerNavigationListener(DrawerNavigationListener drawerNavigationListener) {
        this.drawerNavigationListener = drawerNavigationListener;
    }

    @Override
    public void displaySurveys(List<ViewSurvey> surveys, Long selectedSurveyId) {
        surveyAdapter.setSurveys(surveys, selectedSurveyId);
    }

    @Override
    public void notifySurveyDeleted(long surveyGroupId) {
        if (drawerNavigationListener != null) {
            drawerNavigationListener.onSurveyDeleted(surveyGroupId);
        }
    }

    @Override
    public void selectSurvey(SurveyGroup surveyGroup) {
        if (drawerNavigationListener != null) {
            drawerNavigationListener.onSurveySelected(surveyGroup);
            surveyAdapter.updateSelected(surveyGroup.getId());
        }
    }

    @Override
    public void displayUsers(String selectedUserName, List<ViewUser> viewUsers) {
        usersAdapter.setUsers(viewUsers);
        currentUserTextView.setText(selectedUserName);
    }

    @Override
    public void displayAddUser() {
        CreateUserDialog dialog = new CreateUserDialog();
        dialog.show(getSupportFragmentManager(), CreateUserDialog.TAG);
    }

    @Override
    public void displaySurveyError() {
        snackBarManager.displaySnackBar(this, R.string.surveys_error, getContext());
    }

    @Override
    public void displayUsersError() {
        snackBarManager.displaySnackBar(this, R.string.users_error, getContext());
    }

    @Override
    public void displayErrorDeleteSurvey() {
        snackBarManager.displaySnackBar(this, R.string.survey_delete_error, getContext());
    }

    @Override
    public void displayErrorSelectSurvey() {
        snackBarManager.displaySnackBar(this, R.string.survey_select_error, getContext());
    }

    @Override
    public void displayUserEditError() {
        snackBarManager.displaySnackBar(this, R.string.user_edit_error, getContext());
    }

    @Override
    public void displayUserDeleteError() {
        snackBarManager.displaySnackBar(this, R.string.user_delete_error, getContext());
    }

    @Override
    public void displayUserSelectError() {
        snackBarManager.displaySnackBar(this, R.string.user_select_error, getContext());
    }

    @Override
    public void displayEditUser(ViewUser currentUser) {
        DialogFragment fragment = EditUserDialog.newInstance(currentUser);
        fragment.show(getSupportFragmentManager(), EditUserDialog.TAG);
    }

    @Override
    public void onUserLongPress(ViewUser viewUser) {
        DialogFragment dialogFragment = UserOptionsDialog.newInstance(viewUser);
        dialogFragment.show(getSupportFragmentManager(), UserOptionsDialog.TAG);
    }

    private FragmentManager getSupportFragmentManager() {
        return ((AppCompatActivity) getContext()).getSupportFragmentManager();
    }

    public void onSurveyDeleteConfirmed(long surveyGroupId) {
        presenter.onDeleteSurvey(surveyGroupId);
    }

    @SuppressLint("RestrictedApi") @Override
    protected void onDetachedFromWindow() {
        presenter.destroy();
        super.onDetachedFromWindow();
    }

    public void editUser(ViewUser viewUser) {
        presenter.editUser(viewUser);
    }

    public void deleteUser(ViewUser viewUser) {
        presenter.deleteUser(viewUser);
    }

    public void createUser(String userName) {
        presenter.createUser(userName);
    }

    public interface DrawerNavigationListener {

        void onSurveySelected(SurveyGroup surveyGroup);

        void onSurveyDeleted(long surveyGroupId);

        void navigateToHelp();

        void navigateToAbout();

        void navigateToSettings();

        void navigateToOfflineMaps();
    }
}
