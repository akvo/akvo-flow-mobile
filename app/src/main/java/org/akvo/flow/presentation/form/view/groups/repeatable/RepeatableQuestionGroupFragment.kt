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

package org.akvo.flow.presentation.form.view.groups.repeatable

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
import org.akvo.flow.presentation.form.view.groups.QuestionGroupPresenter
import org.akvo.flow.presentation.form.view.groups.QuestionGroupView
import org.akvo.flow.util.image.GlideImageLoader
import org.akvo.flow.util.image.ImageLoader
import javax.inject.Inject

class RepeatableQuestionGroupFragment : Fragment(), QuestionGroupView {

    private lateinit var questionGroupTitle: String
    private lateinit var groupRepetitions: ArrayList<GroupRepetition>
    private lateinit var imageLoader: ImageLoader
    private lateinit var repetitionsRv: RecyclerView

    @Inject
    lateinit var presenter: QuestionGroupPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questionGroupTitle = arguments!!.getString(QUESTION_GROUP_TITLE, "")
        groupRepetitions = arguments!!.getParcelableArrayList(REPETITIONS_LIST)?: arrayListOf()
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
        return inflater.inflate(R.layout.repeatable_question_group_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repetitionsRv = view.findViewById(R.id.repetitionsRv)
        repetitionsRv.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = RepeatableGroupQuestionAdapter(groupRepetitions)
        }
    }

    //TODO: fix this
    override fun showDownloadSuccess(viewIndex: Int) {
      //  (repetitionsRv.adapter as GroupQuestionsAdapter).showDownLoadSuccess(viewIndex)
    }

    //TODO: fix this
    override fun showDownloadFailed(viewIndex: Int) {
      //  (repetitionsRv.adapter as GroupQuestionsAdapter).showDownLoadFailed(viewIndex)
    }

    //TODO: fix this
    fun downloadMedia(filename: String, index: Int) {
        presenter.downloadMedia(filename, index)
    }

    companion object {

        private const val QUESTION_GROUP_TITLE = "group_title"
        private const val REPETITIONS_LIST = "repetitions_list"

        @JvmStatic
        fun newInstance(
            questionGroupTitle: String,
            questionAnswers: ArrayList<GroupRepetition>
        ): RepeatableQuestionGroupFragment {
            return RepeatableQuestionGroupFragment().apply {
                arguments = Bundle().apply {
                    putString(QUESTION_GROUP_TITLE, questionGroupTitle)
                    putParcelableArrayList(REPETITIONS_LIST, questionAnswers)
                }
            }
        }
    }
}
