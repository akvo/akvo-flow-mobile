/*
 *  Copyright (C) 2010-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.view.Display;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import org.akvo.flow.R;
import org.akvo.flow.service.ServiceToastRunnable;

import androidx.annotation.NonNull;

/**
 * Utility class to handle common features for the View tier
 *
 * @author Christopher Fagiani
 */
public class ViewUtil {

    /**
     * displays a simple dialog box with a single positive button and an
     * optional (based on a flag) cancel button using the resource ids of the
     * strings passed in for the title and text.
     *
     * @param titleId
     * @param textId
     * @param parentContext
     */
    public static void showConfirmDialog(int titleId, int textId,
            Context parentContext, boolean includeNegative,
            DialogInterface.OnClickListener listener) {
        showConfirmDialog(titleId, textId, parentContext, includeNegative,
                listener, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                });
    }

    /**
     * displays a simple dialog box with a single positive button and an
     * optional (based on a flag) cancel button using the resource ids of the
     * strings passed in for the title and text. users can install listeners for
     * both the positive and negative buttons
     *
     * @param titleId
     * @param textId
     * @param parentContext
     * @param includeNegative
     * @param positiveListener - if includeNegative is false, this will also be
     *                         bound to the cancel handler
     * @param negativeListener - only used if includeNegative is true - if the
     *                         negative listener is non-null, it will also be bound to the
     *                         cancel listener so pressing back to dismiss the dialog will
     *                         have the same effect as clicking the negative button.
     */
    public static void showConfirmDialog(int titleId, int textId,
            Context parentContext, boolean includeNegative,
            final DialogInterface.OnClickListener positiveListener,
            final DialogInterface.OnClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(parentContext);
        builder.setTitle(titleId);
        builder.setMessage(textId);
        builder.setPositiveButton(R.string.okbutton, positiveListener);
        if (includeNegative) {
            builder.setNegativeButton(R.string.cancelbutton, negativeListener);
            if (negativeListener != null) {
                builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        negativeListener.onClick(dialog, -1);
                    }
                });
            }
        } else if (positiveListener != null) {
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    positiveListener.onClick(dialog, -1);
                }
            });
        }

        builder.show();
    }

    /**
     * displays a simple dialog box with a single positive button and an
     * optional (based on a flag) cancel button using the resource id of the
     * string passed in for the title, and a String parameter for the text.
     * users can install listeners for both the positive and negative buttons
     *
     * @param titleId
     * @param text
     * @param parentContext
     * @param includeNegative
     * @param positiveListener - if includeNegative is false, this will also be
     *                         bound to the cancel handler
     * @param negativeListener - only used if includeNegative is true - if the
     *                         negative listener is non-null, it will also be bound to the
     *                         cancel listener so pressing back to dismiss the dialog will
     *                         have the same effect as clicking the negative button.
     */
    public static void showConfirmDialog(int titleId, String text,
            Context parentContext, boolean includeNegative,
            final DialogInterface.OnClickListener positiveListener,
            final DialogInterface.OnClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(parentContext);
        builder.setTitle(titleId);
        builder.setMessage(text);
        builder.setPositiveButton(R.string.okbutton, positiveListener);
        if (includeNegative) {
            builder.setNegativeButton(R.string.cancelbutton, negativeListener);
            if (negativeListener != null) {
                builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        negativeListener.onClick(dialog, -1);
                    }
                });
            }
        } else if (positiveListener != null) {
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    positiveListener.onClick(dialog, -1);
                }
            });
        }

        builder.show();
    }

    /**
     * Display a UI Toast using the Handler's thread (main thread)
     *
     * @param msg                message to display
     * @param uiThreadHandler    the handler to use
     * @param applicationContext the Context to use for the toast
     */
    public static void displayToastFromService(@NonNull final String msg,
            @NonNull Handler uiThreadHandler,
            @NonNull final Context applicationContext) {
        uiThreadHandler.post(new ServiceToastRunnable(applicationContext, msg));
    }

    public static int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display;
        if (windowManager != null) {
            display = windowManager.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            return size.x;
        }
        return 0;
    }

    @SuppressWarnings("deprecation")
    public static void removeLayoutListener(ViewTreeObserver viewTreeObserver,
            ViewTreeObserver.OnGlobalLayoutListener victim) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            viewTreeObserver.removeOnGlobalLayoutListener(victim);
        } else {
            viewTreeObserver.removeGlobalOnLayoutListener(victim);
        }
    }
}
