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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.question_group_fragment.questionsRv
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.injector.component.ApplicationComponent
import org.akvo.flow.injector.component.DaggerViewComponent
import org.akvo.flow.presentation.form.view.groups.entity.ImageLocation
import org.akvo.flow.presentation.form.view.groups.entity.ViewQuestionAnswer
import org.akvo.flow.util.MediaFileHelper
import org.akvo.flow.util.files.FileBrowser
import org.akvo.flow.util.image.GlideImageLoader
import org.akvo.flow.util.image.ImageLoader
import javax.inject.Inject

class QuestionGroupFragment : Fragment(),
    QuestionGroupView {

    private lateinit var questionGroupTitle: String
    private lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var presenter: QuestionGroupPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questionGroupTitle = arguments!!.getString(QUESTION_GROUP_TITLE, "")
        initialiseInjector()
        imageLoader = GlideImageLoader(this)
        presenter.setView(this)
    }

    private fun initialiseInjector() {
        val viewComponent =
            DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                .build()
        viewComponent.inject(this)
    }

    private fun getApplicationComponent(): ApplicationComponent? {
        return (context?.applicationContext as FlowApp).getApplicationComponent()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.question_group_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        questionsRv.layoutManager = LinearLayoutManager(activity)
        val questionAnswer1 = ViewQuestionAnswer.FreeTextViewQuestionAnswer(
            "123",
            "1. text question",
            false,
            emptyList(),
            "valeria",
            true
        )

        val questionAnswer2 =
            ViewQuestionAnswer.NumberViewQuestionAnswer(
                "123", "2. number question", false, emptyList(),
                "2.0",
                requireDoubleEntry = true
            )

    /*    val barcodeAnswer =
            ViewQuestionAnswer.BarcodeViewQuestionAnswer(
                "123", "3. barcode question", false, emptyList()
            ) //ADD list of answers
*/
        val emptyQuestionAnswer =
            ViewQuestionAnswer.FreeTextViewQuestionAnswer(
                "123",
                "4. empty question",
                false,
                emptyList(),
                "",
                true
            )

        val dateAnswer =
            ViewQuestionAnswer.DateViewQuestionAnswer(
                "123", "5. date question", false, emptyList(), "Oct 23, 2019"
            )

        //TODO: this will be done in mapper
        val mediaFileHelper = MediaFileHelper(context, FileBrowser())
        val filePath =
            mediaFileHelper.getMediaFile("afdba3c3-911a-439c-a1e8-e4cff07e0598.jpg").absolutePath
        val photoAnswer =
            ViewQuestionAnswer.PhotoViewQuestionAnswer(
                "123",
                "6. image question",
                false,
                emptyList(),
                filePath,
                ImageLocation("42.0", "2.2")
            )

        val filePathVideo =
            mediaFileHelper.getMediaFile("1afe78fb-2d94-40e2-b425-9e531c51c288.mp4").absolutePath
        val videoAnswer =
            ViewQuestionAnswer.VideoViewQuestionAnswer(
                "123",
                "7. video question",
                false,
                emptyList(),
                filePathVideo
            )

        val signatureAnswer =
            ViewQuestionAnswer.SignatureViewQuestionAnswer(
                "123",
                "9. signature question",
                false,
                emptyList(),
                "iVBORw0KGgoAAAANSUhEUgAAAUAAAADwCAIAAAD+Tyo8AAAC+ElEQVR4nOzVPQ2CYRgEQf5U4AkpiEAIhnBA6LBAR4UHkjdP9suMgms2d7qfPzu26/l9TU9gocP0AOB/AoYwAUOYgCFMwBAmYAgTMIQJGMIEDGEChjABQ5iAIUzAECZgCBMwhAkYwgQMYQKGMAFDmIAhTMAQJmAIEzCECRjCBAxhAoYwAUOYgCFMwBAmYAgTMIQJGMIEDGEChjABQ5iAIUzAECZgCBMwhAkYwgQMYQKGMAFDmIAhTMAQJmAIEzCECRjCBAxhAoYwAUOYgCFMwBAmYAgTMIQJGMIEDGEChjABQ5iAIUzAECZgCBMwhAkYwgQMYQKGMAFDmIAhTMAQJmAIEzCECRjCBAxhAoYwAUOYgCFMwBAmYAgTMIQJGMIEDGEChjABQ5iAIUzAECZgCBMwhAkYwgQMYQKGMAFDmIAhTMAQJmAIEzCECRjCBAxhAoYwAUOYgCFMwBAmYAgTMIQJGMIEDGEChjABQ5iAIUzAECZgCBMwhAkYwgQMYQKGMAFDmIAhTMAQJmAIEzCE7d+Py/QGFrrejtMTWMgDQ5iAIUzAECZgCBMwhAkYwgQMYQKGMAFDmIAhTMAQJmAIEzCECRjCBAxhAoYwAUOYgCFMwBAmYAgTMIQJGMIEDGEChjABQ5iAIUzAECZgCBMwhAkYwgQMYQKGMAFDmIAhTMAQJmAIEzCECRjCBAxhAoYwAUOYgCFMwBAmYAgTMIQJGMIEDGEChjABQ5iAIUzAECZgCBMwhAkYwgQMYQKGMAFDmIAhTMAQJmAIEzCECRjCBAxhAoYwAUOYgCFMwBAmYAgTMIQJGMIEDGEChjABQ5iAIUzAECZgCBMwhAkYwgQMYQKGMAFDmIAhTMAQJmAIEzCECRjCBAxhAoYwAUOYgCFMwBAmYAgTMIQJGMIEDGEChjABQ5iAIUzAECZgCBMwhAkYwgQMYQKGMAFDmIAhTMAQJmAIEzCECRjCBAxhAoYwAUOYgCFMwBAmYAgTMIQJGMIEDGG/AAAA//+ijgkkvQyYKAAAAABJRU5ErkJggg==",
                "valeria"
            )

        questionsRv.adapter = GroupQuestionsAdapter<QuestionViewHolder<ViewQuestionAnswer>>(
            mutableListOf(
                questionAnswer1,
                questionAnswer2,
                //barcodeAnswer,
                emptyQuestionAnswer,
                dateAnswer,
                photoAnswer,
                videoAnswer,
                signatureAnswer
            )
        )
    }

    override fun showDownloadSuccess(viewIndex: Int) {
        (questionsRv.adapter as GroupQuestionsAdapter).showDownLoadSuccess(viewIndex)
    }

    override fun showDownloadFailed(viewIndex: Int) {
        (questionsRv.adapter as GroupQuestionsAdapter).showDownLoadFailed(viewIndex)    }

    fun downloadMedia(filename: String, index: Int) {
        presenter.downloadMedia(filename, index)
    }

    companion object {

        private const val QUESTION_GROUP_TITLE = "group_title"

        @JvmStatic
        fun newInstance(questionGroupTitle: String): QuestionGroupFragment {
            return QuestionGroupFragment().apply {
                arguments = Bundle().apply {
                    putString(QUESTION_GROUP_TITLE, questionGroupTitle)
                }
            }
        }
    }
}
