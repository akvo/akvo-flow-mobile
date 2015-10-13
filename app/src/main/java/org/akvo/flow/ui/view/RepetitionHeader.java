package org.akvo.flow.ui.view;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.ViewUtil;

public class RepetitionHeader extends TextView implements View.OnTouchListener {
    private String mTitle;
    private int mID, mPosition;
    private OnDeleteListener mListener;

    public interface OnDeleteListener {
        void onDeleteRepetition(Integer index);
    }

    public RepetitionHeader(Context context, String title, int id, int pos, OnDeleteListener listener) {
        super(context);

        mID = id;
        mPosition = pos;
        mTitle = title;
        mListener = listener;

        int padding = (int)PlatformUtil.dp2Pixel(context, 8);
        setPadding(padding, padding, padding, padding);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        setText(mTitle + " - " + pos);
        setTextColor(getResources().getColor(R.color.text_color_orange));
        setBackgroundColor(getResources().getColor(R.color.background_alternate));

        Drawable deleteIcon = getContext().getResources().getDrawable(R.drawable.ic_trash);
        setCompoundDrawablesWithIntrinsicBounds(null, null, deleteIcon, null);

        setOnTouchListener(this);
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
