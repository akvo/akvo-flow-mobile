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

package org.akvo.flow.presentation.geoshape.properties;

import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.presentation.geoshape.entities.AreaCounter;
import org.akvo.flow.presentation.geoshape.entities.AreaShape;
import org.akvo.flow.presentation.geoshape.entities.LengthCounter;
import org.akvo.flow.presentation.geoshape.entities.LineShape;
import org.akvo.flow.presentation.geoshape.entities.Shape;

import javax.inject.Inject;

public class PropertiesPresenter implements Presenter {

    private final LengthCounter lengthCounter;
    private final AreaCounter areaCounter;
    private PropertiesView view;

    @Inject
    public PropertiesPresenter(LengthCounter lengthCounter, AreaCounter areaCounter) {
        this.lengthCounter = lengthCounter;
        this.areaCounter = areaCounter;
    }

    @Override
    public void destroy() {
        // EMPTY
    }

    public void setView(PropertiesView view) {
        this.view = view;
    }

    public void countProperties(Shape shape) {
        String count = shape.getPoints().size() +"";
        String length = "";
        String area = "";
        if (shape instanceof LineShape || shape instanceof AreaShape) {
            length = lengthCounter.computeLength(shape.getPoints()) + "";
        }

        if (shape instanceof AreaShape) {
            area = areaCounter.computeArea(shape.getPoints()) + "";
        }

        view.displayShapeCount(count, length, area);
    }
}
