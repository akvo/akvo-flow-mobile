/*
 *  Copyright (C) 2015-2019 Stichting Akvo (Akvo Foundation)
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
import androidx.core.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;

import org.akvo.flow.R;
import org.akvo.flow.util.ViewUtil;

public class QuestionGroupIterationHeader extends androidx.appcompat.widget.AppCompatTextView
        implements View.OnTouchListener {

    private String mTitle;
    private int mID, mPosition;
    private OnDeleteListener mListener;
    private final int paddingLeftRight;

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

        int paddingTopBottom = getDimension(R.dimen.small_padding);
        paddingLeftRight = getDimension(R.dimen.form_left_right_padding);
        setCompoundDrawablePadding(paddingTopBottom);
        setPadding(paddingLeftRight, paddingTopBottom, paddingLeftRight, paddingTopBottom);

        setTextSize(20);
        setTextColor(ContextCompat.getColor(context, R.color.repetitions_text_color));
        setBackgroundColor(ContextCompat.getColor(context, R.color.background_alternate));
        showTitleWithPosition(pos);
    }

    private int getDimension(int resId) {
        return (int) getResources().getDimension(resId);
    }

    private void showTitleWithPosition(int pos) {
        setText(getContext().getString(R.string.repeated_group_title, mTitle, pos));
    }

    public void showDeleteIcon() {
        if (mListener != null) {
            Drawable deleteIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_trash);
            setCompoundDrawablesWithIntrinsicBounds(null, null, deleteIcon, null);
            setOnTouchListener(this);
        }
    }

    public void hideDeleteIcon() {
        setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        setOnTouchListener(null);
    }

    public void decreasePosition() {
        showTitleWithPosition(--mPosition);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                if (isDeleteButtonPressed(event)) {
                    ViewUtil.showConfirmDialog(R.string.delete_group_title,
                            R.string.delete_group_text,
                            getContext(), true, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mListener.onDeleteRepetition(mID);
                                }
                            });
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    private boolean isDeleteButtonPressed(MotionEvent event) {
        int x = getRight() - getCompoundDrawables()[2].getBounds().width()
                - (paddingLeftRight * 2);
        return event.getRawX() > x;
    }
}
