/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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

import org.jetbrains.annotations.NotNull;

public interface PreferenceView {

    void showLoading();

    void hideLoading();

    void displaySettings(ViewUserSettings viewUserSettings);

    void showDeleteCollectedData();

    void showDeleteCollectedDataWithPending();

    void showDeleteAllData();

    void showDeleteAllDataWithPending();

    void showClearDataError();

    void showClearDataSuccess();

    void dismiss();

    void showDownloadFormsError(int numberOfForms);

    void showDownloadFormsSuccess(int numberOfForms);

    void showConversationNotFound(int resId);

    void showErrorSending();

    void showInformationSent();

    void showSendInfoDialog(@NotNull String userName);
}
