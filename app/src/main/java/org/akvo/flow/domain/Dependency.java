/*
 *  Copyright (C) 2010-2015 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.serialization.response.value.OptionValue;

import java.util.List;

/**
 * data structure representing a dependency between questions. A dependency
 * consists of two values: a question ID and an answer value. When a question
 * contains a dependency, it will not be shown unless the question referenced by
 * the dependency's questionID has an answer that matches the answerValue in the
 * A question can have 0 or 1 dependencies.
 * 
 * @author Christopher Fagiani
 */
public class Dependency {
    private String question;
    private String answer;

    public Dependency() {
    }

    /**
     * Copy constructor
     */
    public Dependency(Dependency dependency) {
        this.question = dependency.getQuestion();
        this.answer = dependency.getAnswer();
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isMatch(String val) {
        if (answer == null || val == null) {
            return answer == val;
        }

        List<Option> values = OptionValue.deserialize(val);
        for (Option o : values) {
            for (String a : answer.split("\\|", -1)) {
                if (o.getText().trim().equals(a.trim())) {
                    return true;
                }
            }
        }

        return false;
    }
}
