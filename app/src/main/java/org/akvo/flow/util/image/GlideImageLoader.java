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

package org.akvo.flow.util.image;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;

import java.io.File;

public class GlideImageLoader implements ImageLoader {

    private final RequestManager requestManager;

    public GlideImageLoader(Context context) {
        requestManager = Glide.with(context);
    }

    public GlideImageLoader(Activity activity) {
        requestManager = Glide.with(activity);
    }

    public GlideImageLoader(Fragment fragment) {
        requestManager = Glide.with(fragment);
    }

    @Override
    public void loadFromFile(File file, final ImageLoaderListener listener) {
        requestManager.load(file)
                .asBitmap()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap,
                            GlideAnimation<? super Bitmap> glideAnimation) {
                        listener.onImageReady(bitmap);
                    }
                });
    }

    @Override
    public void loadFromFile(File file, ImageView imageView) {
        requestManager.load(file)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(imageView);
    }

    @Override
    public void loadFromBase64String(String image, ImageView imageView,
            final ImageLoaderListener listener) {
        requestManager
                .load(Base64.decode(image, Base64.DEFAULT))
                .listener(new RequestListener<byte[], GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, byte[] model,
                            Target<GlideDrawable> target,
                            boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, byte[] model,
                            Target<GlideDrawable> target, boolean isFromMemoryCache,
                            boolean isFirstResource) {
                        listener.onImageReady(null);
                        return false;
                    }
                })
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(imageView);

    }
}
