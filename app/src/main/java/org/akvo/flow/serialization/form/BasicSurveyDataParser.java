/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.serialization.form;

import org.akvo.flow.domain.BasicSurveyData;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class BasicSurveyDataParser {

    public BasicSurveyData parse(InputStream inputStream) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            BasicSurveyDataHandler handler = new BasicSurveyDataHandler();
            Reader reader = new InputStreamReader(inputStream, "UTF-8");
            InputSource source = new InputSource(reader);
            source.setEncoding("UTF-8");
            parser.parse(source, handler);
            return handler.getSurvey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
