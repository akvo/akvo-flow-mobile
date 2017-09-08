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

package org.akvo.flow.presentation.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.ui.Navigator;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PreferenceActivity extends BackActivity {

    @Inject
    Navigator navigator;

    @BindView(R.id.preferences_rv)
    RecyclerView preferencesRv;

    @BindView(R.id.appbar_layout)
    AppBarLayout appBarLayout;

    @BindView(R.id.toolbar_shadow)
    View toolbarShadow;

    private String[] maxImgSizes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);
        ButterKnife.bind(this);
        initializeInjector();
        setupToolBar();
        setUpToolBarAnimationListener();
        maxImgSizes = getResources().getStringArray(R.array.max_image_size_pref);
        setUpPreferences();
    }

    private void setUpToolBarAnimationListener() {
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                    onToolBarCollapsed();
                } else if (verticalOffset == 0) {
                    onToolbarExpanded();
                } else {
                    onToolbarMove();
                }
            }

            private void onToolbarMove() {
                toolbarShadow.setVisibility(View.GONE);
            }

            private void onToolbarExpanded() {
                toolbarShadow.setVisibility(View.VISIBLE);
            }

            private void onToolBarCollapsed() {
                toolbarShadow.setVisibility(View.GONE);
            }
        });
    }

    private void initializeInjector() {
        ViewComponent viewComponent = DaggerViewComponent.builder()
                .applicationComponent(getApplicationComponent()).build();
        viewComponent.inject(this);
    }

    private void setUpPreferences() {
        preferencesRv.setLayoutManager(new LinearLayoutManager(this));
        //TODO: extract all constants and string
        List<Preference> preferences = new ArrayList<>(17);
        preferences.add(new PreferenceSeparator(0, "Settings"));
        preferences.add(new PreferenceSwitch(1, "Keep screen on during form input"));
        preferences.add(new PreferenceSwitch(2, "Enable usage of mobile data"));
        preferences.add(new PreferenceTitleSubtitle(3, "App language", "English"));
        preferences.add(new PreferenceTitleSubtitle(4, "Image size", maxImgSizes[0]));
        preferences.add(new PreferenceSeparator(5, "Configuration"));
        preferences.add(new PreferenceTitle(6, "GPS fixes"));
        preferences.add(new PreferenceTitle(7, "Available storage"));
        preferences.add(new PreferenceSeparator(8, "Information"));
        preferences.add(new PreferenceTitleSubtitle(9, "Device identifier", "valeria"));
        preferences.add(new PreferenceTitleSubtitle(10, "Instance name", BuildConfig.INSTANCE_URL));
        preferences.add(new PreferenceSeparator(11, "Data"));
        preferences.add(new PreferenceTitle(12, "Send submitted data points to Flow instance"));
        preferences.add(new PreferenceTitleSubtitle(13, "Delete collected data and images",
                getString(R.string.reset_responses_desc)));
        preferences.add(new PreferenceTitleSubtitle(14, "Delete everything",
                getString(R.string.resetalldesc)));
        preferences.add(new PreferenceTitleSubtitle(15, getString(R.string.downloadsurveylabel),
                getString(R.string.downloadsurveydesc)));
        preferences.add(new PreferenceTitleSubtitle(16, getString(R.string.reloadsurveyslabel),
                getString(R.string.reloadsurveysdesc)));
        preferencesRv.setAdapter(new PreferenceAdapter(preferences));
    }

    public abstract class Preference {

        private final int id;
        private final String title;

        protected Preference(int id, String title) {
            this.id = id;
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    public class PreferenceSeparator extends Preference {

        protected PreferenceSeparator(int id, String title) {
            super(id, title);
        }
    }

    public class PreferenceTitle extends Preference {

        protected PreferenceTitle(int id, String title) {
            super(id, title);
        }
    }

    public class PreferenceSwitch extends Preference {

        protected PreferenceSwitch(int id, String title) {
            super(id, title);
        }
    }

    public class PreferenceTitleSubtitle extends Preference {

        private final String subtitle;

        protected PreferenceTitleSubtitle(int id, String title, String subtitle) {
            super(id, title);
            this.subtitle = subtitle;
        }

        public String getSubtitle() {
            return subtitle;
        }
    }

    public class PreferenceAdapter extends RecyclerView.Adapter<PreferenceViewHolder> {

        private static final int VIEW_TYPE_SEPARATOR = 0;
        private static final int VIEW_TYPE_TITLE = 1;
        private static final int VIEW_TYPE_TITLE_AND_SUBTITLE = 2;
        private static final int VIEW_TYPE_SWITCH = 3;

        @NonNull
        private final List<Preference> preferences;

        public PreferenceAdapter(@NonNull List<Preference> preferences) {
            this.preferences = preferences;
        }

        @Override
        public PreferenceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case VIEW_TYPE_SEPARATOR:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.preference_separator, parent, false);
                    return new SeparatorPreferenceViewHolder(view);
                case VIEW_TYPE_TITLE:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.preference_title, parent, false);
                    return new TitlePreferenceViewHolder(view);
                case VIEW_TYPE_TITLE_AND_SUBTITLE:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.preference_title_subtitle, parent, false);
                    return new TitleSubtitlePreferenceViewHolder(view);
                case VIEW_TYPE_SWITCH:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.preference_switch, parent, false);
                    return new SwitchPreferenceViewHolder(view);
                default:
                    throw new IllegalArgumentException("Wrong view type");
            }
        }

        @Override
        public int getItemViewType(int position) {
            Preference preference = preferences.get(position);
            if (preference instanceof PreferenceSeparator) {
                return VIEW_TYPE_SEPARATOR;
            } else if (preference instanceof PreferenceTitle) {
                return VIEW_TYPE_TITLE;
            } else if (preference instanceof PreferenceTitleSubtitle) {
                return VIEW_TYPE_TITLE_AND_SUBTITLE;
            } else if (preference instanceof PreferenceSwitch) {
                return VIEW_TYPE_SWITCH;
            } else {
                throw new IllegalArgumentException(
                        "invalid data found: " + preference.getClass().getSimpleName());
            }
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder holder, int position) {
            Preference preference = preferences.get(position);
            holder.updateViews(preference, position);
        }

        @Override
        public int getItemCount() {
            return preferences.size();
        }
    }

    public abstract class PreferenceViewHolder<T extends Preference>
            extends RecyclerView.ViewHolder {

        public PreferenceViewHolder(View itemView) {
            super(itemView);
        }

        public abstract void updateViews(T preference, int position);
    }

    public class SeparatorPreferenceViewHolder extends PreferenceViewHolder<PreferenceSeparator> {

        @BindView(R.id.title)
        TextView title;

        @BindView(R.id.separator)
        View separator;

        public SeparatorPreferenceViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        public void updateViews(PreferenceSeparator preference, int position) {
            if (position == 0) {
                separator.setVisibility(View.GONE);
            } else {
                separator.setVisibility(View.VISIBLE);
            }
            title.setText(preference.getTitle());
        }
    }

    public class TitlePreferenceViewHolder extends PreferenceViewHolder<PreferenceTitle> {

        @BindView(R.id.title)
        TextView title;

        @BindView(R.id.separator)
        View separator;

        public TitlePreferenceViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        public void updateViews(PreferenceTitle preference, int position) {
            title.setText(preference.getTitle());
            //TODO: use ids!!!
            if (position == 4 || position == 7 || position == 10) {
                separator.setVisibility(View.GONE);
            } else {
                separator.setVisibility(View.VISIBLE);
            }
        }
    }

    public class SwitchPreferenceViewHolder extends PreferenceViewHolder<PreferenceSwitch> {

        @BindView(R.id.switch_compat)
        SwitchCompat switchCompat;

        @BindView(R.id.separator)
        View separator;

        public SwitchPreferenceViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        public void updateViews(PreferenceSwitch preference, int position) {
            switchCompat.setText(preference.getTitle());
            //TODO: use ids!!!
            if (position == 4 || position == 7 || position == 10) {
                separator.setVisibility(View.GONE);
            } else {
                separator.setVisibility(View.VISIBLE);
            }
        }
    }

    public class TitleSubtitlePreferenceViewHolder
            extends PreferenceViewHolder<PreferenceTitleSubtitle> {

        @BindView(R.id.title)
        TextView title;

        @BindView(R.id.subtitle)
        TextView subtitle;

        @BindView(R.id.separator)
        View separator;

        public TitleSubtitlePreferenceViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        public void updateViews(PreferenceTitleSubtitle preference, int position) {
            title.setText(preference.getTitle());
            subtitle.setText(preference.getSubtitle());
            //TODO: use ids!!!
            if (position == 4 || position == 7 || position == 10) {
                separator.setVisibility(View.GONE);
            } else {
                separator.setVisibility(View.VISIBLE);
            }
        }
    }
}
