/*
 *  Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.GsonBuilder;
import com.squareup.sqlbrite2.BriteDatabase;
import com.squareup.sqlbrite2.SqlBrite;

import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.data.database.cascade.CascadeDB;
import org.akvo.flow.database.DatabaseHelper;
import org.akvo.flow.database.tables.DataPointDownloadTable;
import org.akvo.flow.database.tables.FormUpdateNotifiedTable;
import org.akvo.flow.database.tables.LanguageTable;
import org.akvo.flow.database.tables.QuestionGroupTable;
import org.akvo.flow.domain.Node;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.entity.User;
import org.akvo.flow.domain.util.GsonMapper;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.files.FileBrowser;
import org.akvo.flow.util.files.FormFileBrowser;
import org.akvo.flow.util.files.FormResourcesFileBrowser;
import org.akvo.flow.utils.FileHelper;
import org.akvo.flow.utils.XmlFormParser;
import org.akvo.flow.utils.entity.Form;
import org.akvo.flow.utils.entity.Question;
import org.akvo.flow.utils.entity.QuestionGroup;
import org.akvo.flow.utils.entity.SurveyGroup;
import org.akvo.flow.utils.entity.SurveyMetadata;

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

public class TestSurveyInstaller {

    private static final String TAG = "TestSurveyInstaller";
    private final SurveyDbDataSource adapter;
    //Need an array that holds every File so we can delete them in the end
    private final Queue<File> surveyFiles = new ArrayDeque<>();

    public TestSurveyInstaller(Context context) {
        SqlBrite sqlBrite = new SqlBrite.Builder().build();
        DatabaseHelper databaseHelper = new DatabaseHelper(context, new LanguageTable(),
                new DataPointDownloadTable(), new FormUpdateNotifiedTable(), new QuestionGroupTable());
        BriteDatabase db = sqlBrite
                .wrapDatabaseHelper(databaseHelper, AndroidSchedulers.mainThread());
        this.adapter = new SurveyDbDataSource(db, databaseHelper);
    }

    public Pair<Form, SurveyGroup> installSurvey(int resId, Context context) {
        InputStream input = context.getResources()
                .openRawResource(resId);
        Pair<Form, SurveyGroup> survey = null;
        try {
            survey = persistSurvey(input);
            installCascades(survey.first, context);
        } catch (IOException e) {
            Log.e(TAG, "Error installing survey");
        }
        return survey;
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
            for (QuestionResponse.QuestionResponseBuilder responseBuilder : responseBuilders) {
                QuestionResponse responseToSave = responseBuilder
                        .setSurveyInstanceId(surveyInstanceId)
                        .createQuestionResponse();
                questionResponseMap.put(responseToSave.responseMapKey(), responseToSave);
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
            GsonMapper mapper = new GsonMapper(new GsonBuilder().create());
            TestDataPoint dataPoint = mapper.read(jsonDataString, TestDataPoint.class);
            List<TestResponse> responses = dataPoint.getResponses();
            List<QuestionResponse.QuestionResponseBuilder> builders = new ArrayList<>(
                    responses.size());
            for (TestResponse response : responses) {
                QuestionResponse.QuestionResponseBuilder questionResponse =
                        generateResponse(response.getValue(), response.getAnswerType(),
                                response.getQuestionId(), response.getIteration());
                builders.add(questionResponse);
            }
            return createDataPoint(surveyGroup, builders
                    .toArray(new QuestionResponse.QuestionResponseBuilder[builders.size()]));
        } catch (IOException e) {
            Timber.e(e);
        }
        return null;
    }

    @NonNull
    public static QuestionResponse.QuestionResponseBuilder[] generateRepeatedTwoGroupsResponseData() {
        return new QuestionResponse.QuestionResponseBuilder[] {
                generateResponse("123456", ConstantUtil.VALUE_RESPONSE_TYPE, "205929117", 0),
                generateResponse("test1", ConstantUtil.VALUE_RESPONSE_TYPE, "205929118", 0),
                generateResponse("test2", ConstantUtil.VALUE_RESPONSE_TYPE, "205929118", 1),
                generateResponse("test3", ConstantUtil.VALUE_RESPONSE_TYPE, "205929118", 2)
        };
    }

    @NonNull
    public static QuestionResponse.QuestionResponseBuilder[] generateRepeatedOneGroupResponseData() {
        return new QuestionResponse.QuestionResponseBuilder[] {
                generateResponse("test1", ConstantUtil.VALUE_RESPONSE_TYPE, "205929118", 0),
                generateResponse("test2", ConstantUtil.VALUE_RESPONSE_TYPE, "205929118", 1),
                generateResponse("test3", ConstantUtil.VALUE_RESPONSE_TYPE, "205929118", 2)
        };
    }

    @NonNull
    public static QuestionResponse.QuestionResponseBuilder[] generatePartialRepeatedGroupResponseData() {
        return new QuestionResponse.QuestionResponseBuilder[] {
                generateResponse("text-response-rep-one", ConstantUtil.VALUE_RESPONSE_TYPE, "205929118", 0),
                generateResponse("1234567", ConstantUtil.VALUE_RESPONSE_TYPE, "205929119", 0),
                generateResponse("text-response-rep-two", ConstantUtil.VALUE_RESPONSE_TYPE, "205929118", 1),
        };
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

    public void deleteResponses() {
        adapter.open();
        adapter.deleteAllResponses();
        adapter.close();
    }

    public SparseArray<List<Node>> getAllNodes(Question question, Context context) {
        String cascadeFileName = question.getCascadeResource();
        FormResourcesFileBrowser formResourcesFileUtil = new FormResourcesFileBrowser(
                new FileBrowser());
        File cascadeFolder = formResourcesFileUtil
                .getExistingAppInternalFolder(
                        InstrumentationRegistry.getInstrumentation().getTargetContext());
        if (!TextUtils.isEmpty(cascadeFileName)) {
            File db = new File(cascadeFolder, cascadeFileName);
            if (db.exists()) {
                CascadeDB cascadeDB = new CascadeDB(context, db.getAbsolutePath());
                cascadeDB.open();
                SparseArray<List<Node>> values = cascadeDB.getValues();
                cascadeDB.close();
                return values;
            }
        }
        return new SparseArray<>(0);
    }

    private void installCascades(Form survey, Context context) throws IOException {
        FormResourcesFileBrowser formResourcesFileUtil = new FormResourcesFileBrowser(
                new FileBrowser());
        File cascadeFolder = formResourcesFileUtil
                .getExistingAppInternalFolder(
                        InstrumentationRegistry.getInstrumentation().getTargetContext());
        for (QuestionGroup group : survey.getGroups()) {
            for (Question question : group.getQuestions()) {
                String cascadeFileName = question.getCascadeResource();
                if (!TextUtils.isEmpty(cascadeFileName)) {
                    String cascadeResourceName = cascadeFileName.replace(".sqlite", "");
                    cascadeResourceName = cascadeResourceName.replaceAll("-", "_");
                    int cascadeResId = context.getResources()
                            .getIdentifier(cascadeResourceName, "raw", context.getPackageName());
                    FileOutputStream output = new FileOutputStream(
                            new File(cascadeFolder, cascadeFileName));
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
     * @return survey
     * @throws IOException if string cannot be written to file
     */
    private Pair<Form, SurveyGroup> persistSurvey(InputStream input) throws IOException {
        String xml = FileUtil.readText(input);
        Pair<Form, SurveyMetadata> result = parseSurvey(input);
        Form form = result.first;
        SurveyGroup group = result.second.getSurveyGroup();
        FormFileBrowser formFileBrowser = new FormFileBrowser(new FileBrowser());
        File surveyFile = new File(
                formFileBrowser.getExistingAppInternalFolder(
                        InstrumentationRegistry.getInstrumentation().getTargetContext()),
                form.getId() + ConstantUtil.XML_SUFFIX);
        writeString(surveyFile, xml);

        surveyFiles.add(surveyFile);
        form.setFilename(form.getId() + ConstantUtil.XML_SUFFIX);
        form.setType("Survey");
        form.setLocation(ConstantUtil.FILE_LOCATION);
        form.setCascadeDownloaded(true);

        saveSurvey(form, group);
        return new Pair<>(form, group);
    }

    private void writeString(File file, String data) throws IOException {
        Writer writer = new FileWriter(file);
        writer.write(data);
        writer.close();
    }

    private Pair<Form, SurveyMetadata> parseSurvey(InputStream input) {
        XmlFormParser parser = new XmlFormParser(new FileHelper());
        Pair<Form, SurveyMetadata> result = parser.parseXmlFormWithMeta(input);
        return result;
    }

    private void saveSurvey(Form form, SurveyGroup group) {
        adapter.open();
        adapter.saveSurvey(form);
        adapter.addSurveyGroup(group);
        adapter.addQuestionGroups(form);
        //TODO: insert questions when also in db
        adapter.close();
    }

    private static QuestionResponse.QuestionResponseBuilder generateResponse(String value,
            String valueResponseType, String questionId, int iteration) {
        return new QuestionResponse.QuestionResponseBuilder()
                .setValue(value)
                .setType(valueResponseType)
                .setQuestionId(questionId)
                .setIteration(iteration);
    }
}
