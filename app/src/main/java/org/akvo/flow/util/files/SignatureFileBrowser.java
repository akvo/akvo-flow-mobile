/*
 * Copyright (C) 2017,2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util.files;

import android.content.Context;
import androidx.annotation.NonNull;

import java.io.File;

import javax.inject.Inject;

public class SignatureFileBrowser {

    public static final String RESIZED_SUFFIX = "resized";
    public static final String ORIGINAL_SUFFIX = "original";

    private static final String DIR_TMP = "tmp";
    private static final String IMAGE_SUFFIX = ".jpg";
    private static final String SIGNATURE_IMAGE_PREFIX = "signature_";

    private final FileBrowser fileBrowser;
    private Context context;

    @Inject
    public SignatureFileBrowser(FileBrowser fileBrowser, Context context) {
        this.fileBrowser = fileBrowser;
        this.context = context;
    }

    @NonNull
    public File getSignatureImageFile(String sizeSuffix, String questionId, String datapointId) {
        String fileName = generateSignatureFileName(sizeSuffix, questionId, datapointId);
        return new File(fileBrowser.getExistingAppInternalFolder(context, DIR_TMP), fileName);
    }

    private String generateSignatureFileName(String sizeSuffix, String questionId,
            String datapointId) {
        return SIGNATURE_IMAGE_PREFIX + questionId + "_" + datapointId + "_" + sizeSuffix
                + IMAGE_SUFFIX;
    }
}
