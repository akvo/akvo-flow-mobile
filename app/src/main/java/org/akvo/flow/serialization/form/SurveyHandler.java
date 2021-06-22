/*
 *  Copyright (C) 2010-2012,2018-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.serialization.form;

import org.akvo.flow.domain.AltText;
import org.akvo.flow.domain.Dependency;
import org.akvo.flow.domain.Level;
import org.akvo.flow.domain.Option;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionHelp;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.ValidationRule;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.StringUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Handler for sax-based xml parser for Survey files
 * 
 * @author Christopher Fagiani
 **/
public class SurveyHandler extends DefaultHandler {
    private static final String DEFAULT_LANG = "defaultLanguageCode";
    private static final String QUESTION_GROUP = "questionGroup";
    private static final String HEADING = "heading";
    private static final String QUESTION = "question";
    private static final String SURVEY = "survey";
    private static final String APP = "app";
    private static final String ORDER = "order";
    private static final String MANDATORY = "mandatory";
    private static final String TYPE = "type";
    private static final String ID = "id";
    private static final String DEPENDENCY = "dependency";
    private static final String ANSWER = "answer-value";
    private static final String TEXT = "text";
    private static final String OPTION = "option";
    private static final String VALUE = "value";
    private static final String CODE = "code";
    private static final String OPTIONS = "options";
    private static final String ALLOW_OTHER = "allowOther";
    private static final String VALIDATION_TYPE = "validationType";
    private static final String VALIDATION_RULE = "validationRule";
    private static final String MAX_LENGTH = "maxLength";
    private static final String ALLOW_DEC = "allowDecimal";
    private static final String ALLOW_SIGN = "signed";
    private static final String RENDER_TYPE = "renderType";
    private static final String ALLOW_MULT = "allowMultiple";
    private static final String MIN_VAL = "minVal";
    private static final String MAX_VAL = "maxVal";
    private static final String ALT_TEXT = "altText";
    private static final String LANG = "language";
    private static final String LOCKED = "locked";
    private static final String HELP = "help";
    private static final String STRENGTH_MIN = "strengthMin";
    private static final String STRENGTH_MAX = "strengthMax";
    private static final String NAME = "name";
    private static final String VERSION = "version";
    private static final String LOCALE_NAME = "localeNameFlag";
    private static final String LOCALE_LOCATION = "localeLocationFlag";
    private static final String SOURCE_QUESTION_ID = "sourceId";
    private static final String SOURCE_SURVEY_ID = "sourceSurveyId";
    private static final String DOUBLE_ENTRY = "requireDoubleEntry";
    private static final String REPEATABLE = "repeatable";

    private static final String SURVEY_GROUP_ID = "surveyGroupId";
    private static final String SURVEY_GROUP_NAME = "surveyGroupName";
    private static final String REGISTRATION_SURVEY = "registrationSurvey";

    private static final String CASCADE_RESOURCE = "cascadeResource";
    private static final String CADDISFLY_RESOURCE = "caddisflyResourceUuid";
    private static final String LEVELS = "levels";
    private static final String LEVEL = "level";

    private static final String ALLOW_POINTS = "allowPoints";
    private static final String ALLOW_LINE = "allowLine";
    private static final String ALLOW_POLYGON = "allowPolygon";

    @SuppressWarnings("unused")
    private static final String TRANSLATION = "translation";

    private Survey survey;
    private QuestionGroup currentQuestionGroup;
    private Question currentQuestion;
    private Option currentOption;
    private ArrayList<Option> currentOptions;
    private ValidationRule currentValidation;
    private AltText currentAltText;
    private QuestionHelp currentHelp;
    private Level currentLevel;
    private List<Level> currentLevels;

    private StringBuilder builder;

