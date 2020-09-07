/*
 * Copyright (C) 2017,2019-2020 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.presentation.navigation

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.akvo.flow.R
import org.akvo.flow.presentation.navigation.SurveyAdapter.SurveyViewHolder
import java.util.ArrayList

internal class SurveyAdapter(context: Context?) : RecyclerView.Adapter<SurveyViewHolder>() {
    private val surveyList: MutableList<ViewSurvey>
    private val selectedTextColor: Int
    private val textColor: Int
    private var selectedSurveyId: Long = 0

    init {
        surveyList = ArrayList()
        selectedTextColor = ContextCompat.getColor(context!!, R.color.orange_main)
        textColor = ContextCompat.getColor(context, R.color.black_main)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SurveyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.navigation_item, parent, false)
        return SurveyViewHolder(view, selectedTextColor, textColor)
    }

    fun setSurveys(surveys: List<ViewSurvey>, selectedSurveyId: Long) {
        this.selectedSurveyId = selectedSurveyId
        surveyList.clear()
        if (surveys.isNotEmpty()) {
            surveyList.addAll(surveys)
        }
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: SurveyViewHolder, position: Int) {
        val viewSurvey = surveyList[position]
        holder.setViews(viewSurvey, selectedSurveyId == viewSurvey.id)
    }

    override fun getItemCount(): Int {
        return surveyList.size
    }

    fun getItem(position: Int): ViewSurvey {
        return surveyList[position]
    }

    fun updateSelected(surveyId: Long) {
        selectedSurveyId = surveyId
        notifyDataSetChanged()
    }

    internal class SurveyViewHolder(
        view: View, private val selectedTextColor: Int,
        private val textColor: Int
    ) :
        RecyclerView.ViewHolder(view) {
        private val surveyTv: TextView = view.findViewById(R.id.item_text_view)
        fun setViews(navigationItem: ViewSurvey, isSelected: Boolean) {
            surveyTv.text = navigationItem.name
            surveyTv.setTextColor(if (isSelected) selectedTextColor else textColor)
        }
    }
}
