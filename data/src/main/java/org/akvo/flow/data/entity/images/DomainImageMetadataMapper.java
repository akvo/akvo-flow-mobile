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

package org.akvo.flow.data.entity.images;

import org.akvo.flow.domain.entity.DomainImageMetadata;

import javax.inject.Inject;

public class DomainImageMetadataMapper {

    private final DomainImageLocationMapper mapper;

    @Inject
    public DomainImageMetadataMapper(DomainImageLocationMapper mapper) {
        this.mapper = mapper;
    }

    public DomainImageMetadata transform(DataImageLocation location, String resizedImagePath) {
        return new DomainImageMetadata(resizedImagePath, mapper.transform(location));
    }
}
