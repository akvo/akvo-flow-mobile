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

package org.akvo.flow.util.image;

import android.app.Activity;
import android.content.Context;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.akvo.flow.BuildConfig;

import java.io.File;

import timber.log.Timber;

public class PicassoImageLoader implements ImageLoader<PicassoImageTarget> {

    private final Picasso requestManager;

    public PicassoImageLoader(Context context) {
        requestManager = getPicassoBuilder(context);
    }

    public PicassoImageLoader(Activity activity) {
        requestManager = getPicassoBuilder(activity);
    }

    private Picasso getPicassoBuilder(Context context) {
        return new Picasso.Builder(context)
                .loggingEnabled(BuildConfig.DEBUG)
                .addRequestHandler(new VideoRequestHandler())
                .addRequestHandler(new Base64RequestHandler())
                .build();
    }

    @Override
    public void loadFromFile(File file, PicassoImageTarget target) {
        requestManager.load(file).into(target);
    }

    @Override
    public void loadFromFile(File file, ImageView imageView) {
        requestManager.load(file).into(imageView);
    }

    @Override
    public void loadVideoThumbnail(final String filepath, ImageView imageView,
            final ImageLoaderListener listener) {
        requestManager.load(VideoRequestHandler.SCHEME_VIDEO + ":" + filepath).into(imageView,
                new Callback() {
                    @Override
                    public void onSuccess() {
                        listener.onImageReady();
                    }

                    @Override
                    public void onError() {
                        listener.onImageError();
                        Timber.e("Error getting video: " + filepath);
                    }
                });
    }

    @Override
    public void loadFromBase64String(String image, ImageView imageView,
            final ImageLoaderListener listener) {
        requestManager.load(Base64RequestHandler.SCHEME_BASE64 + ":" + image).into(imageView,
                new Callback() {
                    @Override
                    public void onSuccess() {
                        listener.onImageReady();
                    }

                    @Override
                    public void onError() {
                        //Empty
                    }
                });

    }

    @Override
    public void clearImage(File imageFile) {
        requestManager.invalidate(imageFile);
    }
}
