/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.settings.passcode;

import android.content.Context;
import android.support.v4.app.FragmentActivity;

public class PassCodeDeleteCollectedDialog extends PassCodeDialog {

    public static final String TAG = "DeleteCollected";

    private PassCodeDeleteCollectedListener listener;

    public PassCodeDeleteCollectedDialog() {
    }

    public static PassCodeDeleteCollectedDialog newInstance() {
        return new PassCodeDeleteCollectedDialog();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if (activity instanceof PassCodeDeleteCollectedListener) {
            listener = (PassCodeDeleteCollectedListener) activity;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implement PassCodeDeleteCollectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    void onPassCodeCorrect() {
        if (listener != null) {
            listener.deleteCollectedData();
        }
    }

    public interface PassCodeDeleteCollectedListener {

        void deleteCollectedData();
    }
}
