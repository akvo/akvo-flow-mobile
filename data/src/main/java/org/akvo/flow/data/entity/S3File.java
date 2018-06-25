/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.entity;

import java.io.File;

public class S3File {

    public static final String S3_DATA_DIR = "devicezip/";
    public static final String S3_IMAGE_DIR = "images/";
    public static final String ACTION_SUBMIT = "submit";
    public static final String ACTION_IMAGE = "image";

    private final File file;
    private final boolean isPublic;
    private final String dir;
    private final String action;
    private final byte[] rawMd5;

    public S3File(File file, boolean isPublic, String dir, String action, byte[] rawMd5) {
        this.file = file;
        this.isPublic = isPublic;
        this.dir = dir;
        this.action = action;
        this.rawMd5 = rawMd5;
    }

    public File getFile() {
        return file;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public String getDir() {
        return dir;
    }

    public String getAction() {
        return action;
    }

    public byte[] getRawMd5() {
        return rawMd5;
    }

    @Override public String toString() {
        return "S3File{" +
                "file=" + file.getName() +
                '}';
    }
}
