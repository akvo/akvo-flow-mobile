/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.datapoints.map.offline.list.entity;

public class ListOfflineArea {

    private final long id;
    private final String name;
    private final String size;
    private final boolean isDownloading;
    private final boolean isAvailable;

    public ListOfflineArea(long id, String name, String size, boolean isDownloading,
            boolean isAvailable) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.isDownloading = isDownloading;
        this.isAvailable = isAvailable;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSize() {
        return size;
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ListOfflineArea that = (ListOfflineArea) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
