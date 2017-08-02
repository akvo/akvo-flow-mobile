/*
 *  Copyright (C) 2015-2017 Stichting Akvo (Akvo Foundation)
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

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import org.akvo.flow.R;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.ViewUtil;

public class QuestionGroupIterationHeader extends android.support.v7.widget.AppCompatTextView
        implements View.OnTouchListener {

    private String mTitle;
    private int mID, mPosition;
    private OnDeleteListener mListener;

    public interface OnDeleteListener {

        void onDeleteRepetition(Integer index);
    }

    public QuestionGroupIterationHeader(Context context, String title, int id, int pos,
            OnDeleteListener listener) {
        super(context);

        mID = id;
        mPosition = pos;
        mTitle = title;
        mListener = listener;

        int padding = (int) PlatformUtil.dp2Pixel(context, 8);
        setPadding(padding, padding, padding, padding);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        setText(mTitle + " - " + pos);
        setTextColor(ContextCompat.getColor(context, R.color.repetitions_text_color));
        setBackgroundColor(ContextCompat.getColor(context, R.color.background_alternate));
    }

    /**
     * Show 'delete' icon
     */
    public void enableDeleteButton() {
        if (mListener != null) {
            Drawable deleteIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_trash);
            setCompoundDrawablesWithIntrinsicBounds(null, null, deleteIcon, null);
            setOnTouchListener(this);
        }
    }


    public void disableDeleteButton() {
        setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        setOnTouchListener(null);
    }

    public void decreasePosition() {
        setText(mTitle + " - " + --mPosition);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                int x = getRight() - getCompoundDrawables()[2].getBounds().width();
                if (event.getRawX() < x) {
                    return false;
                }
                ViewUtil.showConfirmDialog(R.string.delete_group_title, R.string.delete_group_text,
                        getContext(), true, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mListener.onDeleteRepetition(mID);
                            }
                        });
                return true;
            default:
                return false;
        }
    }

}
