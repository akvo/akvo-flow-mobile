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

package org.akvo.flow.util.image;

import android.widget.ImageView;

import org.akvo.flow.domain.util.ImageSize;

import java.io.File;

import androidx.annotation.NonNull;

public interface ImageLoader {

    void loadFromFile(File file, ImageView imageView);

    void loadFromFile(File file, ImageLoaderListener listener);

    void loadFromBase64String(String image, ImageView imageView, ImageLoaderListener listener);

    void loadFromFile(ImageView imageView, File file, ImageLoaderListener listener,
            @NonNull ImageSize size);
}
