/*
 * Copyright (C) 2017-2020 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.presentation.datapoints.list

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import org.akvo.flow.R
import org.akvo.flow.database.SurveyInstanceStatus
import org.akvo.flow.presentation.datapoints.list.entity.ListDataPoint

internal class DataPointListAdapter(context: Context?) : BaseAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val dataPoints: MutableList<ListDataPoint> = mutableListOf()
    private var displayIds: Boolean = false

    override fun getCount(): Int {
        return dataPoints.size
    }

    override fun getItem(position: Int): ListDataPoint {
        return dataPoints[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val view: View =
            convertView ?: inflater.inflate(R.layout.datapoint_list_item, parent, false)
        val nameView = view.findViewById<TextView>(R.id.locale_name)
        val dateView = view.findViewById<TextView>(R.id.last_modified)
        val idView = view.findViewById<TextView>(R.id.locale_id)
        val distanceView = view.findViewById<TextView>(R.id.locale_distance)
        val statusImage = view.findViewById<ImageView>(R.id.status_img)

        val (displayName, status, id, _, _, displayDate, viewed, distanceText) = getItem(position)
        nameView.text = displayName
        displayDistanceText(distanceView, distanceText)
        displayDateText(dateView, displayDate)
        idView.text = id
        if (displayIds) {
            idView.visibility = View.VISIBLE
        } else {
            idView.visibility = View.GONE
        }
        var statusRes = 0
        when (status) {
            SurveyInstanceStatus.SAVED, SurveyInstanceStatus.SUBMIT_REQUESTED -> statusRes =
                R.drawable.ic_status_saved_18dp
            SurveyInstanceStatus.SUBMITTED -> statusRes = R.drawable.ic_status_submitted_18dp
            SurveyInstanceStatus.UPLOADED, SurveyInstanceStatus.DOWNLOADED -> statusRes =
                R.drawable.ic_status_synced_18dp
        }
        statusImage.setImageResource(statusRes)
        if (viewed) {
            nameView.setTypeface(null, Typeface.NORMAL)
        } else {
            nameView.setTypeface(null, Typeface.BOLD)
        }
        return view
    }

    private fun displayDateText(tv: TextView, date: String?) {
        if (date == null || date.isEmpty()) {
            tv.visibility = View.GONE
        } else {
            tv.visibility = View.VISIBLE
            tv.text = date
        }
    }

    private fun displayDistanceText(tv: TextView, distance: String) {
        if (!TextUtils.isEmpty(distance)) {
            tv.visibility = View.VISIBLE
            tv.text = distance
        } else {
            tv.visibility = View.GONE
        }
    }

    fun setDataPoints(dataPoints: List<ListDataPoint>?) {
        this.dataPoints.clear()
        this.dataPoints.addAll(dataPoints!!)
        notifyDataSetChanged()
    }

    fun displayIds(display: Boolean) {
        this.displayIds = display
        notifyDataSetChanged()
    }
}
