/*
 * Copyright (C) 2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.form.view.groups

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import io.reactivex.observers.DisposableCompletableObserver
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.domain.interactor.datapoints.DownloadMedia
import org.akvo.flow.injector.component.ApplicationComponent
import org.akvo.flow.injector.component.DaggerViewComponent
import org.akvo.flow.util.image.DrawableLoadListener
import org.akvo.flow.util.image.GlideImageLoader
import org.akvo.flow.util.image.ImageLoader
import java.io.File
import java.util.HashMap
import javax.inject.Inject

class MediaQuestionLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var imageView: ImageView
    private var downloadButton: ImageButton
    private var errorPlaceHolder: Drawable
    private var progressBar: ProgressBar
    private var listener: MediaQuestionListener
    private var imageLoader: ImageLoader = GlideImageLoader(context)

    @Inject
    lateinit var downloadMedia: DownloadMedia

    init {
        inflate(context, R.layout.media_question_preview, this)
        imageView = findViewById(R.id.image)
        downloadButton = findViewById(R.id.media_download)
        errorPlaceHolder = ContextCompat.getDrawable(context, R.drawable.blurry_image)!!
        progressBar = findViewById(R.id.media_progress)
        if (context is MediaQuestionListener) {
            listener = context
        } else {
            throw IllegalArgumentException("Activity must implement MediaQuestionListener")
        }
        initialiseInjector()
    }

    private fun initialiseInjector() {
        val viewComponent = DaggerViewComponent.builder().applicationComponent(
            applicationComponent
        )
            .build()
        viewComponent.inject(this)
    }

    private val applicationComponent: ApplicationComponent
        get() = (context.applicationContext as FlowApp).getApplicationComponent()

    fun setUpImageDisplay(index: Int, filePath: String) {
        progressBar.visibility = View.GONE
        downloadButton.visibility = View.GONE
        imageView.setOnClickListener(null)
        val file = File(filePath)
        if (file.exists() && file.canRead()) {
            imageLoader.loadFromFile(file, imageView,
                object : DrawableLoadListener {
                    override fun onLoadFailed() {
                        showDownloadMedia(index, filePath)
                    }

                    override fun onLoadSucceeded() {
                        imageView.setOnClickListener {
                            val isVideo = ".mp4" == filePath.substring(filePath.lastIndexOf("."))
                            if (isVideo) {
                                listener.viewVideo(filePath)
                            } else {
                                listener.viewImage(filePath)
                            }
                        }
                    }
                })
        } else {
            showDownloadMedia(index, filePath)
        }
    }

    private fun showDownloadMedia(index: Int, filePath: String) {
        imageView.setImageDrawable(errorPlaceHolder)
        downloadButton.visibility = View.VISIBLE
        downloadButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            downloadButton.visibility = View.GONE
            val params: MutableMap<String?, Any> = HashMap(2)
            params[DownloadMedia.PARAM_FILE_PATH] = filePath
            downloadMedia.execute(object :
                DisposableCompletableObserver() {
                override fun onComplete() {
                    setUpImageDisplay(index, filePath)
                }

                override fun onError(e: Throwable) {
                    setUpImageDisplay(index, filePath)
                }
            }, params)
        }
    }
}
