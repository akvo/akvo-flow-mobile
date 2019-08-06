/*
 * Copyright (C) 2018-2019 Stichting Akvo (Akvo Foundation)
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
import androidx.fragment.app.FragmentActivity;

public class PassCodeDeleteAllDialog extends PassCodeDialog {

    public static final String TAG = "DeleteAll";

    private PassCodeDeleteAllListener listener;

    public PassCodeDeleteAllDialog() {
    }

    public static PassCodeDeleteAllDialog newInstance() {
        return new PassCodeDeleteAllDialog();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if (activity instanceof PassCodeDeleteAllListener) {
            listener = (PassCodeDeleteAllListener) activity;
        } else {
            throw new IllegalArgumentException("Activity must implement PassCodeDeleteAllListener");
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
            listener.deleteAllData();
        }
    }

    public interface PassCodeDeleteAllListener {

        void deleteAllData();
    }
}
