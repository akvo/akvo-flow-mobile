/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation, either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
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
            createNotificationBuilder(title, text, context).setStyle(new NotificationCompat.BigTextStyle().bigText(text));
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
            createErrorNotificationBuilder(title, text, context).setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        notifyWithDummyIntent(context, notificationId, builder);
    }


    public static void displayNonOnGoingErrorNotification(Context context, int notificationId, String text, String title) {
        NotificationCompat.Builder builder = createErrorNotificationBuilder(title, text, context);
        builder.setOngoing(false);

        notifyWithDummyIntent(context, notificationId, builder);
    }

    public static void displayProgressNotification(Context context, int synced, int total, String title, String text,
                                                   int notificationId) {
        NotificationCompat.Builder builder = createNotificationBuilder(title, text, context);
        builder.setOngoing(true);

        // Progress will only be displayed in Android versions > 4.0
        builder.setProgress(total, synced, false);

        notifyWithDummyIntent(context, notificationId, builder);
    }

    public static void displayNonOngoingNotificationWithProgress(Context context, String text, String title,
                                                                 int notificationId) {
        NotificationCompat.Builder builder = createNotificationBuilder(title, text, context);
        builder.setOngoing(false);

        // Progress will only be displayed in Android versions > 4.0
        builder.setProgress(1, 1, false);

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
        return new NotificationCompat.Builder(context).setSmallIcon(R.drawable.notification_icon)
                                                      .setColor(context.getResources().getColor(R.color.orange_main))
                                                      .setContentTitle(title)
                                                      .setContentText(text)
                                                      .setTicker(title);
    }

    private static NotificationCompat.Builder createErrorNotificationBuilder(String title, String text, Context context) {
        return new NotificationCompat.Builder(context).setSmallIcon(R.drawable.notification_icon)
                                                      .setColor(context.getResources().getColor(R.color.red))
                                                      .setContentTitle(title)
                                                      .setContentText(text)
                                                      .setTicker(title);
    }
}
