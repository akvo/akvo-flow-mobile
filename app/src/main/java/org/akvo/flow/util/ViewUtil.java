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

package org.akvo.flow.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.akvo.flow.R;
import org.akvo.flow.service.ServiceToastRunnable;

/**
 * Utility class to handle common features for the View tier
 *
 * @author Christopher Fagiani
 */
public class ViewUtil {

    /**
     * displays the alert dialog box warning that the GPS receiver is off. If
     * the affirmative button is clicked, the Location Settings panel is
     * launched. If the negative button is clicked, it will just close the
     * dialog
     *
     * @param parentContext
     */
    public static void showGPSDialog(final Context parentContext) {
        AlertDialog.Builder builder = new AlertDialog.Builder(parentContext);
        builder.setMessage(R.string.geodialog)
                .setCancelable(true)
                .setPositiveButton(R.string.okbutton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                parentContext
                                        .startActivity(new Intent(
                                                "android.settings.LOCATION_SOURCE_SETTINGS"));
                            }
                        })
                .setNegativeButton(R.string.cancelbutton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        builder.show();
    }

    /**
     * displays a simple dialog box with only a single, positive button using
     * the resource ids of the strings passed in for the title and text.
     *
     * @param titleId
     * @param textId
     * @param parentContext
     */
    private static void showConfirmDialog(int titleId, int textId,
            Context parentContext) {
        showConfirmDialog(titleId, textId, parentContext, false,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (dialog != null) {
                            dialog.cancel();
                        }
                    }
                });
    }

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
     * shows an authentication dialog that asks for the administrator passcode
     */
    public static void showAdminAuthDialog(final Context parentContext,
            final AdminAuthDialogListener listener) {
        final EditText input = new EditText(parentContext);
        input.setSingleLine();
        ShowTextInputDialog(parentContext, R.string.authtitle,
                R.string.authtext, input,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String val = input.getText().toString();
                        if (ConstantUtil.ADMIN_AUTH_CODE.equals(val)) {
                            listener.onAuthenticated();
                            if (dialog != null) {
                                dialog.dismiss();
                            }
                        } else {
                            showConfirmDialog(R.string.authfailed,
                                    R.string.invalidpassword, parentContext);
                            if (dialog != null) {
                                dialog.dismiss();
                            }
                        }
                    }
                });
    }

    /**
     * shows a dialog that prompts the user to enter a single text value as
     * input
     */
    public static void ShowTextInputDialog(final Context parentContext,
            int title, int text, EditText inputView,
            DialogInterface.OnClickListener clickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(parentContext);
        LinearLayout main = new LinearLayout(parentContext);
        main.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        main.setOrientation(LinearLayout.VERTICAL);
        builder.setTitle(title);
        builder.setMessage(text);
        main.addView(inputView);
        builder.setView(main);
        builder.setPositiveButton(R.string.okbutton, clickListener);

        builder.setNegativeButton(R.string.cancelbutton,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                });

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

    /**
     * interface that should be implemented by uses of the AdminAuthDialog to be
     * notified when authorization is successful
     */
    public interface AdminAuthDialogListener {
        void onAuthenticated();
    }

}
