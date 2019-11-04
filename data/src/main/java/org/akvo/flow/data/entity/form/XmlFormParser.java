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

package org.akvo.flow.data.entity.form;

import android.text.TextUtils;

import org.akvo.flow.data.util.FileHelper;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.annotations.NonNull;
import timber.log.Timber;

public class XmlFormParser {

    private static final String QUESTION = "question";
    private static final String SURVEY = "survey";
    private static final String CASCADE_RESOURCE = "cascadeResource";
    private static final String VERSION = "version";
    private final FileHelper helper;

    @Inject
    public XmlFormParser(FileHelper helper) {
        this.helper = helper;
    }

    @NonNull
    public Form parse(InputStream input) {
        List<String> resources = new ArrayList<>();
        String version = "0.0";

        XmlPullParserFactory parserFactory;
        try {
            parserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserFactory.newPullParser();

            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(input, null);

            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                String eltName;

                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        eltName = parser.getName();
                        if (SURVEY.equals(eltName)) {
                            version = parser.getAttributeValue(null, VERSION);
                        } else if (QUESTION.equals(eltName)) {
                            String resource = parser.getAttributeValue(null, CASCADE_RESOURCE);
                            if (!TextUtils.isEmpty(resource)) {
                                resources.add(resource);
                            }
                        }
                        break;
                    default:
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Timber.e(e);
        } finally {
          helper.close(input);
        }
        return new Form(resources, version);
    }
}
