/*
 *  Copyright (C) 2010-2019 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.utils.entity.AltText;
import org.akvo.flow.utils.entity.Dependency;
import org.akvo.flow.utils.entity.Level;
import org.akvo.flow.utils.entity.Option;
import org.akvo.flow.utils.entity.QuestionHelp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Stores all available translations for each survey
     * Key: the language code, ej: en
     * Value: text of the question in the language represented by the key
     */
    private Map<String, AltText> languageTranslationMap = new HashMap<>();
    private List<Dependency> dependencies;
    private boolean useStrength;
    private int strengthMin;
    private int strengthMax;
    private boolean localeName = false;
    private boolean localeLocation = false;
    private String sourceQuestionId; // "Copied-from" question Id
    private boolean isDoubleEntry;
    private boolean allowPoints, allowLine, allowPolygon;
    private String caddisflyRes;

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

    public Map<String, AltText> getLanguageTranslationMap() {
        return languageTranslationMap;
    }

    public AltText getAltText(String lang) {
        return languageTranslationMap.get(lang);
    }

    public void addAltText(AltText altText) {
        languageTranslationMap.put(altText.getLanguageCode(), altText);
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
            dependencies = new ArrayList<>();
        }
        dependencies.add(dep);
    }

    public List<QuestionHelp> getHelpByType(String type) {
        List<QuestionHelp> help = new ArrayList<>();
        if (questionHelp != null && type != null) {
            for (int i = 0; i < questionHelp.size(); i++) {
                help.add(questionHelp.get(i));
            }
        }
        return help;
    }

    public void addQuestionHelp(QuestionHelp help) {
        if (questionHelp == null) {
            questionHelp = new ArrayList<>();
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
     */
    public int getHelpTypeCount() {
        int count = 0;
        if (getHelpByType(ConstantUtil.TIP_HELP_TYPE).size() > 0) {
            count++;
        }
        return count;
    }

    public String toString() {
        return text;
    }

    public void setIsDoubleEntry(boolean isDoubleEntry) {
        this.isDoubleEntry = isDoubleEntry;
    }

    public boolean isDoubleEntry() {
        return isDoubleEntry;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getSrc() {
        return src;
    }

    public void setCaddisflyRes(String res) {
        this.caddisflyRes = res;
    }

    public String getCaddisflyRes() {
        return caddisflyRes;
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
        q.allowPoints = question.isAllowPoints();
        q.allowLine = question.isAllowLine();
        q.allowPolygon = question.isAllowPolygon();
        q.src = question.getSrc();
        q.validationRule = question.getValidationRule();
        q.questionHelp = question.getQuestionHelp();
        q.languageTranslationMap = question.getLanguageTranslationMap();
        q.levels = question.getLevels();
        q.caddisflyRes = question.getCaddisflyRes();

        // Deep-copy dependencies
        if (question.dependencies != null) {
            q.dependencies = new ArrayList<>();
            for (Dependency d : question.getDependencies()) {
                q.dependencies.add(new Dependency(d.getQuestion(), d.getAnswer()));
            }
        }

        // Deep-copy options
        if (question.options != null) {
            q.options = new ArrayList<>();
            for (Option o : question.options) {
                q.options.add(new Option(o.getText(), o.getCode(), o.isOther(), o.getAltTextMap()));
            }
        }
        return q;
    }

    public boolean isRepeatable() {
        return id != null && id.contains("|");
    }

    public String getQuestionId() {
        if (isRepeatable()) {
            String[] questionIdAndIteration = id.split("\\|");
            return questionIdAndIteration[0];
        } else {
            return id;
        }
    }

    public int getIteration() {
        if (isRepeatable()) {
            String[] questionIdAndIteration = id.split("\\|");
            String iteration = questionIdAndIteration[1];
            return Integer.parseInt(iteration);
        } else {
            return QuestionResponse.NO_ITERATION;
        }
    }
}
