/*
 * Copyright (C) 2017,2019 Stichting Akvo (Akvo Foundation)
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

import androidx.core.util.Pair;

import org.akvo.flow.domain.entity.Survey;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.entity.User;
import org.akvo.flow.domain.interactor.users.CreateUser;
import org.akvo.flow.domain.interactor.DeleteSurvey;
import org.akvo.flow.domain.interactor.users.DeleteUser;
import org.akvo.flow.domain.interactor.users.EditUser;
import org.akvo.flow.domain.interactor.SaveSelectedSurvey;
import org.akvo.flow.domain.interactor.users.SelectUser;
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
    private final UseCase selectUser;
    private final UseCase createUser;

    private final SurveyMapper surveyMapper;
    private final UserMapper userMapper;
    private final SurveyGroupMapper surveyGroupMapper;

    private IFlowNavigationView view;
    private ViewUser currentUser;

    @Inject
    public FlowNavigationPresenter(@Named("getAllSurveys") UseCase getAllSurveys,
            SurveyMapper surveyMapper, @Named("deleteSurvey") UseCase deleteSurvey,
            SurveyGroupMapper surveyGroupMapper,
            @Named("saveSelectedSurvey") UseCase saveSelectedSurvey,
            @Named("getUsers") UseCase getUsers, UserMapper userMapper,
            @Named("editUser") UseCase editUser, @Named("deleteUser") UseCase deleteUser,
            @Named("selectUser") UseCase selectUser,
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
        this.selectUser = selectUser;
        this.createUser = createUser;
    }

    @Override
    public void destroy() {
        getAllSurveys.dispose();
        deleteSurvey.dispose();
        saveSelectedSurvey.dispose();
        getUsers.dispose();
        editUser.dispose();
        deleteUser.dispose();
        selectUser.dispose();
        createUser.dispose();
    }

    public void setView(IFlowNavigationView view) {
        this.view = view;
    }

    public void load() {
        loadSurveys();
        loadUsers();
    }

    private void loadSurveys() {
        getAllSurveys.execute(new DefaultObserver<Pair<List<Survey>, Long>>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Error getting all surveys");
                view.displaySurveyError();
            }

            @Override
            public void onNext(Pair<List<Survey>, Long> result) {
                view.displaySurveys(surveyMapper.transform(result.first), result.second);
            }
        }, null);
    }

    private void loadUsers() {
        getUsers.execute(new DefaultObserver<Pair<User, List<User>>>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Error getting users");
                view.displayUsersError();
            }

            @Override
            public void onNext(Pair<User, List<User>> userListPair) {
                currentUser = userMapper.transform(userListPair.first);
                String name = currentUser == null ? "" : currentUser.getName();
                view.displayUsers(name, userMapper.transform(userListPair.second));
            }
        }, null);
    }

    public void onDeleteSurvey(final long surveyGroupId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put(DeleteSurvey.SURVEY_ID_PARAM, surveyGroupId);
        deleteSurvey.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.displayErrorDeleteSurvey();
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
            Map<String, Object> params = new HashMap<>(2);
            params.put(SaveSelectedSurvey.KEY_SURVEY_ID, viewSurvey.getId());
            saveSelectedSurvey.execute(new DefaultObserver<Boolean>() {
                @Override
                public void onError(Throwable e) {
                    Timber.e(e);
                    view.displayErrorSelectSurvey();
                }

                @Override
                public void onNext(Boolean ignored) {
                    view.selectSurvey(surveyGroupMapper.transform(viewSurvey));
                }
            }, params);
        }
    }

    public void onCurrentUserLongPress() {
        if (currentUser != null) {
            view.displayEditUser(currentUser);
        }
    }

    public void editUser(ViewUser viewUser) {
        Map<String, Object> params = new HashMap<>(2);
        params.put(EditUser.PARAM_USER, userMapper.transform(viewUser));
        editUser.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.displayUserEditError();
            }
        }, params);
    }

    public void deleteUser(ViewUser viewUser) {
        Map<String, Object> params = new HashMap<>(2);
        params.put(DeleteUser.PARAM_USER, userMapper.transform(viewUser));
        deleteUser.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                view.displayUserDeleteError();
            }
        }, params);
    }

    public void onUserSelected(ViewUser item) {
        if (item.getId() == ViewUser.ADD_USER_ID) {
            view.displayAddUser();
        } else {
            Map<String, Object> params = new HashMap<>(2);
            params.put(SelectUser.PARAM_USER_ID, item.getId());
            selectUser.execute(new DefaultObserver<Boolean>() {
                @Override
                public void onError(Throwable e) {
                    Timber.e(e);
                    view.displayUserSelectError();
                }

                @Override
                public void onNext(Boolean aBoolean) {
                    load();
                }
            }, params);
        }
    }

    public void createUser(String userName) {
        Map<String, Object> params = new HashMap<>(2);
        params.put(CreateUser.PARAM_USER_NAME, userName);
        createUser.execute(new DefaultObserver<Boolean>() {
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
