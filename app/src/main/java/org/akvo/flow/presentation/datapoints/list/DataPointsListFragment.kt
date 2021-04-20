/*
 * Copyright (C) 2017-2021 Stichting Akvo (Akvo Foundation)
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PorterDuff
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.injector.component.ApplicationComponent
import org.akvo.flow.injector.component.DaggerViewComponent
import org.akvo.flow.presentation.datapoints.DataPointSyncSnackBarManager
import org.akvo.flow.presentation.datapoints.list.entity.ListDataPoint
import org.akvo.flow.tracking.TrackingListener
import org.akvo.flow.ui.Navigator
import org.akvo.flow.ui.fragment.OrderByDialogFragment
import org.akvo.flow.ui.fragment.OrderByDialogFragment.OrderByDialogListener
import org.akvo.flow.ui.fragment.RecordListListener
import org.akvo.flow.util.ConstantUtil
import org.akvo.flow.util.WeakLocationListener
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

class DataPointsListFragment : Fragment(), LocationListener, AdapterView.OnItemClickListener,
    OrderByDialogListener, DataPointsListView {

    @Inject
    lateinit var dataPointSyncSnackBarManager: DataPointSyncSnackBarManager

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var presenter: DataPointsListPresenter

    private lateinit var weakLocationListener: WeakLocationListener

    private var mLocationManager: LocationManager? = null

    private var mLatitude: Double? = null
    private var mLongitude: Double? = null
    private var mListener: RecordListListener? = null
    private var trackingListener: TrackingListener? = null
    private lateinit var mAdapter: DataPointListAdapter
    private lateinit var listView: ListView
    private lateinit var emptyTitleTv: TextView
    private lateinit var emptySubTitleTv: TextView
    private lateinit var emptyIv: ImageView
    private lateinit var progressBar: ProgressBar

    private var searchView: SearchView? = null
    private var menuRes: Int? = null

    /**
     * BroadcastReceiver to notify of data synchronisation. This should be
     * fired from [DataPointUploadWorker]
     */
    private val dataSyncReceiver: BroadcastReceiver = DataSyncBroadcastReceiver(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeInjector()
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        val activity = activity
        mListener = try {
            activity as RecordListListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(
                activity.toString()
                        + " must implement SurveyedLocalesFragmentListener"
            )
        }
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
        return inflater.inflate(R.layout.data_points_list_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mLocationManager = activity!!.applicationContext
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager
        weakLocationListener = WeakLocationListener(this)
        val view = view!!
        listView = view.findViewById(R.id.locales_lv)
        val emptyView = view.findViewById<View>(R.id.empty_view)
        listView.emptyView = emptyView
        emptyTitleTv = view.findViewById(R.id.empty_title_tv)
        emptySubTitleTv = view.findViewById(R.id.empty_subtitle_tv)
        emptyIv = view.findViewById(R.id.empty_iv)
        val surveyGroup = arguments
            ?.getSerializable(ConstantUtil.SURVEY_EXTRA) as SurveyGroup?
        mAdapter = DataPointListAdapter(activity)
        listView.adapter = mAdapter
        listView.onItemClickListener = this
        progressBar = view.findViewById(R.id.progress)
        updateProgressDrawable()

        presenter.setView(this)
        presenter.onDataReady(surveyGroup)
    }

    private fun updateProgressDrawable() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val progressDrawable = progressBar.indeterminateDrawable
            progressDrawable?.setColorFilter(
                ContextCompat.getColor(activity!!, R.color.colorAccent),
                PorterDuff.Mode.MULTIPLY
            )
        }
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
        LocalBroadcastManager.getInstance(activity!!).registerReceiver(
            dataSyncReceiver,
            IntentFilter(ConstantUtil.ACTION_DATA_SYNC)
        )
        if (searchView == null || searchView!!.isIconified) {
            updateLocation()
            presenter.loadDataPoints(mLatitude, mLongitude)
        }
        listView.onItemClickListener = this
    }

    private fun updateLocation() {
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_FINE
        val provider = mLocationManager?.getBestProvider(criteria, true)
        try {
            if (provider != null) {
                val loc = mLocationManager!!.getLastKnownLocation(provider)
                if (loc != null) {
                    mLatitude = loc.latitude
                    mLongitude = loc.longitude
                }
                mLocationManager!!.requestLocationUpdates(provider, 1000, 0f, weakLocationListener)
            }
        } catch (e: SecurityException) {
            Timber.e("Missing permission")
        }
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(activity!!).unregisterReceiver(dataSyncReceiver)
        mLocationManager?.removeUpdates(weakLocationListener)
    }

    override fun onStop() {
        super.onStop()
        mLocationManager?.removeUpdates(weakLocationListener)
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
        trackingListener = null
    }

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    fun onNewSurveySelected(surveyGroup: SurveyGroup?) {
        arguments!!.putSerializable(ConstantUtil.SURVEY_EXTRA, surveyGroup)
        presenter.onNewSurveySelected(surveyGroup)
    }

    override fun showOrderByDialog(orderBy: Int) {
        val dialogFragment: DialogFragment = OrderByDialogFragment.instantiate(orderBy)
        dialogFragment.setTargetFragment(this, 0)
        dialogFragment.show(fragmentManager!!, OrderByDialogFragment.FRAGMENT_ORDER_BY_TAG)
    }

    override fun showNoSurveySelected() {
        emptyTitleTv.setText(R.string.no_survey_selected_text)
        emptySubTitleTv.text = ""
    }

    override fun showNoDataPoints(monitored: Boolean) {
        emptyTitleTv.setText(R.string.no_datapoints_error_text)
        val subtitleResource =
            if (monitored) R.string.no_records_subtitle_monitored else R.string.no_records_subtitle_non_monitored
        emptySubTitleTv.setText(subtitleResource)
        emptyIv.setImageResource(R.drawable.ic_format_list_bulleted)
    }

    override fun showLoading() {
        progressBar.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        listView.onItemClickListener = null
        val (_, _, localeId) = mAdapter.getItem(position)
        mListener!!.onDatapointSelected(localeId)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menuRes?.let { menuRes ->
            inflater.inflate(menuRes, menu)
            setUpSearchView(menu)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun setUpSearchView(menu: Menu) {
        val searchMenuItem = menu.findItem(R.id.search)
        searchView = searchMenuItem.actionView as SearchView
        searchView?.let { searchView ->
            searchView.setIconifiedByDefault(true)
            searchView.queryHint = getString(R.string.search_hint)
            searchView.setOnQueryTextListener(object :
                SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    // Empty
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    if (!TextUtils.isEmpty(newText)) {
                        presenter.getFilteredDataPoints(newText)
                    } else {
                        presenter.loadDataPoints(mLatitude, mLongitude)
                    }
                    return false
                }
            })
        }

        searchMenuItem.setOnActionExpandListener(
            object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    trackingListener?.logSearchEvent()
                    mAdapter.displayIds(true)
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    presenter.loadDataPoints(mLatitude, mLongitude)
                    mAdapter.displayIds(false)
                    return true
                }
            })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.order_by -> {
                presenter.onOrderByClicked()
                trackingListener?.logSortEvent()
                true
            }
            R.id.download -> {
                presenter.onDownloadPressed()
                trackingListener?.logDownloadEvent(LIST_TAB)
                true
            }
            R.id.upload -> {
                presenter.onUploadPressed()
                trackingListener?.logUploadEvent(LIST_TAB)
                true
            }
            else -> false
        }
    }

    override fun onOrderByClick(order: Int) {
        presenter.onOrderByClick(order)
        trackingListener?.logOrderEvent(order)
    }

    override fun onLocationChanged(location: Location) {
        // a single location is all we need
        mLocationManager!!.removeUpdates(weakLocationListener)
        mLatitude = location.latitude
        mLongitude = location.longitude
        presenter.loadDataPoints(mLatitude, mLongitude)
    }

    override fun onProviderDisabled(provider: String) {
        // EMPTY
    }

    override fun onProviderEnabled(provider: String) {
        // EMPTY
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        // EMPTY
    }

    private fun refreshLocalData() {
        presenter.loadDataPoints(mLatitude, mLongitude)
    }

    override fun displayData(listDataPoints: List<ListDataPoint>) {
        mAdapter.setDataPoints(listDataPoints)
    }

    override fun showErrorMissingLocation() {
        //TODO: should we prompt the user to enable location?
        Toast.makeText(activity, R.string.locale_list_error_unknown_location, Toast.LENGTH_SHORT)
            .show()
    }

    override fun showNonMonitoredMenu() {
        menuRes = R.menu.datapoints_list
        reloadMenu()
    }

    override fun showMonitoredMenu() {
        menuRes = R.menu.datapoints_list_monitored
        reloadMenu()
    }

    override fun hideMenu() {
        menuRes = null
        reloadMenu()
    }

    private fun reloadMenu() {
        activity?.invalidateOptionsMenu()
    }

    override fun showDownloadedResults(numberOfNewDataPoints: Int) {
        dataPointSyncSnackBarManager.showDownloadedResults(numberOfNewDataPoints, view)
    }

    override fun showErrorAssignmentMissing() {
        dataPointSyncSnackBarManager.showErrorAssignmentMissing(view)
    }

    override fun showErrorNoNetwork() {
        dataPointSyncSnackBarManager.showErrorNoNetwork(view) { presenter.onDownloadPressed() }
    }

    override fun showErrorSync() {
        dataPointSyncSnackBarManager.showErrorSync(view) { presenter.onDownloadPressed() }
    }

    override fun displayNoSearchResultsFound() {
        emptyTitleTv.setText(R.string.no_search_results_error_text)
        emptySubTitleTv.text = ""
        emptyIv.setImageResource(R.drawable.ic_search_results_error)
    }

    override fun showNoDataPointsToSync() {
        dataPointSyncSnackBarManager.showNoDataPointsToDownload(view)
    }

    fun enableItemClicks() {
        listView.onItemClickListener = this
    }

    class DataSyncBroadcastReceiver(fragment: DataPointsListFragment) :
        BroadcastReceiver() {

        private val fragmentWeakRef: WeakReference<DataPointsListFragment> = WeakReference(fragment)
        override fun onReceive(context: Context, intent: Intent) {
            Timber.i("Survey Instance status has changed. Refreshing UI...")
            val fragment = fragmentWeakRef.get()
            fragment?.refreshLocalData()
        }
    }

    companion object {
        private const val LIST_TAB = 0

        @JvmStatic
        fun newInstance(surveyGroup: SurveyGroup?): DataPointsListFragment {
            val fragment = DataPointsListFragment()
            val args = Bundle()
            args.putSerializable(ConstantUtil.SURVEY_EXTRA, surveyGroup)
            fragment.arguments = args
            return fragment
        }
    }
}
