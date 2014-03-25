/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
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
public class DateQuestionView extends QuestionView {
    private EditText mDateTextEdit;
    private Button mPickButton;
    private int mYear;
    private int mMonth;
    private int mDay;
    private DateFormat mDateFormat;
    private Date mSelectedDate;
    private Calendar mCalendar;

    public DateQuestionView(Context context, Question q, String defaultLang, String[] langCodes,
            boolean readOnly) {
        super(context, q, defaultLang, langCodes, readOnly);
        mCalendar = Calendar.getInstance();
        mYear = mCalendar.get(Calendar.YEAR);
        mMonth = mCalendar.get(Calendar.MONTH);
        mDay = mCalendar.get(Calendar.DAY_OF_MONTH);
        mDateFormat = SimpleDateFormat.getDateInstance();
        init();
    }

    protected void init() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.date_question_view, this, true);

        setupQuestion();

        mDateTextEdit = (EditText)findViewById(R.id.date_et);
        mPickButton = (Button)findViewById(R.id.date_btn);

        mPickButton.setOnClickListener(new OnClickListener() {
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
        });
    }

    /**
     * pulls the data out of the fields and saves it as a response object
     */
    @Override
    public void captureResponse() {
        captureResponse(false);
    }

    @Override
    public void setResponse(QuestionResponse resp) {
        if (resp != null && mDateTextEdit != null) {
            if (resp.getValue() != null && resp.getValue().trim().length() > 0) {
                mSelectedDate = new Date(Long.parseLong(resp.getValue()));
                mDateTextEdit.setText(mDateFormat.format(mSelectedDate));
            } else {
                mSelectedDate = null;
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
        if (resp != null && mDateTextEdit != null) {
            if (resp.getValue() != null && resp.getValue().trim().length() > 0) {
                mSelectedDate = new Date(Long.parseLong(resp.getValue()));
                mDateTextEdit.setText(mDateFormat.format(mSelectedDate));
            } else {
                mSelectedDate = null;
                mDateTextEdit.setText("");
            }
        }
    }

    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
        mDateTextEdit.setText("");
        mSelectedDate = null;
    }
    
}
