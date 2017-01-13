/*
 *  Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.view;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.ConstantUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import timber.log.Timber;

/**
 * Question for capturing a date (no time component). Once selected, the date
 * will be displayed on the screen using the locale-specific date format
 * (obtained via SimpleDateFormat.getDateInstance()). Though the actual value
 * saved in the response object will be a timestamp (milliseconds since
 * Midnight, Jan 1, 1970 UTC).
 * 
 * @author Christohper Fagiani
 */
public class DateQuestionView extends QuestionView implements View.OnClickListener {

    private EditText mDateTextEdit;
    private DateFormat mDateFormat;
    private Date mSelectedDate;
    private Calendar mLocalCalendar;
    private Calendar mGMTCalendar;

    public DateQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        mLocalCalendar = GregorianCalendar.getInstance();
        mGMTCalendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
        mGMTCalendar.set(Calendar.HOUR_OF_DAY, 0);
        mGMTCalendar.set(Calendar.MINUTE, 0);
        mGMTCalendar.set(Calendar.SECOND, 0);
        mGMTCalendar.set(Calendar.MILLISECOND, 0);
        mDateFormat = SimpleDateFormat.getDateInstance();
        init();
    }

    private void init() {
        setQuestionView(R.layout.date_question_view);

        mDateTextEdit = (EditText)findViewById(R.id.date_et);

        View pickButton = findViewById(R.id.date_btn);
        pickButton.setOnClickListener(this);
        pickButton.setEnabled(!isReadOnly());
    }

    @Override
    public void onClick(View v) {
        if (mSelectedDate != null) {
            mLocalCalendar.setTime(mSelectedDate);
        }
        DatePickerDialog dia = new DatePickerDialog(getContext(), new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mGMTCalendar.set(year, monthOfYear, dayOfMonth);
                mSelectedDate = mGMTCalendar.getTime();
                mDateTextEdit.setText(mDateFormat.format(mSelectedDate));
                captureResponse();
            }
        }, mLocalCalendar.get(Calendar.YEAR), mLocalCalendar.get(Calendar.MONTH), mLocalCalendar.get(Calendar.DAY_OF_MONTH));
        dia.show();
    }

    @Override
    public void setResponse(QuestionResponse resp) {
        if (resp != null && mDateTextEdit != null) {
            mSelectedDate = parseDateValue(resp.getValue());
            if (mSelectedDate != null) {
                mDateTextEdit.setText(mDateFormat.format(mSelectedDate));
            } else {
                mDateTextEdit.setText("");
            }
        }
        super.setResponse(resp);
    }

    /**
     * pulls the data out of the fields and saves it as a response object,
     * possibly suppressing listeners
     */
    @Override
    public void captureResponse(boolean suppressListeners) {
        setResponse(new QuestionResponse(mSelectedDate != null ? mSelectedDate.getTime() + "" : "",
                        ConstantUtil.DATE_RESPONSE_TYPE,
                        getQuestion().getId()),
                suppressListeners);
    }

    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
        if (resp == null || mDateTextEdit == null) {
            return;
        }
        mSelectedDate = parseDateValue(resp.getValue());
        if (mSelectedDate != null) {
            mDateTextEdit.setText(mDateFormat.format(mSelectedDate));
        } else {
            mDateTextEdit.setText("");
        }
    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mDateTextEdit.setText("");
        mSelectedDate = null;
    }

    private Date parseDateValue(String value) {
        try {
            if (!TextUtils.isEmpty(value)) {
                return new Date(Long.parseLong(value));
            }
        } catch (NumberFormatException e) {
            Timber.e("parseDateValue() - Value is not a number: " + value);
        }
        return null;
    }
    
}
