/*
 *  Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain;

import android.text.TextUtils;

import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * domain object for Surveys
 * 
 * @author Christopher Fagiani
 */
public class Survey {
    private SurveyGroup surveyGroup;
    private String name;
    private String id;
    private Date startDate;
    private Date endDate;
    private List<QuestionGroup> questionGroups;
    private double version;
    private String type;
    private String location;
    private String fileName;
    private boolean helpDownloaded;

    /**
     * Main language code
     */
    private String language;
    private String sourceSurveyId;// "Copied-from" survey Id
    private String app;// FLOW instance ID

    public Survey() {
        questionGroups = new ArrayList<>();
    }
    
    public void setSurveyGroup(SurveyGroup surveyGroup) {
        this.surveyGroup = surveyGroup;
    }
    
    public SurveyGroup getSurveyGroup() {
        return surveyGroup;
    }

    /**
     * Default survey language code
     * @return a string with the language code for example 'en'
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Default survey language code
     * @return a string with the language code for example 'en'
     */
    public String getDefaultLanguageCode() {
        return TextUtils.isEmpty(language) ? ConstantUtil.ENGLISH_CODE : language.toLowerCase();
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isHelpDownloaded() {
        return helpDownloaded;
    }

    public void setHelpDownloaded(boolean helpDownloaded) {
        this.helpDownloaded = helpDownloaded;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public double getVersion() {
        return version;
    }

    public void setVersion(double version) {
        this.version = version;
    }

    public String getName() {
        return name;

    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public List<QuestionGroup> getQuestionGroups() {
        return questionGroups;
    }

    public void setQuestionGroups(List<QuestionGroup> questionGroups) {
        this.questionGroups = questionGroups;
    }

    public void setSourceSurveyId(String sourceSurveyId) {
        this.sourceSurveyId = sourceSurveyId;
    }

    public String getSourceSurveyId() {
        return this.sourceSurveyId;
    }

    public void setApp(String app) {
        this.app = app;
    }
    
    public String getApp() {
        return this.app;
    }

    /**
     * adds a new quesitonGroup to the survey at the end of the questionGroup
     * list. If the questionGroup list is null, it is initialized before adding.
     * 
     * @param group
     */
    public void addQuestionGroup(QuestionGroup group) {
        questionGroups.add(group);
    }
    
    public List<String> getLocaleNameQuestions() {
        List<String> localeNameQuestions = new ArrayList<>();
        if (questionGroups != null) {
            for (QuestionGroup group : questionGroups) {
                localeNameQuestions.addAll(group.getLocaleNameQuestions());
            }
        }
        
        return localeNameQuestions;
    }
    
    public String getLocaleGeoQuestion() {
        if (questionGroups != null) {
            for (QuestionGroup group : questionGroups) {
                String localeGeoQuestion = group.getLocaleGeoQuestion();
                // Just return the first occurrence
                if (localeGeoQuestion != null) {
                    return localeGeoQuestion;
                }
            }
        }
        return null;
    }

    public Set<String> getAvailableLanguageCodes() {
        Set<String> languageCodes = new LinkedHashSet<>();
        languageCodes.add(getDefaultLanguageCode());
        if (questionGroups != null) {
            int size = questionGroups.size();
            for (int i = 0; i < size; i++) {
                ArrayList<Question> questions = questionGroups.get(i).getQuestions();
                if (questions != null) {
                    for (Question question : questions) {
                        Map<String, AltText> questionAltTextMap = question
                                .getLanguageTranslationMap();
                        if (questionAltTextMap != null) {
                            languageCodes.addAll(questionAltTextMap.keySet());
                        }
                    }
                }
            }
        }
        return languageCodes;
    }
}