    public Survey getSurvey() {
        return survey;
    }

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        super.characters(ch, start, length);
        builder.append(ch, start, length);
    }

    /**
     * processes elements after the end tag is encountered
     */
    public void endElement(String uri, String localName, String name)
            throws SAXException {
        super.endElement(uri, localName, name);
        if (currentQuestionGroup != null) {
            if (localName.equalsIgnoreCase(HEADING)) {
                currentQuestionGroup.setHeading(builder.toString().trim());
            } else if (localName.equalsIgnoreCase(QUESTION)) {
                currentQuestionGroup.addQuestion(currentQuestion);
                currentQuestion = null;
            } else if (localName.equalsIgnoreCase(QUESTION_GROUP)) {
                survey.addQuestionGroup(currentQuestionGroup);
                currentQuestionGroup = null;
            }
        }
        if (currentQuestion != null) {
            // <text> can appear multiple places. We need to make sure we're not
            // in the context of an option or help here
            if (localName.equalsIgnoreCase(TEXT) && currentOption == null
                    && currentHelp == null && currentLevel == null) {
                currentQuestion.setText(builder.toString().trim());
            } else if (localName.equalsIgnoreCase(OPTIONS)) {
                currentQuestion.setOptions(currentOptions);
                currentOptions = null;
            } else if (localName.equalsIgnoreCase(LEVELS)) {
                currentQuestion.setLevels(currentLevels);
                currentLevels = null;
            } else if (localName.equalsIgnoreCase(VALIDATION_RULE)) {
                currentQuestion.setValidationRule(currentValidation);
                currentValidation = null;
            } else if (localName.equalsIgnoreCase(HELP)) {
                if (currentHelp.isValid()) {
                    if (StringUtil.isNullOrEmpty(currentHelp.getType())) {
                        currentHelp.setType(ConstantUtil.TIP_HELP_TYPE);
                    }
                    currentQuestion.addQuestionHelp(currentHelp);
                }
                currentHelp = null;
            }
        }
        if (currentOption != null) {
            // the null check here is to handle "old" style options that don't
            // have a <text> element
            if (localName.equalsIgnoreCase(OPTION)
                    && currentOption.getText() == null) {
                currentOption.setText(builder.toString().trim());
                if (currentOptions != null) {
                    currentOptions.add(currentOption);
                }
                currentOption = null;
            }
            // handle "new" style options that have a <text> element
            if (localName.equalsIgnoreCase(TEXT)) {
                currentOption.setText(builder.toString().trim());
                if (currentOptions != null) {
                    currentOptions.add(currentOption);
                }
            }
            // close the current option
            if (localName.equalsIgnoreCase(OPTION)) {
                currentOption = null;
            }
        }
        if (currentLevel != null) {
            if (localName.equalsIgnoreCase(TEXT)) {
                currentLevel.setText(builder.toString().trim());
                if (currentLevels != null) {
                    currentLevels.add(currentLevel);
                }
            } else if (localName.equalsIgnoreCase(LEVEL)) {
                // close the current option
                currentLevel = null;
            }
        }
        if (currentAltText != null) {
            if (localName.equalsIgnoreCase(ALT_TEXT)) {
                currentAltText.setText(builder.toString().trim());
                if (currentHelp != null) {
                    currentHelp.addAltText(currentAltText);
                } else if (currentOption != null) {
                    currentOption.addAltText(currentAltText);
                } else if (currentLevel != null) {
                    currentLevel.addAltText(currentAltText);
                } else if (currentQuestion != null) {
                    currentQuestion.addAltText(currentAltText);
                }
                currentAltText = null;
            }
        }
        if (currentHelp != null) {
            if (localName.equalsIgnoreCase(TEXT)) {
                currentHelp.setText(builder.toString().trim());
            }
        }

        builder.setLength(0);
    }

    /**
     * construct a new survey object and store as a member
     */
    public void startDocument() throws SAXException {
        super.startDocument();
        survey = new Survey();
        builder = new StringBuilder();
    }

    /**
     * read in the attributes of the new xml element and set the appropriate
     * values on the object(s) being hydrated.
     */
    public void startElement(String uri, String localName, String name,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, name, attributes);
        if (localName.equalsIgnoreCase(SURVEY)) {
            if (attributes.getValue(NAME) != null) {
                survey.setName(attributes.getValue(NAME));
            }
            if (attributes.getValue(VERSION) != null) {
                survey.setVersion(Double.parseDouble(attributes.getValue(VERSION)));
            }
            if (attributes.getValue(DEFAULT_LANG) != null) {
                survey.setLanguage(attributes.getValue(DEFAULT_LANG));
            } else {
                survey.setLanguage(ConstantUtil.ENGLISH_CODE);
            }
            if (attributes.getValue(SOURCE_SURVEY_ID) != null) {
                survey.setSourceSurveyId(attributes.getValue(SOURCE_SURVEY_ID));
            }
            // SurveyGroup info, if exists
            if (attributes.getValue(SURVEY_GROUP_ID) != null &&
                    attributes.getValue(SURVEY_GROUP_NAME) != null) {
                long sgid = Long.valueOf(attributes.getValue(SURVEY_GROUP_ID));
                String sgname = attributes.getValue(SURVEY_GROUP_NAME);
                String regform = attributes.getValue(REGISTRATION_SURVEY);
                survey.setSurveyGroup(new SurveyGroup(sgid, sgname, regform, regform != null));
            }
            survey.setApp(attributes.getValue(APP));
        } else if (localName.equalsIgnoreCase(QUESTION_GROUP)) {
            currentQuestionGroup = new QuestionGroup();
            if (attributes.getValue(ORDER) != null) {
                currentQuestionGroup.setOrder(Integer.parseInt(attributes
                        .getValue(ORDER)));
            } else {
                int count = 1;
                if (survey != null && survey.getQuestionGroups() != null) {
                    count = survey.getQuestionGroups().size() + 2;
                }
                currentQuestionGroup.setOrder(count);
            }
            // Repeatable flag
            currentQuestionGroup.setRepeatable(Boolean.parseBoolean(attributes.getValue(REPEATABLE)));
        } else if (localName.equalsIgnoreCase(QUESTION)) {
            currentQuestion = new Question();
            if (attributes.getValue(ORDER) != null) {
                currentQuestion.setOrder(Integer.parseInt(attributes
                        .getValue(ORDER)));
            } else {
                int count = 1;
                if (currentQuestionGroup != null
                        && currentQuestionGroup.getQuestions() != null) {
                    count = currentQuestionGroup.getQuestions().size() + 2;
                }
                currentQuestion.setOrder(count);
            }
            if (attributes.getValue(MANDATORY) != null) {
                currentQuestion.setMandatory(Boolean.parseBoolean(attributes
                        .getValue(MANDATORY)));
            } else {
                currentQuestion.setMandatory(false);
            }
            if (attributes.getValue(LOCKED) != null) {
                currentQuestion.setLocked(Boolean.parseBoolean(attributes
                        .getValue(LOCKED)));
            } else {
                currentQuestion.setLocked(false);
            }
            
            // Double Entry flag
            if (attributes.getValue(DOUBLE_ENTRY) != null) {
                currentQuestion.setIsDoubleEntry(Boolean.parseBoolean(attributes
                        .getValue(DOUBLE_ENTRY)));
            } else {
                currentQuestion.setIsDoubleEntry(false);
            }

            // 'allowMultiple' flag can be found at the <question> and <options> scopes. In option
            // questions, the latter will be used. For the rest, the flag will be set in <question>
            currentQuestion.setAllowMultiple(Boolean.parseBoolean(attributes.getValue(ALLOW_MULT)));

            currentQuestion.setType(attributes.getValue(TYPE));
            currentQuestion.setId(attributes.getValue(ID));
            //This validation no longer exists
            String validation = attributes.getValue(VALIDATION_TYPE);
            if (validation != null && validation.trim().length() > 0) {
                currentQuestion
                        .setValidationRule(new ValidationRule(validation));
            }
            //strength no longer exists
            if (attributes.getValue(STRENGTH_MAX) != null
                    && currentQuestion.getType().equalsIgnoreCase(
                            ConstantUtil.STRENGTH_QUESTION_TYPE)) {
                currentQuestion.setUseStrength(true);
                try {
                    currentQuestion.setStrengthMax(Integer.parseInt(attributes
                            .getValue(STRENGTH_MAX).trim()));
                    if (attributes.getValue(STRENGTH_MIN) != null) {
                        currentQuestion.setStrengthMin(Integer
                                .parseInt(attributes.getValue(STRENGTH_MIN)
                                        .trim()));
                    } else {
                        currentQuestion.setStrengthMin(0);
                    }
                } catch (NumberFormatException e) {
                    currentQuestion.setUseStrength(false);
                    currentQuestion.setType(ConstantUtil.OPTION_QUESTION_TYPE);
                    Timber.e(e, "Could not parse strength values");
                }
            } else {
                currentQuestion.setUseStrength(false);
            }
            
            // Locale Flags
            if (attributes.getValue(LOCALE_NAME) != null) {
                currentQuestion.setIsLocaleName(Boolean.parseBoolean(attributes
                        .getValue(LOCALE_NAME)));
            }
            if (attributes.getValue(LOCALE_LOCATION) != null) {
                currentQuestion.setIsLocaleLocation(Boolean.parseBoolean(attributes
                        .getValue(LOCALE_LOCATION)));
            }
            if (attributes.getValue(SOURCE_QUESTION_ID) != null) {
                currentQuestion.setSourceQuestionId(attributes.getValue(SOURCE_QUESTION_ID));
            }

            // Question src. Added in cascading question implementation.
            currentQuestion.setSrc(attributes.getValue(CASCADE_RESOURCE));

            currentQuestion.setCaddisflyRes(attributes.getValue(CADDISFLY_RESOURCE));

            // Geoshape options (question scope)
            currentQuestion.setAllowPoints(Boolean.parseBoolean(attributes.getValue(ALLOW_POINTS)));
            currentQuestion.setAllowLine(Boolean.parseBoolean(attributes.getValue(ALLOW_LINE)));
            currentQuestion.setAllowPolygon(Boolean.parseBoolean(attributes.getValue(ALLOW_POLYGON)));
        } else if (localName.equalsIgnoreCase(OPTIONS)) {
            currentOptions = new ArrayList<>();
            if (currentQuestion != null) {
                if (attributes.getValue(ALLOW_OTHER) != null) {
                    currentQuestion.setAllowOther(Boolean
                            .parseBoolean(attributes.getValue(ALLOW_OTHER)));
                } else {
                    currentQuestion.setAllowOther(false);
                }
                currentQuestion.setRenderType(attributes.getValue(RENDER_TYPE));
                if (attributes.getValue(ALLOW_MULT) != null) {
                    currentQuestion.setAllowMultiple(Boolean
                            .parseBoolean(attributes.getValue(ALLOW_MULT)));
                } else {
                    currentQuestion.setAllowMultiple(false);
                }
            }
        } else if (localName.equalsIgnoreCase(OPTION)) {
            currentOption = new Option();
            currentOption.setCode(attributes.getValue(CODE));
        } else if (localName.equalsIgnoreCase(LEVELS)) {
            currentLevels = new ArrayList<>();
        } else if (localName.equalsIgnoreCase(LEVEL)) {
            currentLevel = new Level();
        } else if (localName.equalsIgnoreCase(DEPENDENCY)) {
            Dependency currentDependency = new Dependency();
            currentDependency.setQuestion(attributes.getValue(QUESTION));
            currentDependency.setAnswer(attributes.getValue(ANSWER));
            if (currentQuestion != null) {
                currentQuestion.addDependency(currentDependency);
            }
        } else if (localName.equalsIgnoreCase(VALIDATION_RULE)) {
            currentValidation = new ValidationRule(
                    attributes.getValue(VALIDATION_TYPE));
            currentValidation.setAllowDecimal(attributes.getValue(ALLOW_DEC));
            currentValidation.setAllowSigned(attributes.getValue(ALLOW_SIGN));
            currentValidation.setMaxLength(attributes.getValue(MAX_LENGTH));
            currentValidation.setMinVal(attributes.getValue(MIN_VAL));
            currentValidation.setMaxVal(attributes.getValue(MAX_VAL));
        } else if (localName.equalsIgnoreCase(ALT_TEXT)) {
            currentAltText = new AltText();
            currentAltText.setLanguage(attributes.getValue(LANG));
            currentAltText.setType(attributes.getValue(TYPE));
        } else if (localName.equalsIgnoreCase(HELP)) {
            currentHelp = new QuestionHelp();
            currentHelp.setType(attributes.getValue(TYPE));
            currentHelp.setValue(attributes.getValue(VALUE));
        }
    }
}
