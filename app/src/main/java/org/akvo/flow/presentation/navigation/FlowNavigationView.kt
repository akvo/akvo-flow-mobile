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
package org.akvo.flow.presentation.navigation

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.internal.NavigationMenuView
import com.google.android.material.navigation.NavigationView
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.utils.entity.SurveyGroup
import org.akvo.flow.injector.component.ApplicationComponent
import org.akvo.flow.injector.component.DaggerViewComponent
import org.akvo.flow.uicomponents.SnackBarManager
import javax.inject.Inject

class FlowNavigationView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NavigationView(
    context!!, attrs, defStyleAttr
), IFlowNavigationView {
    private lateinit var currentUserTextView: TextView
    private lateinit var surveyTitleTextView: TextView
    private lateinit var surveysRecyclerView: RecyclerView
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var surveyAdapter: SurveyAdapter
    private lateinit var usersAdapter: UserAdapter
    private lateinit var userHeader: View

    private var hideUsersDrawable: Drawable? = null
    private var showUsersDrawable: Drawable? = null
    private var drawerNavigationListener: DrawerNavigationListener? = null

    @Inject
    lateinit var presenter: FlowNavigationPresenter

    @Inject
    lateinit var snackBarManager: SnackBarManager

    init {
        init()
    }

    private fun init() {
        initialiseInjector()
        presenter.setView(this)
        viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    initViews()
                    setNavigationItemListener()
                    initCurrentUserText()
                    initUserList()
                    initSurveyList()
                    presenter.load()
                }
            })
    }

    private fun initViews() {
        currentUserTextView = findViewById(R.id.current_user_name)
        surveyTitleTextView = findViewById(R.id.surveys_title_tv)
        surveysRecyclerView = findViewById(R.id.surveys_rv)
        usersRecyclerView = findViewById(R.id.users_rv)
        userHeader = findViewById(R.id.user_header)
        val navigationMenuView = getChildAt(0) as NavigationMenuView
        navigationMenuView.isVerticalScrollBarEnabled = false
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

    private fun initUserList() {
        val context = context
        usersRecyclerView.layoutManager = LinearLayoutManager(context)
        usersAdapter = UserAdapter(context)
        usersRecyclerView.adapter = usersAdapter
        usersRecyclerView.addOnItemTouchListener(
            RecyclerItemClickListener(context,
                object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(childView: View, position: Int) {
                        presenter.onUserSelected(usersAdapter.getItem(position))
                    }

                    override fun onItemLongPress(childView: View, position: Int) {
                        presenter.onUserLongPress(usersAdapter.getItem(position))
                    }
                })
        )
    }

    private fun setNavigationItemListener() {
        findViewById<View>(R.id.settings_tv).setOnClickListener {
            drawerNavigationListener?.navigateToSettings()
        }
        findViewById<View>(R.id.help_tv).setOnClickListener {
            drawerNavigationListener?.navigateToHelp()
        }
        findViewById<View>(R.id.about_tv).setOnClickListener {
            drawerNavigationListener?.navigateToAbout()
        }
        findViewById<View>(R.id.offline_maps_tv).setOnClickListener {
            drawerNavigationListener?.navigateToOfflineMaps()
        }
    }

    private fun initSurveyList() {
        val context = context
        surveysRecyclerView.layoutManager = LinearLayoutManager(context)
        surveyAdapter = SurveyAdapter(context)
        surveysRecyclerView.adapter = surveyAdapter
        surveysRecyclerView.addOnItemTouchListener(
            RecyclerItemClickListener(context,
                object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(childView: View, position: Int) {
                        onSurveyItemTap(position)
                    }

                    override fun onItemLongPress(childView: View, position: Int) {
                        onSurveyItemLongPress(position, surveyAdapter)
                    }
                })
        )
    }

    private fun initCurrentUserText() {
        hideUsersDrawable = ContextCompat.getDrawable(context, R.drawable.ic_expand_less)
        showUsersDrawable = ContextCompat.getDrawable(context, R.drawable.ic_expand_more)
        userHeader.setOnClickListener {
            if (surveysRecyclerView.visibility == VISIBLE) {
                updateTextViewDrawable(hideUsersDrawable)
                surveyTitleTextView.visibility = GONE
                surveysRecyclerView.visibility = GONE
                usersRecyclerView.visibility = VISIBLE
            } else {
                updateTextViewDrawable(showUsersDrawable)
                surveyTitleTextView.visibility = VISIBLE
                surveysRecyclerView.visibility = VISIBLE
                usersRecyclerView.visibility = GONE
            }
        }
        userHeader.setOnLongClickListener {
            presenter.onCurrentUserLongPress()
            true
        }
    }

    private fun updateTextViewDrawable(drawable: Drawable?) {
        currentUserTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
    }

    private fun onSurveyItemLongPress(position: Int, adapter: SurveyAdapter) {
        val viewSurvey = adapter.getItem(position)
        val dialogFragment: DialogFragment = SurveyDeleteConfirmationDialog
            .newInstance(viewSurvey)
        dialogFragment.show(supportFragmentManager, SurveyDeleteConfirmationDialog.TAG)
    }

    private fun onSurveyItemTap(position: Int) {
        presenter.onSurveyItemTap(surveyAdapter.getItem(position))
    }

    fun setDrawerNavigationListener(drawerNavigationListener: DrawerNavigationListener?) {
        this.drawerNavigationListener = drawerNavigationListener
    }

    override fun displaySurveys(surveys: List<ViewSurvey>, selectedSurveyId: Long?) {
        surveyAdapter.setSurveys(surveys, selectedSurveyId!!)
        val newSurveys = surveys.filter {
            !it.viewed
        }
        drawerNavigationListener?.updateDrawerIcon(newSurveys)
    }

    override fun notifySurveyDeleted(surveyGroupId: Long) {
            drawerNavigationListener?.onSurveyDeleted(surveyGroupId)
    }

    override fun selectSurvey(surveyGroup: SurveyGroup?) {
        drawerNavigationListener?.let { drawerNavigationListener ->
            drawerNavigationListener.onSurveySelected(surveyGroup)
            surveyAdapter.updateSelected(surveyGroup!!.id)
        }
    }

    override fun displayUsers(selectedUserName: String?, viewUsers: List<ViewUser>?) {
        usersAdapter.setUsers(viewUsers)
        currentUserTextView.text = selectedUserName
    }

    override fun displayAddUser() {
        val dialog = CreateUserDialog()
        dialog.show(supportFragmentManager, CreateUserDialog.TAG)
    }

    override fun displaySurveyError() {
        snackBarManager.displaySnackBar(this, R.string.surveys_error, context)
    }

    override fun displayUsersError() {
        snackBarManager.displaySnackBar(this, R.string.users_error, context)
    }

    override fun displayErrorDeleteSurvey() {
        snackBarManager.displaySnackBar(this, R.string.survey_delete_error, context)
    }

    override fun displayErrorSelectSurvey() {
        snackBarManager.displaySnackBar(this, R.string.survey_select_error, context)
    }

    override fun displayUserEditError() {
        snackBarManager.displaySnackBar(this, R.string.user_edit_error, context)
    }

    override fun displayUserDeleteError() {
        snackBarManager.displaySnackBar(this, R.string.user_delete_error, context)
    }

    override fun displayUserSelectError() {
        snackBarManager.displaySnackBar(this, R.string.user_select_error, context)
    }

    override fun displayEditUser(currentUser: ViewUser?) {
        val fragment: DialogFragment = EditUserDialog.newInstance(currentUser)
        fragment.show(supportFragmentManager, EditUserDialog.TAG)
    }

    override fun onUserLongPress(viewUser: ViewUser?) {
        val dialogFragment: DialogFragment = UserOptionsDialog.newInstance(viewUser)
        dialogFragment.show(supportFragmentManager, UserOptionsDialog.TAG)
    }

    private val supportFragmentManager: FragmentManager
        get() = (context as AppCompatActivity).supportFragmentManager

    fun onSurveyDeleteConfirmed(surveyGroupId: Long) {
        presenter.onDeleteSurvey(surveyGroupId)
    }

    override fun onDetachedFromWindow() {
        presenter.destroy()
        super.onDetachedFromWindow()
    }

    fun editUser(viewUser: ViewUser?) {
        presenter.editUser(viewUser)
    }

    fun deleteUser(viewUser: ViewUser?) {
        presenter.deleteUser(viewUser)
    }

    fun createUser(userName: String?) {
        presenter.createUser(userName)
    }

    interface DrawerNavigationListener {
        fun onSurveySelected(surveyGroup: SurveyGroup?)
        fun onSurveyDeleted(surveyGroupId: Long)
        fun navigateToHelp()
        fun navigateToAbout()
        fun navigateToSettings()
        fun navigateToOfflineMaps()
        fun updateDrawerIcon(newSurveys: List<ViewSurvey>)
    }
}
