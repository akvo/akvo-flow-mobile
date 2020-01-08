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

import java.util.List;

public class ApiFilesResult {

    private List<String> missingFiles;
    private List<String> missingUnknown;
    private List<String> deletedForms;

    public List<String> getMissingFiles() {
        return missingFiles;
    }

    public void setMissingFiles(List<String> missingFiles) {
        this.missingFiles = missingFiles;
    }

    public List<String> getMissingUnknown() {
        return missingUnknown;
    }

    public void setMissingUnknown(List<String> missingUnknown) {
        this.missingUnknown = missingUnknown;
    }

    public List<String> getDeletedForms() {
        return deletedForms;
    }

    public void setDeletedForms(List<String> deletedForms) {
        this.deletedForms = deletedForms;
    }
}
