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

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import org.akvo.flow.R;
import org.akvo.flow.uicomponents.BackActivity;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.image.GlideImageLoader;
import org.akvo.flow.util.image.ImageLoader;

import java.io.File;

import androidx.appcompat.app.ActionBar;
import butterknife.BindView;
import butterknife.ButterKnife;

public class FullImageActivity extends BackActivity {

    @BindView(R.id.imageView)
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);
        setupToolBar();
        ButterKnife.bind(this);
        setUpTitleAndSubtitle();
        loadImage();
    }

    private void setUpTitleAndSubtitle() {
        Intent intent = getIntent();
        String title = intent.getStringExtra(ConstantUtil.FORM_TITLE_EXTRA);
        if (title == null) {
            title = "";
        }
        String subTitle = intent.getStringExtra(ConstantUtil.FORM_SUBTITLE_EXTRA);
        if (subTitle == null) {
            subTitle = "";
        }
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setTitle(title);
            supportActionBar.setSubtitle(subTitle);
        }
    }

    private void loadImage() {
        Intent intent = getIntent();
        String imageFileName = intent.getStringExtra(ConstantUtil.IMAGE_URL_EXTRA);
        ImageLoader imageLoader = new GlideImageLoader(this);
        imageLoader.loadFromFile(new File(imageFileName), imageView);
    }
}
