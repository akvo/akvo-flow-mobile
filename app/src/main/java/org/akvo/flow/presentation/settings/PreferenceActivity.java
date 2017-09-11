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
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
        //TODO: extract all constants
        //TODO: retrieve values in preferences
        List<Preference> preferences = new ArrayList<>(17);
        preferences
                .add(new PreferenceSeparator(0, getString(R.string.preferences_settings_section)));
        preferences.add(new PreferenceSwitch(1, getString(R.string.preference_screen_on)));
        preferences.add(new PreferenceSwitch(2, getString(R.string.preference_mobile_data)));
        preferences.add(new PreferenceSpinner(3, getString(R.string.preference_app_language),
                R.array.app_languages,
                0));
        preferences.add(new PreferenceSpinner(4, getString(R.string.preference_image_size),
                R.array.max_image_size_pref, 0));
        preferences.add(new PreferenceSeparator(5, getString(R.string.preferences_data_section)));
        preferences.add(new PreferenceTitle(6, getString(R.string.preference_sync_datapoints)));
        preferences.add(new PreferenceTitleSubtitle(7,
                getString(R.string.preference_delete_collected_data),
                getString(R.string.reset_responses_desc)));
        preferences.add(new PreferenceTitleSubtitle(8,
                getString(R.string.preference_delete_everything),
                getString(R.string.resetalldesc)));
        preferences.add(new PreferenceTitleSubtitle(9,
                getString(R.string.preference_download_form_title),
                getString(R.string.preference_download_form_subtitle)));
        preferences.add(new PreferenceTitleSubtitle(10, getString(R.string.reloadsurveyslabel),
                getString(R.string.reloadsurveysdesc)));
        preferences.add(new PreferenceSeparator(11,
                getString(R.string.preferences_configuration_section)));
        preferences.add(new PreferenceTitle(12, getString(R.string.preference_gps)));
        preferences.add(new PreferenceTitle(13, getString(R.string.preference_storage)));
        preferences.add(new PreferenceSeparator(14,
                getString(R.string.preferences_section_information)));
        preferences.add(new PreferenceTitleSubtitle(15, getString(R.string.preference_identifier),
                ""));
        preferences
                .add(new PreferenceTitleSubtitle(16, getString(R.string.preference_instance_name),
                        BuildConfig.INSTANCE_URL));
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

    public class PreferenceSpinner extends Preference {

        @ArrayRes
        private final int items;
        private final int selectedItemPosition;

        protected PreferenceSpinner(int id, String title, @ArrayRes int items,
                int selectedItemPosition) {
            super(id, title);
            this.items = items;
            this.selectedItemPosition = selectedItemPosition;
        }

        public int getItems() {
            return items;
        }

        public int getSelectedItemPosition() {
            return selectedItemPosition;
        }
    }

    public class PreferenceAdapter extends RecyclerView.Adapter<PreferenceViewHolder> {

        private static final int VIEW_TYPE_SEPARATOR = 0;
        private static final int VIEW_TYPE_TITLE = 1;
        private static final int VIEW_TYPE_TITLE_AND_SUBTITLE = 2;
        private static final int VIEW_TYPE_SWITCH = 3;
        private static final int VIEW_TYPE_SPINNER = 4;

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
                case VIEW_TYPE_SPINNER:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.preference_spinner, parent, false);
                    return new SpinnerPreferenceViewHolder(view);
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
            } else if (preference instanceof PreferenceSpinner) {
                return VIEW_TYPE_SPINNER;
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

        @BindView(R.id.separator)
        View separator;

        public PreferenceViewHolder(View itemView) {
            super(itemView);
        }

        public abstract void updateViews(T preference, int position);

        protected void updateDivider(int position) {
            //TODO: use ids!!!
            if (position == 4 || position == 13 || position == 10 || position == 16) {
                separator.setVisibility(View.GONE);
            } else {
                separator.setVisibility(View.VISIBLE);
            }
        }
    }

    public class SeparatorPreferenceViewHolder extends PreferenceViewHolder<PreferenceSeparator> {

        @BindView(R.id.title)
        TextView title;

        public SeparatorPreferenceViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        public void updateViews(PreferenceSeparator preference, int position) {
            updateDivider(position);
            title.setText(preference.getTitle());
        }
    }

    public class TitlePreferenceViewHolder extends PreferenceViewHolder<PreferenceTitle> {

        @BindView(R.id.title)
        TextView title;

        public TitlePreferenceViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        public void updateViews(PreferenceTitle preference, int position) {
            updateDivider(position);
            title.setText(preference.getTitle());
        }

    }

    public class SwitchPreferenceViewHolder extends PreferenceViewHolder<PreferenceSwitch> {

        @BindView(R.id.switch_compat)
        SwitchCompat switchCompat;

        public SwitchPreferenceViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        public void updateViews(PreferenceSwitch preference, int position) {
            updateDivider(position);
            switchCompat.setText(preference.getTitle());
        }
    }

    public class SpinnerPreferenceViewHolder
            extends PreferenceViewHolder<PreferenceSpinner> {

        @BindView(R.id.title)
        TextView title;

        @BindView(R.id.preference_spinner)
        Spinner preferenceS;

        public SpinnerPreferenceViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        public void updateViews(PreferenceSpinner preference, int position) {
            updateDivider(position);
            title.setText(preference.getTitle());
            ArrayAdapter<CharSequence> adapter = ArrayAdapter
                    .createFromResource(preferenceS.getContext(),
                            preference.getItems(), android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            preferenceS.setAdapter(adapter);
            preferenceS.setSelection(preference.getSelectedItemPosition());
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
            updateDivider(position);
            title.setText(preference.getTitle());
            subtitle.setText(preference.getSubtitle());
        }
    }
}
