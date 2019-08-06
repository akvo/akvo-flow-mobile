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

package org.akvo.flow.offlinemaps.presentation;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.akvo.flow.offlinemaps.R;

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

public class ToolBarBackActivity extends AppCompatActivity {

    protected void setupToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    protected void displaySnackBar(View view, @StringRes int resId) {
        Snackbar snackbar = Snackbar.make(view, resId, Snackbar.LENGTH_LONG);
        fixSnackBarText(this, snackbar);
        snackbar.show();
    }

    private void fixSnackBarText(Context context, Snackbar snackbar) {
        View snackBarView = snackbar.getView();
        int snackBarTextId = com.google.android.material.R.id.snackbar_text;
        TextView textView = snackBarView.findViewById(snackBarTextId);
        if (textView != null) {
            textView.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        }
    }
}
