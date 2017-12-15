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

package org.akvo.flow.activity.form.data;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.sqlbrite2.BriteDatabase;
import com.squareup.sqlbrite2.SqlBrite;

import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.data.migration.FlowMigrationListener;
import org.akvo.flow.data.migration.languages.MigrationLanguageMapper;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.database.DatabaseHelper;
import org.akvo.flow.database.LanguageTable;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyMetadata;
import org.akvo.flow.domain.User;
import org.akvo.flow.serialization.form.SaxSurveyParser;
import org.akvo.flow.serialization.form.SurveyMetadataParser;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.FormFileUtil;
import org.akvo.flow.util.GsonMapper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import io.reactivex.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class SurveyInstaller {

    private static final String TAG = "SurveyInstaller";
    private SurveyDbDataSource adapter;
    //Need an array that holds every File so we can delete them in the end
    private Queue<File> surveyFiles = new ArrayDeque<>();

    public SurveyInstaller(Context context) {
        SqlBrite sqlBrite = new SqlBrite.Builder().build();
        DatabaseHelper databaseHelper = new DatabaseHelper(context, new LanguageTable(),
                new FlowMigrationListener(new Prefs(context),
                        new MigrationLanguageMapper(context)));
        BriteDatabase db = sqlBrite
                .wrapDatabaseHelper(databaseHelper, AndroidSchedulers.mainThread());
        this.adapter = new SurveyDbDataSource(context, db);
    }

    public Survey installSurvey(int resId, Context context) {
        InputStream input = context.getResources()
                .openRawResource(resId);
        Survey survey = null;
        try {
            survey = persistSurvey(FileUtil.readText(input));
            installCascades(survey, context);
        } catch (IOException e) {
            Log.e(TAG, "Error installing survey");
        }
        return survey;
    }

    private void installCascades(Survey survey, Context context) throws IOException {
        for (QuestionGroup group : survey.getQuestionGroups()) {
            for (Question question : group.getQuestions()) {
                String cascadeFileName = question.getSrc();
                if (!TextUtils.isEmpty(cascadeFileName)) {
                    String cascadeResourceName = cascadeFileName.replace(".sqlite", "");
                    cascadeResourceName = cascadeResourceName.replaceAll("-", "_");
                    int cascadeResId = context.getResources()
                            .getIdentifier(cascadeResourceName, "raw", context.getPackageName());
                    FileOutputStream output = new FileOutputStream(
                            new File(FileUtil.getFilesDir(FileUtil.FileType.RES), cascadeFileName));
                    InputStream input = context.getResources().openRawResource(cascadeResId);
                    FileUtil.copy(input, output);
                }
            }
        }
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
        FormFileUtil formFileUtil = new FormFileUtil();
        File surveyFile = new File(
                formFileUtil.getFormsFolder(InstrumentationRegistry.getTargetContext()),
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

    public Pair<Long, Map<String, QuestionResponse>> createDataPoint(SurveyGroup surveyGroup,
            QuestionResponse.QuestionResponseBuilder... responseBuilders) {
        adapter.open();
        Survey registrationForm = adapter.getRegistrationForm(surveyGroup);
        String surveyedLocaleId = adapter.createSurveyedLocale(surveyGroup.getId());
        User user = new User(1L, "User");
        long surveyInstanceId = adapter
                .createSurveyRespondent(registrationForm.getId(), registrationForm.getVersion(),
                        user, surveyedLocaleId);
        Map<String, QuestionResponse> questionResponseMap = new HashMap<>();
        if (responseBuilders != null) {
            int length = responseBuilders.length;
            for (int i = 0; i < length; i++) {
                QuestionResponse responseToSave = responseBuilders[i]
                        .setSurveyInstanceId(surveyInstanceId)
                        .createQuestionResponse();
                questionResponseMap.put(responseToSave.getResponseKey(), responseToSave);
                adapter.createOrUpdateSurveyResponse(responseToSave);
            }
        }
        adapter.close();
        return new Pair<>(surveyInstanceId, questionResponseMap);
    }

    public Pair<Long, Map<String, QuestionResponse>> createDataPointFromFile(
            SurveyGroup surveyGroup, Context context, int resId) {

        InputStream input = context.getResources()
                .openRawResource(resId);
        try {
            String jsonDataString = FileUtil.readText(input);
            GsonMapper mapper = new GsonMapper();
            TestDataPoint dataPoint = mapper.read(jsonDataString, TestDataPoint.class);
            List<TestResponse> responses = dataPoint.getResponses();
            List<QuestionResponse.QuestionResponseBuilder> builders = new ArrayList<>(
                    responses.size());
            for (TestResponse response : responses) {
                QuestionResponse.QuestionResponseBuilder questionResponse =
                        new QuestionResponse.QuestionResponseBuilder()
                        .setValue(response.getValue())
                        .setType(response.getAnswerType())
                        .setQuestionId(response.getQuestionId())
                        .setIteration(response.getIteration());
                builders.add(questionResponse);
            }
            return createDataPoint(surveyGroup, builders
                    .toArray(new QuestionResponse.QuestionResponseBuilder[builders.size()]));
        } catch (IOException e) {
            Timber.e(e);
        }
        return null;
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
