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

package org.akvo.flow.service.bootstrap

import android.os.Environment
import org.akvo.flow.util.FileUtil
import java.io.File
import java.util.ArrayList
import java.util.Locale
import javax.inject.Inject

class ZipFileLister @Inject constructor() {
    /**
     * returns an ordered list of zip files that exist in the device's bootstrap
     * directory
     */
    fun listSortedZipFiles(): List<File> {
        val zipFiles = listZipFiles()
        if (zipFiles.isNotEmpty()) {
            zipFiles.sort()
        }
        return zipFiles
    }

    fun listZipFiles(): MutableList<File> {
        val zipFiles: MutableList<File> = ArrayList()
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            val dir =
                FileUtil.getFilesDir(FileUtil.FileType.INBOX)
            val fileList = dir.listFiles()
            if (fileList != null) {
                for (file in fileList) {
                    if (isZipFile(file)) {
                        zipFiles.add(file)
                    }
                }
            }
        }
        return zipFiles
    }

    private fun isZipFile(file: File): Boolean {
        return file.isFile && file.name.lowercase(Locale.getDefault()).endsWith(ARCHIVE_SUFFIX)
    }

    companion object {
        const val ARCHIVE_SUFFIX = ".zip"
    }
}