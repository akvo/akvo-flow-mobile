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

package org.akvo.flow.presentation;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.akvo.flow.R;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

public class SnackBarManager {

    @Inject
    public SnackBarManager() {
    }

    public void displaySnackBarWithAction(@Nullable View rootView, @StringRes int message,
            @StringRes int actionText, View.OnClickListener actionListener, Context context) {
        if (rootView != null) {
            Snackbar snackbar = Snackbar
                    .make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAction(actionText, actionListener);
            fixSnackBarText(context, snackbar);
            snackbar.show();
        }
    }

    private void fixSnackBarText(Context context, Snackbar snackbar) {
        View snackBarView = snackbar.getView();
        int snackBarTextId = com.google.android.material.R.id.snackbar_text;
        TextView textView = snackBarView.findViewById(snackBarTextId);
        if (textView != null) {
            textView.setTextColor(ContextCompat.getColor(context, R.color.white));
        }
    }

    public void displaySnackBar(@Nullable View rootView, @StringRes int message, Context context) {
       displaySnackBar(rootView, context.getResources().getString(message), context);
    }

    public void displaySnackBar(@Nullable View rootView, String message, Context context) {
        if (rootView != null) {
            Snackbar snackbar = Snackbar
                    .make(rootView, message, Snackbar.LENGTH_LONG);
            fixSnackBarText(context, snackbar);
            snackbar.show();
        }
    }
}