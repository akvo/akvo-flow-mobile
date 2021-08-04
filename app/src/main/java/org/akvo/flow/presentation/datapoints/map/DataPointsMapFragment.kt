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
 */
package org.akvo.flow.presentation.datapoints.map

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.geojson.FeatureCollection
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.injector.component.ApplicationComponent
import org.akvo.flow.injector.component.DaggerViewComponent
import org.akvo.flow.maps.presentation.MapBoxMapItemListViewImpl
import org.akvo.flow.maps.presentation.MapReadyCallback
import org.akvo.flow.presentation.datapoints.DataPointSyncSnackBarManager
import org.akvo.flow.tracking.TrackingListener
import org.akvo.flow.ui.Navigator
import org.akvo.flow.util.ConstantUtil
import javax.inject.Inject

class DataPointsMapFragment : Fragment(), DataPointsMapView, MapReadyCallback {
    @Inject
    lateinit var dataPointSyncSnackBarManager: DataPointSyncSnackBarManager

    @Inject
    lateinit var presenter: DataPointsMapPresenter

    @Inject
    lateinit var navigator: Navigator

    private lateinit var progressBar: ProgressBar
    private lateinit var offlineMapsFab: FloatingActionButton
    private lateinit var mapView: MapBoxMapItemListViewImpl

    private var activityJustCreated = false
    private var menuRes: Int? = null
    private var trackingListener: TrackingListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeInjector()
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
        trackingListener = if (activity !is TrackingListener) {
            throw IllegalArgumentException("Activity must implement TrackingListener")
        } else {
            activity
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map_box_map, container, false)
        progressBar = view.findViewById(R.id.progressBar)
        offlineMapsFab = view.findViewById(R.id.offline_maps_fab)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsyncWithCallback(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        presenter.setView(this)
        val surveyGroup =
            arguments?.getSerializable(ConstantUtil.SURVEY_EXTRA) as SurveyGroup?
        presenter.onSurveyGroupReady(surveyGroup)
        activityJustCreated = true
    }

    private fun initializeInjector() {
        val viewComponent = DaggerViewComponent.builder()
            .applicationComponent(applicationComponent)
            .build()
        viewComponent.inject(this)
    }

    private val applicationComponent: ApplicationComponent
        get() = (activity!!.application as FlowApp).getApplicationComponent()

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (!activityJustCreated) {
            mapView.getMapAsyncWithCallback(this)
        }
        activityJustCreated = false
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    fun onNewSurveySelected(surveyGroup: SurveyGroup?) {
        var arguments = arguments
        if (arguments == null) {
            arguments = Bundle()
        }
        arguments.putSerializable(ConstantUtil.SURVEY_EXTRA, surveyGroup)
        setArguments(arguments)
        presenter.onNewSurveySelected(surveyGroup)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (menuRes != null) {
            inflater.inflate(menuRes!!, menu)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.download -> {
                presenter.onSyncRecordsPressed()
                trackingListener?.logDownloadEvent(MAP_TAB)
                true
            }
            R.id.upload -> {
                presenter.onUploadPressed()
                trackingListener?.logUploadEvent(MAP_TAB)
                true
            }
            else -> false
        }
    }

    override fun showProgress() {
        progressBar.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        progressBar.visibility = View.INVISIBLE
    }

    override fun displayDataPoints(dataPoints: FeatureCollection?) {
        mapView.displayDataPoints(dataPoints)
    }

    override fun showNonMonitoredMenu() {
        menuRes = R.menu.datapoints_map
        reloadMenu()
    }

    override fun showMonitoredMenu() {
        menuRes = R.menu.datapoints_map_monitored
        reloadMenu()
    }

    override fun hideMenu() {
        menuRes = null
        reloadMenu()
    }

    private fun reloadMenu() {
        val activity = activity
        activity?.invalidateOptionsMenu()
    }

    override fun showDownloadedResults(numberOfNewItems: Int) {
        dataPointSyncSnackBarManager.showDownloadedResults(numberOfNewItems, view)
    }

    override fun showErrorAssignmentMissing() {
        dataPointSyncSnackBarManager.showErrorAssignmentMissing(view)
    }

    override fun showErrorNoNetwork() {
        dataPointSyncSnackBarManager.showErrorNoNetwork(view) { presenter.onSyncRecordsPressed() }
    }

    override fun showErrorSync() {
        dataPointSyncSnackBarManager.showErrorSync(view) { presenter.onSyncRecordsPressed() }
    }

    override fun showNoDataPointsToDownload() {
        dataPointSyncSnackBarManager.showNoDataPointsToDownload(view)
    }

    override fun showFab() {
        offlineMapsFab.show()
    }

    fun hideFab() {
        offlineMapsFab.hide()
    }

    fun refreshView() {
        mapView.refreshSelectedArea()
    }

    override fun onMapReady() {
        presenter.loadDataPoints()
    }

    companion object {
        private const val MAP_TAB = 1

        @JvmStatic
        fun newInstance(surveyGroup: SurveyGroup?): DataPointsMapFragment {
            val fragment = DataPointsMapFragment()
            val args = Bundle()
            args.putSerializable(ConstantUtil.SURVEY_EXTRA, surveyGroup)
            fragment.arguments = args
            return fragment
        }
    }
}
