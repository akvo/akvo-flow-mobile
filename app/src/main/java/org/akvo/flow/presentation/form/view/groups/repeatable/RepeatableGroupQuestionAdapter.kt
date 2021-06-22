/*
 * Copyright (C) 2021 Stichting Akvo (Akvo Foundation)
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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.akvo.flow.R
import org.akvo.flow.presentation.form.view.groups.GroupQuestionsAdapter
import org.akvo.flow.presentation.form.view.groups.QuestionViewHolder
import org.akvo.flow.presentation.form.view.groups.entity.ViewQuestionAnswer

class RepeatableGroupQuestionAdapter(private val repetitions: List<GroupRepetition>): RecyclerView.Adapter<RepeatableGroupQuestionAdapter.RepetitionViewHolder>() {

    private val viewPool = RecyclerView.RecycledViewPool()

    inner class RepetitionViewHolder(val view: View) :
        RecyclerView.ViewHolder(view) {
        val recyclerView : RecyclerView = view.findViewById(R.id.repetitionsRv)
        val textView: TextView = itemView.findViewById(R.id.repetitionsHeaderTv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepetitionViewHolder {
       return RepetitionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.question_group_repetition, parent, false))
    }

    override fun onBindViewHolder(holder: RepetitionViewHolder, position: Int) {
        val parent = repetitions[position]
        holder.textView.text = parent.header
        holder.recyclerView.apply {
            layoutManager = LinearLayoutManager(holder.recyclerView.context)
            adapter = GroupQuestionsAdapter<QuestionViewHolder<ViewQuestionAnswer>>(parent.questionAnswers.toMutableList())
            setRecycledViewPool(viewPool)
        }
    }

    override fun getItemCount(): Int {
       return repetitions.size
    }
}
