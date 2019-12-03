/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.ui.view;

import android.text.TextUtils;

import org.akvo.flow.domain.QuestionResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;

class RepeatableGroupIterations implements Iterable<Integer> {

    private List<Integer> mIDs = new ArrayList<>();

    /**
     * For the given form instance, load the list of repetitions IDs.
     * The populated list will contain the IDs of existing repetitions.
     * Although IDs are auto-incremented numeric values, there might be
     * gaps caused by deleted iterations.
     */
    void loadIDs(Set<String> questionIds, Collection<QuestionResponse> questionResponses) {
        Set<Integer> reps = new HashSet<>();
        for (QuestionResponse qr : questionResponses) {
            String qid = qr.getQuestionId();
            if (!TextUtils.isEmpty(qid) && questionIds.contains(qid) && qr
                    .isAnswerToRepeatableGroup()) {
                reps.add(qr.getIteration());
            }
        }

        mIDs = new ArrayList<>(reps);
        Collections.sort(mIDs);
    }

    /**
     * Create and return the next repetition's ID.
     */
    int next() {
        int id = 0;
        if (!mIDs.isEmpty()) {
            id = mIDs.get(mIDs.size() - 1) + 1;// Increment last item's ID
        }
        mIDs.add(id);
        return id;
    }

    int getRepetitionId(int index) {
        return mIDs.get(index);
    }

    int size() {
        return mIDs.size();
    }

    void remove(Integer repetitionID) {
        mIDs.remove(repetitionID);
    }

    @NonNull
    @Override
    public Iterator<Integer> iterator() {
        return mIDs.iterator();
    }
}
