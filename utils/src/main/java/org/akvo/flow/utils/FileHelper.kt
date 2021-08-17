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
package org.akvo.flow.utils

import android.text.TextUtils
import android.util.Base64
import okhttp3.ResponseBody
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.zip.Adler32
import java.util.zip.CheckedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject


class FileHelper @Inject constructor() {
    /**
     * Compute MD5 checksum of the given file
     */
    private fun getMD5Checksum(file: File): ByteArray? {
        var `in`: InputStream? = null
        val md: MessageDigest
        try {
            md = MessageDigest.getInstance("MD5")
            `in` = BufferedInputStream(FileInputStream(file))
            val buffer = ByteArray(BUFFER_SIZE)
            var read: Int
            while (`in`.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
            return md.digest()
        } catch (e: NoSuchAlgorithmException) {
            Timber.e(e)
        } catch (e: IOException) {
            Timber.e(e)
        } finally {
            close(`in`)
        }
        return null
    }

    fun getMd5Base64(file: File): String {
        val md5Checksum = getMD5Checksum(file)
        return if (md5Checksum != null) {
            Base64.encodeToString(md5Checksum, Base64.NO_WRAP)
        } else {
            ""
        }
    }

    fun hexMd5(file: File): String {
        val rawHash = getMD5Checksum(file)
        if (rawHash != null) {
            val builder = StringBuilder()
            for (b in rawHash) {
                builder.append(String.format("%02x", b))
            }
            return builder.toString()
        }
        return ""
    }

    fun copyFileToFolder(originalFile: File, destinationFolder: File?): String? {
        val file = File(destinationFolder, originalFile.name)
        return copyFile(originalFile, file)
    }

    /**
     * Copies a file from originalFile to destinationFile
     *
     * @return the destination file path if copy succeeded, null otherwise
     */
    fun copyFile(originalFile: File?, destinationFile: File): String? {
        var destinationPath: String? = null
        try {
            destinationPath = saveStreamToFile(FileInputStream(originalFile), destinationFile)
        } catch (e: FileNotFoundException) {
            Timber.e(e)
        }
        return destinationPath
    }

    fun saveStreamToFile(inputStream: InputStream, destinationFile: File): String? {
        copyStream(inputStream, destinationFile)
        close(inputStream)
        return destinationFile.absolutePath
    }

    fun close(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (ignored: Exception) {
                //Ignored
            }
        }
    }

    /**
     * deletes all files in the directory (recursively) AND then deletes the
     * directory itself if the "deleteFlag" is true
     */
    fun deleteFilesInDirectory(folder: File?, deleteFolder: Boolean) {
        if (folder != null && folder.exists() && folder.isDirectory) {
            val files = folder.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile) {
                        file.delete()
                    } else {
                        // recursively delete
                        deleteFilesInDirectory(file, true)
                    }
                }
            }
            // now delete the directory itself
            if (deleteFolder) {
                folder.delete()
            }
        }
    }

    fun getFilenameFromPath(filePath: String?): String? {
        var filename: String? = null
        if (!TextUtils.isEmpty(filePath) && filePath!!.contains(File.separator)
            && filePath.contains(".")
        ) {
            filename = filePath.substring(filePath.lastIndexOf(File.separator) + 1)
        }
        return filename
    }

    fun deleteFile(zipFolder: File?, zipFileName: String?) {
        val zipFile = File(zipFolder, zipFileName)
        if (zipFile.exists()) {
            zipFile.delete()
        }
    }

    @Throws(IOException::class)
    fun writeZipFile(zipFolder: File?, zipFileName: String?, formInstanceData: String) {
        val zipFile = File(zipFolder, zipFileName)
        Timber.d("Writing zip to file %s", zipFile.name)
        val fout = FileOutputStream(zipFile)
        val checkedOutStream = CheckedOutputStream(fout, Adler32())
        val zos = ZipOutputStream(checkedOutStream)
        zos.putNextEntry(ZipEntry(SURVEY_DATA_FILE_JSON))
        val allBytes = formInstanceData.toByteArray(charset(UTF_8_CHARSET))
        zos.write(allBytes, 0, allBytes.size)
        zos.closeEntry()
        zos.close()
        fout.close()
    }

    fun extractOnlineArchive(responseBody: ResponseBody, targetFolder: File) {
        val inputStream = responseBody.byteStream()
        extractInputStream(inputStream, targetFolder)
    }

    fun extractInputStream(inputStream: InputStream, targetFolder: File) {
        extractZipContent(inputStream, targetFolder)
        close(inputStream)
    }

    fun saveRemoteFile(responseBody: ResponseBody, filePath: File) {
        val inputStream = responseBody.byteStream()
        saveStreamToFile(inputStream, filePath)
    }

    fun validFile(file: File): Boolean {
        return file.exists() && validZipFile(file)
    }

    private fun validZipFile(file: File): Boolean {
        return try {
            ZipFile(file).size() > 0
        } catch (e: IOException) {
            Timber.e(e)
            false
        }
    }

    private fun copyStream(inputStream: InputStream, destinationFile: File) {
        var out: OutputStream? = null
        try {
            out = FileOutputStream(destinationFile)
            val buffer = ByteArray(1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            out.flush()
        } catch (e: IOException) {
            Timber.e(e)
        } finally {
            close(out)
        }
    }

    private fun extractZipContent(input: InputStream, destinationFolder: File) {
        var zis: ZipInputStream? = null
        try {
            zis = ZipInputStream(input)
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null && !entry.isDirectory) {
                val f = File(destinationFolder, entry.name)
                copyStream(zis, f)
                zis.closeEntry()
                entry = zis.nextEntry
            }
        } catch (e: IOException) {
            Timber.e(e)
        } finally {
            close(zis)
        }
    }

    fun copyFile(inputStream: InputStream, fileOutputStream: FileOutputStream) {
        val buffer = ByteArray(BUFFER_SIZE)
        var size: Int
        while (inputStream.read(buffer, 0, buffer.size).also { size = it } != -1) {
            fileOutputStream.write(buffer, 0, size)
        }
    }

    companion object {
        private const val BUFFER_SIZE = 2048
        const val SURVEY_DATA_FILE_JSON = "data.json"
        const val UTF_8_CHARSET = "UTF-8"
    }
}
