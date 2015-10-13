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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.akvo.flow.util.ConstantUtil;

/**
 * data structure for individual survey questions. Questions have a type which
 * can be any one of:
 * <ul>
 * <li>option - radio-button like selection</li>
 * <li>free - free text</li>
 * <li>video - video capture</li>
 * <li>photo - photo capture</li>
 * <li>geo - geographic detection (GPS)</li>
 * </ul>
 * 
 * @author Christopher Fagiani
 */
public class Question {
    private String id;
    private String text;
    private int order;
    private ValidationRule validationRule;
    private String renderType;
    private List<QuestionHelp> questionHelp;
    private boolean mandatory;
    private String type;
    private List<Option> options;
    private boolean allowOther;
    private boolean allowMultiple;
    private boolean locked;
    private Map<String, AltText> altTextMap = new HashMap<String, AltText>();
    private List<Dependency> dependencies;
    private List<ScoringRule> scoringRules;
    private boolean useStrength;
    private int strengthMin;
    private int strengthMax;
    private boolean localeName = false;
    private boolean localeLocation = false;
    private String sourceQuestionId;// "Copied-from" question Id
    private boolean isDoubleEntry;
    private boolean useExternalSource;
    private boolean allowPoints, allowLine, allowPolygon;

    // cascading question specific attrs
    private String src;
    private List<Level> levels;

    public void setIsLocaleName(boolean localeName) {
        this.localeName = localeName;
    }
    
    public void setIsLocaleLocation(boolean localeLocation) {
        this.localeLocation = localeLocation;
    }
    
    public boolean isLocaleName() {
        return localeName;
    }
    
    public boolean isLocaleLocation() {
        return localeLocation;
    }

    public void setUseStrength(boolean val) {
        useStrength = val;
    }

    public boolean useStrength() {
        return useStrength;
    }

    public int getStrengthMin() {
        return strengthMin;
    }

    public void setStrengthMin(int strengthMin) {
        this.strengthMin = strengthMin;
    }

    public int getStrengthMax() {
        return strengthMax;
    }

    public void setStrengthMax(int strengthMax) {
        this.strengthMax = strengthMax;
    }

