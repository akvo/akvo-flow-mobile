/*
 *  Copyright (C) 2010-2015 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * domain object for Surveys
 * 
 * @author Christopher Fagiani
 */
public class Survey {
    private SurveyGroup surveyGroup;// TODO: Use java mMemberName convention
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
}
