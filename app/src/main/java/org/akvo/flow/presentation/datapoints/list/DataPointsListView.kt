/*
 * Copyright (C) 2017-2018,2020 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.presentation.datapoints.list.entity.ListDataPoint

interface DataPointsListView {
    fun showNoDataPoints(monitored: Boolean)
    fun showLoading()
    fun hideLoading()
    fun showDownloadedResults(numberOfNewDataPoints: Int)
    fun showErrorNoNetwork()
    fun showErrorSync()
    fun displayData(listDataPoints: List<ListDataPoint>)
    fun showErrorMissingLocation()
    fun showNoSurveySelected()
    fun showOrderByDialog(orderBy: Int)
    fun showErrorAssignmentMissing()
    fun displayNoSearchResultsFound()
    fun showNoDataPointsToSync()
    fun hideMenu()
    fun showNonMonitoredMenu()
    fun showMonitoredMenu()
}
