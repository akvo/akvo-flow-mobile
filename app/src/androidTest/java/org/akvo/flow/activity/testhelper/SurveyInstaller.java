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

package org.akvo.flow.activity.testhelper;

import android.content.Context;
import android.util.Log;

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
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Queue;

public class SurveyInstaller {

    private static final String TAG = "SurveyInstaller";
    private SurveyDbDataSource adapter;
    //Need an array that holds every File so we can delete them in the end
    private Queue<File> surveyFiles = new ArrayDeque<>();

    public SurveyInstaller(Context context) {
       this.adapter = new SurveyDbDataSource(context, null);
    }

    public Survey installSurvey(int resId, Context context) {
        InputStream input = context.getResources()
                .openRawResource(resId);
        Survey survey = null;
        try {
            survey = persistSurvey(FileUtil.readText(input));
        } catch (IOException e) {
            Log.e(TAG, "Error installing survey");
        }
        return survey;
    }

    /**
     * Creates a survey object out of an XML string and persists the .xml file in the surveys/
     * directory of the phone
     *
     * @param xml of the survey
     * @return survey
     * @throws IOException if string cannot be written to file
     */
    public Survey persistSurvey(String xml) throws IOException {
        Survey survey = parseSurvey(xml);
        File surveyFile = new File(FileUtil.getFilesDir(FileUtil.FileType.FORMS),
                survey.getId() + ConstantUtil.XML_SUFFIX);
        writeString(surveyFile, xml);

        surveyFiles.add(surveyFile);

        survey.setFileName(survey.getId() + ConstantUtil.XML_SUFFIX);
        survey.setType("Survey");
        survey.setLocation(ConstantUtil.FILE_LOCATION);
        survey.setHelpDownloaded(true);

        saveSurvey(survey);
        return survey;
    }

    private void writeString(File file, String data) throws IOException {
        Writer writer = new FileWriter(file);
        writer.write(data);
        writer.close();
    }

    private Survey parseSurvey(String xml) {
        SurveyMetadataParser surveyMetaData = new SurveyMetadataParser();
        ByteArrayInputStream metaInputStream = new ByteArrayInputStream(xml.getBytes());

        ByteArrayInputStream inputStream = new ByteArrayInputStream(xml.getBytes());

        SaxSurveyParser parser = new SaxSurveyParser();
        SurveyMetadata surveyMeta = surveyMetaData.parse(metaInputStream);
        Survey survey = parser.parse(inputStream);

        survey.setId(surveyMeta.getId());

        return survey;
    }

    private void saveSurvey(Survey survey) {
        adapter.open();
        adapter.saveSurvey(survey);
        adapter.addSurveyGroup(survey.getSurveyGroup());
        adapter.close();
    }

    public void clearSurveys() {
        for (File file : surveyFiles) {
            file.delete();
        }
        adapter.open();
        adapter.deleteAllSurveys();
        adapter.clearCollectedData();
        adapter.close();
    }

    public void deleteResponses(String surveyInstanceId) {
        adapter.open();
        adapter.deleteResponses(surveyInstanceId);
        adapter.close();
    }
}
