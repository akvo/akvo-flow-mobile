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

import android.support.v4.util.Pair;

import org.akvo.flow.domain.entity.Survey;
import org.akvo.flow.domain.entity.User;
import org.akvo.flow.domain.interactor.CreateUser;
import org.akvo.flow.domain.interactor.DefaultSubscriber;
import org.akvo.flow.domain.interactor.DeleteSurvey;
import org.akvo.flow.domain.interactor.DeleteUser;
import org.akvo.flow.domain.interactor.EditUser;
import org.akvo.flow.domain.interactor.SaveSelectedSurvey;
import org.akvo.flow.domain.interactor.SetSelectedUser;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.Presenter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

public class FlowNavigationPresenter implements Presenter {

    private final UseCase getAllSurveys;
    private final UseCase deleteSurvey;
    private final UseCase saveSelectedSurvey;
    private final UseCase getUsers;
    private final UseCase editUser;
    private final UseCase deleteUser;
    private final UseCase setSelectedUser;
    private final UseCase createUser;

    private final SurveyMapper surveyMapper;
    private final UserMapper userMapper;
    private final SurveyGroupMapper surveyGroupMapper;

    private FlowNavigationView view;
    private ViewUser currentUser;

    @Inject
    public FlowNavigationPresenter(@Named("getAllSurveys") UseCase getAllSurveys,
            SurveyMapper surveyMapper, @Named("deleteSurvey") UseCase deleteSurvey,
            SurveyGroupMapper surveyGroupMapper,
            @Named("saveSelectedSurvey") UseCase saveSelectedSurvey,
            @Named("getUsers") UseCase getUsers, UserMapper userMapper,
            @Named("editUser") UseCase editUser, @Named("deleteUser") UseCase deleteUser,
            @Named("setSelectedUser") UseCase setSelectedUser,
            @Named("createUser") UseCase createUser) {
        this.getAllSurveys = getAllSurveys;
        this.surveyMapper = surveyMapper;
        this.deleteSurvey = deleteSurvey;
        this.surveyGroupMapper = surveyGroupMapper;
        this.saveSelectedSurvey = saveSelectedSurvey;
        this.getUsers = getUsers;
        this.userMapper = userMapper;
        this.editUser = editUser;
        this.deleteUser = deleteUser;
        this.setSelectedUser = setSelectedUser;
        this.createUser = createUser;
    }

    @Override
    public void destroy() {
        getAllSurveys.unSubscribe();
        deleteSurvey.unSubscribe();
        saveSelectedSurvey.unSubscribe();
        getUsers.unSubscribe();
        editUser.unSubscribe();
        deleteUser.unSubscribe();
        setSelectedUser.unSubscribe();
        createUser.unSubscribe();
    }

    public void setView(FlowNavigationView view) {
        this.view = view;
    }

    public void load() {
        loadSurveys();
        loadUsers();
    }

    private void loadSurveys() {
        getAllSurveys.execute(new DefaultSubscriber<Pair<List<Survey>, Long>>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Error getting all surveys");
                //what error to display here and how?
            }

            @Override
            public void onNext(Pair<List<Survey>, Long> result) {
                int size = result.first == null ? 0 : result.first.size();
                Timber.d("found new surveys: " + size);
                view.displaySurveys(surveyMapper.transform(result.first), result.second);
            }
        }, null);
    }

    private void loadUsers() {
        getUsers.execute(new DefaultSubscriber<Pair<User, List<User>>>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Error getting users");
            }

            @Override
            public void onNext(Pair<User, List<User>> userListPair) {
                currentUser = userMapper.transform(userListPair.first);
                String name = currentUser == null ? "" : currentUser.getName();
                view.displayUser(name, userMapper.transform(userListPair.second));
            }
        }, null);
    }

    public void onDeleteSurvey(final long surveyGroupId) {
        Map<String, Long> params = new HashMap<>(2);
        params.put(DeleteSurvey.SURVEY_ID_PARAM, surveyGroupId);
        deleteSurvey.execute(new DefaultSubscriber<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                //TODO: notify currentUser
                load();
            }

            @Override
            public void onNext(Boolean aBoolean) {
                view.notifySurveyDeleted(surveyGroupId);
                load();
            }
        }, params);
    }

    public void onSurveyItemTap(final ViewSurvey viewSurvey) {
        if (viewSurvey != null) {
            Map<String, Long> params = new HashMap<>(2);
            params.put(SaveSelectedSurvey.KEY_SURVEY_GROUP_ID, viewSurvey.getId());
            saveSelectedSurvey.execute(new DefaultSubscriber<Boolean>() {
                @Override
                public void onError(Throwable e) {
                    Timber.e(e);
                    //TODO: error
                }

                @Override
                public void onNext(Boolean aBoolean) {
                    view.onSurveySelected(surveyGroupMapper.transform(viewSurvey));
                }
            }, params);
        }
    }

    public void onCurrentUserLongPress() {
        if (currentUser != null) {
            view.onUserLongPress(currentUser);
        }
    }

    public void editUser(ViewUser viewUser) {
        Map<String, User> params = new HashMap<>(2);
        params.put(EditUser.PARAM_USER, userMapper.transform(viewUser));
        editUser.execute(new DefaultSubscriber<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }
        }, params);
    }

    public void deleteUser(ViewUser viewUser) {
        Map<String, User> params = new HashMap<>(2);
        params.put(DeleteUser.PARAM_USER, userMapper.transform(viewUser));
        deleteUser.execute(new DefaultSubscriber<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }
        }, params);
    }

    public void onUserSelected(ViewUser item) {
        if (item.getId() == ViewUser.ADD_USER_ID) {
            view.displayAddUser();
        } else {
            Map<String, Long> params = new HashMap<>(2);
            params.put(SetSelectedUser.PARAM_USER_ID, item.getId());
            setSelectedUser.execute(new DefaultSubscriber<Boolean>() {
                @Override
                public void onError(Throwable e) {
                    Timber.e(e);
                }

                @Override
                public void onNext(Boolean aBoolean) {
                    load();
                }
            }, params);
        }
    }

    public void createUser(String userName) {
        Map<String, String> params = new HashMap<>(2);
        params.put(CreateUser.PARAM_USER_NAME, userName);
        createUser.execute(new DefaultSubscriber<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }

            @Override
            public void onNext(Boolean ignored) {
                loadUsers();
            }
        }, params);
    }

    public void onUserLongPress(ViewUser item) {
        if (item.getId() == ViewUser.ADD_USER_ID) {
            view.displayAddUser();
        } else {
            view.onUserLongPress(item);
        }
    }
}
