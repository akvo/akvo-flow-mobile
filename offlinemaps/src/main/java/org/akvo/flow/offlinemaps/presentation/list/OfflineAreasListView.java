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

package org.akvo.flow.offlinemaps.presentation.list;

import org.akvo.flow.offlinemaps.domain.entity.DomainOfflineArea;

import java.util.List;

public interface OfflineAreasListView {

    void showLoading();

    void hideLoading();

    void displayNoOfflineMaps();

    void showOfflineRegions(List<DomainOfflineArea> transform, long selectedRegionId);

    void showRenameError();

    void showDeleteError();

    void showSelectError();
}
