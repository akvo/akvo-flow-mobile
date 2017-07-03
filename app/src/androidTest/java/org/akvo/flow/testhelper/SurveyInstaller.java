/*
 *  Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.testhelper;

import android.content.Context;

import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyMetadata;
import org.akvo.flow.serialization.form.SaxSurveyParser;
import org.akvo.flow.serialization.form.SurveyMetadataParser;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class SurveyInstaller {

    private SurveyDbDataSource adapter;
    private Context context;

    public SurveyInstaller(Context context) {
        this.context = context;
        adapter = new SurveyDbDataSource(context);
    }

    /**
     * Creates a survey object out of an XML string and persists the .xml file in the DATA_DIR of the phone
     *
     * @param xml of the survey (from akvosandbox)
     * @return survey
     * @throws IOException
     */
    public Survey persistSurvey(String xml) throws IOException {
        SurveyMetadataParser surveyMetaData = new SurveyMetadataParser();
        ByteArrayInputStream metaInputStream = new ByteArrayInputStream(xml.getBytes());

        ByteArrayInputStream inputStream = new ByteArrayInputStream(xml.getBytes());

        SaxSurveyParser parser = new SaxSurveyParser();
        SurveyMetadata surveyMeta = surveyMetaData.parse(metaInputStream);
        Survey survey = parser.parse(inputStream);

        survey.setId(surveyMeta.getId());

        File surveyFile = new File(FileUtil.getFilesDir(FileUtil.FileType.FORMS), survey.getId() + ConstantUtil.XML_SUFFIX);
        Writer writer = new FileWriter(surveyFile);
        writer.write(xml);
        writer.close();

        survey.setFileName(survey.getId() + ConstantUtil.XML_SUFFIX);
        survey.setType("Survey");
        survey.setLocation(ConstantUtil.FILE_LOCATION);
        survey.setHelpDownloaded(true);

        adapter.open();
        adapter.saveSurvey(survey);
        adapter.addSurveyGroup(survey.getSurveyGroup());
        adapter.close();

        return survey;
    }
}
