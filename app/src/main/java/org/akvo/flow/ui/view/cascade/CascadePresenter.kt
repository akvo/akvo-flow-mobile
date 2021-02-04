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

package org.akvo.flow.ui.view.cascade

import android.content.Context
import android.text.TextUtils
import android.util.SparseArray
import androidx.core.util.isEmpty
import androidx.core.util.isNotEmpty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akvo.flow.data.database.cascade.CascadeDB
import org.akvo.flow.domain.Node
import org.akvo.flow.presentation.Presenter
import org.akvo.flow.util.files.FormResourcesFileBrowser
import java.io.File
import javax.inject.Inject

class CascadePresenter @Inject constructor(): Presenter {

    private var isLoading: Boolean = false

    private var values: SparseArray<List<Node>> = SparseArray()
    private var job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    @Inject
    lateinit var resourcesFileUtil: FormResourcesFileBrowser
    private var view: CascadeView? = null
    var isValidDatabase = false

    override fun destroy() {
        uiScope.coroutineContext.cancelChildren()
    }

    fun setView(view: CascadeView) {
       this.view = view
    }

    fun loadCascadeData(src: String, context: Context) {
        uiScope.launch {
            if (values.isEmpty() && !TextUtils.isEmpty(src)) {
                isLoading = true
                values = loadCascadeFromDb(context, src)
                isLoading = false
                isValidDatabase = values.isNotEmpty()
            }

            view?.displayCascades()
        }
    }

    private suspend fun loadCascadeFromDb(context: Context, src: String): SparseArray<List<Node>> {
        var values: SparseArray<List<Node>> = SparseArray()
        return withContext(Dispatchers.IO) {
            val db: File = resourcesFileUtil.findFile(context.applicationContext, src)
            if (db.exists()) {
                val cascadeDB = CascadeDB(context, db.absolutePath)
                cascadeDB.open()
                values = cascadeDB.loadAllValues()
                cascadeDB.close()
            }
            values
        }
    }

    fun loadValuesForParent(parent: Long?): List<Node> {
        return parent?.let { values.get(parent.toInt(), emptyList()) } ?: emptyList()
    }
}