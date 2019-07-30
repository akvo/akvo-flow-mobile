/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.uicomponents;

import android.app.AlertDialog;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class PositiveButtonHandler {

    @NonNull
    private final DialogFragment dialogFragment;

    public PositiveButtonHandler(@NonNull DialogFragment dialogFragment) {
        this.dialogFragment = dialogFragment;
    }

    public void disablePositiveButton() {
        Button button = getPositiveButton();
        button.setEnabled(false);
    }

    public void enablePositiveButton() {
        Button button = getPositiveButton();
        button.setEnabled(true);
    }

    private Button getPositiveButton() {
        AlertDialog dialog = (AlertDialog) dialogFragment.getDialog();
        return dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    }
}
