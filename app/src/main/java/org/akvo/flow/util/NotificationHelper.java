/*
 * Copyright (C) 2016-2019 Stichting Akvo (Akvo Foundation)
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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import org.akvo.flow.R;
import org.akvo.flow.activity.SurveyActivity;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

public class NotificationHelper {

    private static final int PENDING_WORK_NOTIFICATION_ID = 1235;

    private NotificationHelper() {
    }

    /**
     * Displays a notification in the system status bar
     *
     * @param title          - headline to display in notification bar
     * @param text           - body of notification (when user expands bar)
     * @param notificationId - unique (within app) ID of notification
     */
    public static void displayNotification(String title, String text, Context context,
            int notificationId) {
        NotificationCompat.Builder builder =
                createNotificationBuilder(title, text, context);
        notifyWithDummyIntent(context, notificationId, builder);
    }

    /**
     * Displays a notification in the system status bar
     *
     * @param title          - headline to display in notification bar
     * @param text           - body of notification (when user expands bar)
     * @param notificationId - unique (within app) ID of notification
     */
    public static void displayErrorNotification(String title, String text, Context context,
            int notificationId) {
        NotificationCompat.Builder builder =
                createErrorNotificationBuilder(title, text, context);
        notifyWithDummyIntent(context, notificationId, builder);
    }

    public static void displayNonOnGoingErrorNotification(Context context, int notificationId,
            String text, String title) {
        NotificationCompat.Builder builder = createErrorNotificationBuilder(title, text, context);
        builder.setOngoing(false);

        notifyWithDummyIntent(context, notificationId, builder);
    }

    public static void displayFormsSyncedNotification(Context context, int synced) {
        String title = context.getString(R.string.downloading_forms);
        String text = String.format(context.getString(R.string.data_sync_synced), synced);

        NotificationCompat.Builder builder = createNotificationBuilder(title, text, context);

        builder.setProgress(synced, synced, false);

        notifyWithDummyIntent(context, ConstantUtil.NOTIFICATION_FORM, builder);
    }

    public static void displayFormsSyncingNotification(Context context) {
        String title = context.getString(R.string.downloading_forms);

        NotificationCompat.Builder builder = createNotificationBuilder(title, "", context);

        builder.setProgress(0, 0, true);

        notifyWithDummyIntent(context, ConstantUtil.NOTIFICATION_FORM, builder);
    }

    public static Notification getUnPublishingNotification(Context context) {
        createNotificationChannel(context);
        String title = context.getString(R.string.unpublish_service_notification_title);
        NotificationCompat.Builder b = new NotificationCompat.Builder(context,
                ConstantUtil.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setTicker(context.getString(R.string.unpublish_service_notification_ticker))
                .setProgress(0, 0, true)
                .setColor(ContextCompat.getColor(context, R.color.orange_main))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true);
        return (b.build());
    }

    public static void showSyncingNotification(Context context) {
        String title = context.getString(R.string.sync_service_notification_title);
        createPendingNotification(context, title, R.string.sync_service_notification_ticker);
    }

    private static void createPendingNotification(Context context, String title, int tickerRes) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(context,
                ConstantUtil.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setTicker(context.getString(tickerRes))
                .setProgress(0, 0, true)
                .setColor(ContextCompat.getColor(context, R.color.orange_main))
                .setOngoing(true);
        notifyWithDummyIntent(context, PENDING_WORK_NOTIFICATION_ID, b);
    }

    public static void showCheckingNotification(Context context) {
        String title = context.getString(R.string.check_service_notification_title);
        createPendingNotification(context, title, R.string.check_service_notification_ticker);
    }

    public static void hidePendingNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(PENDING_WORK_NOTIFICATION_ID);
    }

    private static void notifyWithDummyIntent(Context context, int notificationId,
            NotificationCompat.Builder builder) {
        Intent resultIntent = new Intent(context, SurveyActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        createNotificationChannel(context);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, builder.build());
    }

    private static NotificationCompat.Builder createNotificationBuilder(String title, String text,
            Context context) {
        return createDefaultNotification(title, text, context)
                .setColor(ContextCompat.getColor(context, R.color.orange_main));
    }

    private static NotificationCompat.Builder createDefaultNotification(String title, String text,
            Context context) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                ConstantUtil.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setTicker(title);
        if (!TextUtils.isEmpty(text)) {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
            builder.setContentText(text);
        }
        return builder;
    }

    private static NotificationCompat.Builder createErrorNotificationBuilder(String title,
            String text, Context context) {
        return createDefaultNotification(title, text, context)
                .setColor(ContextCompat.getColor(context, R.color.red));
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.channel_name);
            String description = context.getString(R.string.channel_description);
            NotificationChannel channel = new NotificationChannel(
                    ConstantUtil.NOTIFICATION_CHANNEL_ID, name,
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(description);
            channel.enableVibration(false);
            NotificationManager notificationManager = context
                    .getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
