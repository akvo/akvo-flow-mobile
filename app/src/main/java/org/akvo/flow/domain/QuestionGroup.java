/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
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

import java.util.ArrayList;
import java.util.List;

/**
 * data structure for grouping questions under a common heading.
 * 
 * @author Christopher Fagiani
 */
public class QuestionGroup {
    private int order;
    private String heading;
    private boolean repeatable;
    private ArrayList<Question> questions;

    public QuestionGroup() {
        questions = new ArrayList<Question>();
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public ArrayList<Question> getQuestions() {
        return questions;
    }

    public void addQuestion(Question q) {
        questions.add(q);
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }

    public boolean isRepeatable() {
        return repeatable;
    }
    
    public List<String> getLocaleNameQuestions() {
        List<String> localeNameQuestions = new ArrayList<String>();
        if (questions != null) {
            for (Question q : questions) {
                if (q.isLocaleName()) {
                    localeNameQuestions.add(q.getId());
                }
            }
        }
        
        return localeNameQuestions;
    }
    
    public String getLocaleGeoQuestion() {
        if (questions != null) {
            for (Question q : questions) {
                if (q.isLocaleLocation()) {
                    return q.getId();
                }
            }
        }
        return null;
    }
}