    public List<QuestionHelp> getQuestionHelp() {
        return questionHelp;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Map<String, AltText> getAltTextMap() {
        return altTextMap;
    }

    public AltText getAltText(String lang) {
        return altTextMap.get(lang);
    }

    public void addAltText(AltText altText) {
        altTextMap.put(altText.getLanguage(), altText);
    }

    public boolean isAllowMultiple() {
        return allowMultiple;
    }

    public void setAllowMultiple(boolean allowMultiple) {
        this.allowMultiple = allowMultiple;
    }

    public String getRenderType() {
        return renderType;
    }

    public void setRenderType(String renderType) {
        this.renderType = renderType;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(ArrayList<Option> options) {
        this.options = options;
    }

    public boolean isAllowOther() {
        return allowOther;
    }

    public void setAllowOther(boolean allowOther) {
        this.allowOther = allowOther;
    }

    public void addDependency(Dependency dep) {
        if (dependencies == null) {
            dependencies = new ArrayList<Dependency>();
        }
        dependencies.add(dep);
    }

    public List<QuestionHelp> getHelpByType(String type) {
        List<QuestionHelp> help = new ArrayList<QuestionHelp>();
        if (questionHelp != null && type != null) {
            for (int i = 0; i < questionHelp.size(); i++) {
                if (type.equalsIgnoreCase(questionHelp.get(i).getType())) {
                    help.add(questionHelp.get(i));
                }
            }
        }
        return help;
    }

    public void addQuestionHelp(QuestionHelp help) {
        if (questionHelp == null) {
            questionHelp = new ArrayList<QuestionHelp>();
        }
        questionHelp.add(help);
    }

    public ValidationRule getValidationRule() {
        return validationRule;
    }
    
    public void setSourceQuestionId(String sourceQuestionId) {
        this.sourceQuestionId = sourceQuestionId;
    }
    
    public String getSourceQuestionId() {
        return sourceQuestionId;
    }

    public void setValidationRule(ValidationRule validationRule) {
        this.validationRule = validationRule;
    }

    /**
     * counts the number of non-empty help tip types
     * 
     * @return
     */
    public int getHelpTypeCount() {
        int count = 0;
        if (getHelpByType(ConstantUtil.IMAGE_HELP_TYPE).size() > 0) {
            count++;
        }
        if (getHelpByType(ConstantUtil.TIP_HELP_TYPE).size() > 0) {
            count++;
        }
        if (getHelpByType(ConstantUtil.VIDEO_HELP_TYPE).size() > 0) {
            count++;
        }
        return count;
    }

    public void addScoringRule(ScoringRule rule) {
        if (scoringRules == null) {
            scoringRules = new ArrayList<ScoringRule>();
        }
        scoringRules.add(rule);
    }

    public List<ScoringRule> getScoringRules() {
        return scoringRules;
    }

    /**
     * scores a response according to the question's scoring rules. If there are
     * no rules or none of the rules match, this method will return null
     * otherwise it will return the scored value
     * 
     * @param response
     * @return
     */
    public String getResponseScore(String response) {
        String result = null;
        if (scoringRules != null) {
            int i = 0;
            while (i < scoringRules.size() && result == null) {
                result = scoringRules.get(i++).scoreResponse(response);
            }
        }
        return result;
    }

    public String toString() {
        return text;
    }

    public void setIsDoubleEntry(boolean isdoubleEntry) {
        this.isDoubleEntry = isdoubleEntry;
    }

    public boolean isDoubleEntry() {
        return isDoubleEntry;
    }

    public void useExternalSource(boolean useExternalSource) {
        this.useExternalSource = useExternalSource;
    }

    public boolean useExternalSource() {
        return useExternalSource;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getSrc() {
        return src;
    }

    public List<Level> getLevels() {
        return levels;
    }

    public void setLevels(List<Level> levels) {
        this.levels = levels;
    }

    public void setAllowPoints(boolean allowPoints) {
        this.allowPoints = allowPoints;
    }

    public boolean isAllowPoints() {
        return allowPoints;
    }

    public void setAllowLine(boolean allowLine) {
        this.allowLine = allowLine;
    }

    public boolean isAllowLine() {
        return allowLine;
    }

    public void setAllowPolygon(boolean allowPolygon) {
        this.allowPolygon = allowPolygon;
    }

    public boolean isAllowPolygon() {
        return allowPolygon;
    }

    /**
     * Clone a question and update the question ID. This is only relevant for repeat-question-groups,
     * which require different instances of the question for each iteration.
     * Note: Excluding dependencies, all non-primitive variables are *not* deep-copied.
     */
    public static Question copy(Question question, String questionId) {
        Question q = new Question();
        q.id = questionId;
        q.text = question.getText();
        q.order = question.getOrder();
        q.renderType = question.getRenderType();
        q.mandatory = question.isMandatory();
        q.type = question.getType();
        q.allowOther = question.isAllowOther();
        q.allowMultiple = question.isAllowMultiple();
        q.locked = question.isLocked();
        q.useStrength = question.useStrength();
        q.strengthMin = question.getStrengthMin();
        q.strengthMax = question.getStrengthMax();
        q.localeName = question.isLocaleName();
        q.localeLocation = question.isLocaleLocation();
        q.sourceQuestionId = question.getSourceQuestionId();
        q.isDoubleEntry = question.isDoubleEntry();
        q.useExternalSource = question.useExternalSource();
        q.allowPoints = question.isAllowPoints();
        q.allowLine = question.isAllowLine();
        q.allowPolygon = question.isAllowPolygon();
        q.src = question.getSrc();
        q.validationRule = question.getValidationRule();// Shallow copy
        q.options = question.getOptions();// Shallow copy
        q.questionHelp = question.getQuestionHelp();// Shallow copy
        q.altTextMap = question.getAltTextMap();// Shallow copy
        q.scoringRules = question.getScoringRules();// Shallow copy
        q.levels = question.getLevels();// Shallow copy

        // Deep-copy dependencies
        if (question.dependencies != null) {
            q.dependencies = new ArrayList<>();
            for (Dependency d : question.getDependencies()) {
                q.dependencies.add(new Dependency(d));
            }
        }
        return q;
    }
}
