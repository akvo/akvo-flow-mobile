/*
 * Copyright (C) 2016-2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.injector.component;

import org.akvo.flow.activity.AddUserActivity;
import org.akvo.flow.activity.AppUpdateActivity;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.SurveyActivity;
import org.akvo.flow.activity.TransmissionHistoryActivity;
import org.akvo.flow.injector.PerActivity;
import org.akvo.flow.injector.module.ViewModule;
import org.akvo.flow.injector.module.WalkThroughModule;
import org.akvo.flow.presentation.AppDownloadDialogFragment;
import org.akvo.flow.presentation.about.AboutActivity;
import org.akvo.flow.presentation.datapoints.list.DataPointsListFragment;
import org.akvo.flow.presentation.datapoints.map.DataPointsMapFragment;
import org.akvo.flow.presentation.datapoints.map.one.DataPointMapActivity;
import org.akvo.flow.presentation.form.mobiledata.MobileDataSettingDialog;
import org.akvo.flow.presentation.geoshape.ViewGeoShapeActivity;
import org.akvo.flow.presentation.geoshape.create.CreateGeoShapeActivity;
import org.akvo.flow.presentation.geoshape.properties.PropertiesDialog;
import org.akvo.flow.presentation.help.HelpActivity;
import org.akvo.flow.presentation.legal.LegalNoticesActivity;
import org.akvo.flow.presentation.main.MainActivity;
import org.akvo.flow.presentation.navigation.FlowNavigationView;
import org.akvo.flow.presentation.record.RecordActivity;
import org.akvo.flow.presentation.settings.PreferenceActivity;
import org.akvo.flow.presentation.settings.publish.PublishFilesPreferenceView;
import org.akvo.flow.presentation.signature.SignatureActivity;
import org.akvo.flow.ui.fragment.ResponseListFragment;
import org.akvo.flow.ui.view.CaddisflyQuestionView;
import org.akvo.flow.ui.view.cascade.CascadeQuestionView;
import org.akvo.flow.ui.view.GeoshapeQuestionView;
import org.akvo.flow.ui.view.geolocation.GeoQuestionView;
import org.akvo.flow.ui.view.media.photo.PhotoQuestionView;
import org.akvo.flow.ui.view.media.video.VideoQuestionView;
import org.akvo.flow.ui.view.signature.SignatureQuestionView;

import dagger.Component;

@PerActivity
@Component(dependencies = ApplicationComponent.class, modules = {ViewModule.class,
        WalkThroughModule.class
})
public interface ViewComponent {

    void inject(FormActivity formActivity);

    void inject(DataPointsMapFragment dataPointsMapFragment);

    void inject(DataPointsListFragment dataPointsListFragment);

    void inject(RecordActivity recordActivity);

    void inject(SignatureActivity signatureActivity);

    void inject(LegalNoticesActivity legalNoticesActivity);

    void inject(AboutActivity aboutActivity);

    void inject(ResponseListFragment responseListFragment);

    void inject(HelpActivity activity);

    void inject(PreferenceActivity preferenceActivity);

    void inject(FlowNavigationView navigation);

    void inject(AppDownloadDialogFragment fragment);

    void inject(PhotoQuestionView photoQuestionViewImpl);

    void inject(VideoQuestionView videoQuestionView);

    void inject(AppUpdateActivity appUpdateActivity);

    void inject(CascadeQuestionView cascadeQuestionView);

    void inject(SignatureQuestionView signatureQuestionView);

    void inject(CaddisflyQuestionView caddisflyQuestionView);

    void inject(GeoQuestionView geoQuestionView);

    void inject(GeoshapeQuestionView geoshapeQuestionView);

    void inject(AddUserActivity addUserActivity);

    void inject(SurveyActivity surveyActivity);

    void inject(TransmissionHistoryActivity transmissionHistoryActivity);

    void inject(MainActivity mainActivity);

    void inject(PublishFilesPreferenceView publishFilesPreferenceView);

    void inject(MobileDataSettingDialog mobileDataSettingDialog);

    void inject(DataPointMapActivity dataPointMapActivity);

    void inject(CreateGeoShapeActivity createGeoShapeActivity);

    void inject(ViewGeoShapeActivity viewGeoShapeActivity);

    void inject(PropertiesDialog propertiesDialog);
}
