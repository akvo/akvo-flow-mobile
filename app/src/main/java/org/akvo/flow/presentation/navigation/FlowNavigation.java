/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.ui.Navigator;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;

public class FlowNavigation extends NavigationView implements FlowNavigationView {

    private TextView currentUserTv;
    private TextView surveyTitleTv;
    private RecyclerView surveysRv;
    private RecyclerView usersRv;
    private DrawerNavigationListener surveyListener;
    private SurveyAdapter adapter;

    @Inject
    FlowNavigationPresenter presenter;

    @Inject
    Navigator navigator;

    public FlowNavigation(Context context) {
        this(context, null);
    }

    public FlowNavigation(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowNavigation(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        initialiseInjector();
        initViews();
        initCurrentUserText();
        initUserList();
        initSurveyList();
        setNavigationItemListener();
        presenter.setView(this);
        presenter.load();
    }

    private void initViews() {
        View headerView = getHeaderView(0);
        currentUserTv = ButterKnife.findById(headerView, R.id.current_user_name);
        surveyTitleTv = ButterKnife.findById(headerView, R.id.surveys_title_tv);
        surveysRv = ButterKnife.findById(headerView, R.id.surveys_rv);
        usersRv = ButterKnife.findById(headerView, R.id.users_rv);
    }

    private void initialiseInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    protected ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getContext().getApplicationContext()).getApplicationComponent();
    }

    private void initUserList() {
       //TODO
    }

    private void setNavigationItemListener() {
        final Context context = getContext();
        setNavigationItemSelectedListener(new OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.settings:
                        navigator.navigateToAppSettings(context);
                        return true;
                    case R.id.about:
                        navigator.navigateToAbout(context);
                        return true;
                    case R.id.help:
                        navigator.navigateToHelp(context);
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void initSurveyList() {
        final Context context = getContext();
        surveysRv.setLayoutManager(new LinearLayoutManager(context));
        adapter = new SurveyAdapter(context);
        surveysRv.setAdapter(adapter);
        surveysRv.addOnItemTouchListener(new RecyclerItemClickListener(context,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View childView, int position) {
                        onSurveyItemTap(position);
                    }

                    @Override
                    public void onItemLongPress(View childView, int position) {
                        onSurveyItemLongPress(position, adapter);
                    }
                })
        );
    }

    private void initCurrentUserText() {
        currentUserTv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (surveysRv.getVisibility() == VISIBLE) {
                    surveyTitleTv.setVisibility(GONE);
                    surveysRv.setVisibility(GONE);
                    usersRv.setVisibility(VISIBLE);
                } else {
                    surveyTitleTv.setVisibility(VISIBLE);
                    surveysRv.setVisibility(VISIBLE);
                    usersRv.setVisibility(GONE);
                }
            }
        });
    }

    private void onSurveyItemLongPress(int position, SurveyAdapter adapter) {
        ViewSurvey viewSurvey = adapter.getItem(position);
        if (viewSurvey != null) {
            final long surveyGroupId = viewSurvey.getId();
            DialogFragment dialogFragment = SurveyDeleteConfirmationDialog
                    .newInstance(surveyGroupId);
            dialogFragment.show(((AppCompatActivity) getContext()).getSupportFragmentManager(),
                    SurveyDeleteConfirmationDialog.TAG);
        }
    }

    private void onSurveyItemTap(int position) {
        presenter.onSurveyItemTap(adapter.getItem(position));
    }

    public void setSurveyListener(DrawerNavigationListener surveyListener) {
        this.surveyListener = surveyListener;
    }

    @Override
    public void display(List<ViewSurvey> surveys, Long selectedSurveyId) {
        adapter.setSurveys(surveys, selectedSurveyId);
    }

    @Override
    public void notifySurveyDeleted(long surveyGroupId) {
        if (surveyListener != null) {
            surveyListener.onSurveyDeleted(surveyGroupId);
        }
    }

    @Override
    public void onSurveySelected(SurveyGroup surveyGroup) {
        if (surveyListener != null) {
            surveyListener.onSurveySelected(surveyGroup);
            adapter.updateSelected(surveyGroup.getId());
        }
    }

    public void onSurveyDeleteConfirmed(long surveyGroupId) {
        presenter.onDeleteSurvey(surveyGroupId);
    }

    @Override
    protected void onDetachedFromWindow() {
        presenter.destroy();
        super.onDetachedFromWindow();
    }


    public interface DrawerNavigationListener {

        void onSurveySelected(SurveyGroup surveyGroup);

        void onSurveyDeleted(long surveyGroupId);
    }
}
