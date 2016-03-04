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

package org.akvo.flow.ui.view;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
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
    private static final String TAG = DateQuestionView.class.getSimpleName();

    private EditText mDateTextEdit;
    private int mYear;
    private int mMonth;
    private int mDay;
    private DateFormat mDateFormat;
    private Date mSelectedDate;
    private Calendar mCalendar;

    public DateQuestionView(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        mCalendar = Calendar.getInstance();
        mYear = mCalendar.get(Calendar.YEAR);
        mMonth = mCalendar.get(Calendar.MONTH);
        mDay = mCalendar.get(Calendar.DAY_OF_MONTH);
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
            Calendar c = new GregorianCalendar();
            c.setTime(mSelectedDate);
            mDay = c.get(Calendar.DAY_OF_MONTH);
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
        }
        DatePickerDialog dia = new DatePickerDialog(getContext(), new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mSelectedDate = new GregorianCalendar(year, monthOfYear, dayOfMonth).getTime();
                mDateTextEdit.setText(mDateFormat.format(mSelectedDate));
                captureResponse();
            }
        }, mYear, mMonth, mDay);
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
            Log.e(TAG, "parseDateValue() - Value is not a number: " + value);
        }
        return null;
    }
    
}
