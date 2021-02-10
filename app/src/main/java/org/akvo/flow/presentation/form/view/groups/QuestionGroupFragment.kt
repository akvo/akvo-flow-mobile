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
import androidx.recyclerview.widget.RecyclerView
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.injector.component.ApplicationComponent
import org.akvo.flow.injector.component.DaggerViewComponent
import org.akvo.flow.presentation.form.view.groups.entity.ViewQuestionAnswer
import org.akvo.flow.util.image.GlideImageLoader
import org.akvo.flow.util.image.ImageLoader
import javax.inject.Inject

class QuestionGroupFragment : Fragment(), QuestionGroupView {

    private lateinit var questionGroupTitle: String
    private lateinit var questionAnswers: ArrayList<ViewQuestionAnswer>
    private lateinit var imageLoader: ImageLoader
    private lateinit var questionsRv: RecyclerView

    @Inject
    lateinit var presenter: QuestionGroupPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questionGroupTitle = arguments!!.getString(QUESTION_GROUP_TITLE, "")
        questionAnswers = arguments!!.getParcelableArrayList(QUESTION_ANSWERS)?: arrayListOf()
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
        questionsRv = view.findViewById(R.id.questionsRv)
        questionsRv.layoutManager = LinearLayoutManager(activity)
        questionsRv.recycledViewPool.setMaxRecycledViews(
            GroupQuestionsAdapter.ViewType.OPTION.ordinal,
            0
        )
        questionsRv.recycledViewPool.setMaxRecycledViews(
            GroupQuestionsAdapter.ViewType.BARCODE.ordinal,
            0
        )
        questionsRv.recycledViewPool.setMaxRecycledViews(
            GroupQuestionsAdapter.ViewType.CASCADE.ordinal,
            0
        )

        questionsRv.adapter = GroupQuestionsAdapter<QuestionViewHolder<ViewQuestionAnswer>>(questionAnswers)
    }

    override fun showDownloadSuccess(viewIndex: Int) {
        (questionsRv.adapter as GroupQuestionsAdapter).showDownLoadSuccess(viewIndex)
    }

    override fun showDownloadFailed(viewIndex: Int) {
        (questionsRv.adapter as GroupQuestionsAdapter).showDownLoadFailed(viewIndex)
    }

    fun downloadMedia(filename: String, index: Int) {
        presenter.downloadMedia(filename, index)
    }

    companion object {

        private const val QUESTION_GROUP_TITLE = "group_title"
        private const val QUESTION_ANSWERS = "answers"

        @JvmStatic
        fun newInstance(
            questionGroupTitle: String,
            questionAnswers: ArrayList<ViewQuestionAnswer>
        ): QuestionGroupFragment {
            return QuestionGroupFragment().apply {
                arguments = Bundle().apply {
                    putString(QUESTION_GROUP_TITLE, questionGroupTitle)
                    putParcelableArrayList(QUESTION_ANSWERS, questionAnswers)
                }
            }
        }
    }
}
