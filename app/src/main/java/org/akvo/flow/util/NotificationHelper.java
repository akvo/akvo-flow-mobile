/*
 * Copyright (C) 2016-2017 Stichting Akvo (Akvo Foundation)
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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import org.akvo.flow.R;

public class NotificationHelper {

    private NotificationHelper() {
    }

    /**
     * Displays a notification in the system status bar
     *
     * @param title - headline to display in notification bar
     * @param text - body of notification (when user expands bar)
     * @param notificationId - unique (within app) ID of notification
     */
    public static void displayNotification(String title, String text, Context context, int notificationId) {
        NotificationCompat.Builder builder =
            createNotificationBuilder(title, text, context);
        notifyWithDummyIntent(context, notificationId, builder);
    }

    /**
     * Displays a notification in the system status bar
     *
     * @param title - headline to display in notification bar
     * @param text - body of notification (when user expands bar)
     * @param notificationId - unique (within app) ID of notification
     */
    public static void displayErrorNotification(String title, String text, Context context, int notificationId) {
        NotificationCompat.Builder builder =
            createErrorNotificationBuilder(title, text, context);
        notifyWithDummyIntent(context, notificationId, builder);
    }


    public static void displayNonOnGoingErrorNotification(Context context, int notificationId, String text, String title) {
        NotificationCompat.Builder builder = createErrorNotificationBuilder(title, text, context);
        builder.setOngoing(false);

        notifyWithDummyIntent(context, notificationId, builder);
    }

    public static void displayNotification(Context context, int total, String title, String text, int notificationId,
                                           boolean ongoing, int progress) {
        NotificationCompat.Builder builder = createNotificationBuilder(title, text, context);

        builder.setOngoing(ongoing);// Ongoing if still syncing the records

        // Progress will only be displayed in Android versions > 4.0
        builder.setProgress(total, progress, false);

        notifyWithDummyIntent(context, notificationId, builder);
    }

    public static void displayNotificationWithProgress(Context context, String title, String text, boolean ongoing,
                                                       boolean indeterminate, int notificationId) {
        NotificationCompat.Builder builder = createNotificationBuilder(title, text, context);

        builder.setOngoing(ongoing); // Ongoing if still syncing the records

        // Progress will only be displayed in Android versions > 4.0
        builder.setProgress(1, 1, indeterminate);

        notifyWithDummyIntent(context, notificationId, builder);
    }

    public static void displayErrorNotificationWithProgress(Context context, String title, String text, boolean ongoing,
                                                       boolean indeterminate, int notificationId) {
        NotificationCompat.Builder builder = createErrorNotificationBuilder(title, text, context);

        builder.setOngoing(ongoing); // Ongoing if still syncing the records

        // Progress will only be displayed in Android versions > 4.0
        builder.setProgress(1, 1, indeterminate);

        notifyWithDummyIntent(context, notificationId, builder);
    }

    private static void notifyWithDummyIntent(Context context, int notificationId, NotificationCompat.Builder builder) {
        // Dummy intent. Do nothing when clicked
        PendingIntent dummyIntent = PendingIntent.getActivity(context, 0, new Intent(), 0);
        builder.setContentIntent(dummyIntent);

        NotificationManager notificationManager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, builder.build());
    }

    private static NotificationCompat.Builder createNotificationBuilder(String title, String text, Context context) {
        return createDefaultNotification(title, text, context)
                .setColor(ContextCompat.getColor(context, R.color.orange_main));
    }

    private static NotificationCompat.Builder createDefaultNotification(String title, String text, Context context) {
        return new NotificationCompat.Builder(context).setSmallIcon(R.drawable.notification_icon)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentTitle(title)
                .setContentText(text)
                .setTicker(title);
    }

    private static NotificationCompat.Builder createErrorNotificationBuilder(String title, String text, Context context) {
        return createDefaultNotification(title, text, context)
                .setColor(ContextCompat.getColor(context, R.color.red));
    }
}
