/*
 *  Copyright (C) 2013 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package com.gallatinsystems.survey.device.api.parser;

import java.io.InputStream;
import java.util.List;

public interface FlowParser<E> {
    /**
     * Parse a single element of type <E>
     * @param inputStream the stream containing the element
     * @return parsed value
     */
    public E parse(InputStream inputStream);
    
    /**
     * Parse a single element of type <E>
     * @param response the String response
     * @return parsed value
     */
    public E parse(String response);
    
    /**
     * Parse a list of elements of type <E>
     * @param response the String response
     * @return parsed List of values
     */
    public List<E> parseList(String response);
}
