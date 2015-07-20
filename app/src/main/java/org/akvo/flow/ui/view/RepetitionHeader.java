package org.akvo.flow.ui.view;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.util.ViewUtil;

public class RepetitionHeader extends TextView implements View.OnTouchListener {
    private String mTitle;
    private int mIndex;
    private OnDeleteListener mListener;

    public interface OnDeleteListener {
        public void onDelete(int index);
    }

    public RepetitionHeader(Context context, String title, int index, OnDeleteListener listener) {
        super(context);

        mTitle = title;
        mIndex = index;
        mListener = listener;

        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        setText(mTitle + " - " + index);
        setTextColor(getResources().getColor(R.color.text_color_orange));
        setBackgroundColor(getResources().getColor(R.color.background_alternate));

        Drawable deleteIcon = getContext().getResources().getDrawable(R.drawable.ic_trash);
        setCompoundDrawablesWithIntrinsicBounds(null, null, deleteIcon, null);

        setOnTouchListener(this);
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
                                mListener.onDelete(mIndex);
                            }
                        });
                return true;
            default:
                return false;
        }
    }

}
