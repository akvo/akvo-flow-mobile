/*
 * Copyright (C) 2016-2020 Stichting Akvo (Akvo Foundation)
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

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import org.akvo.flow.R;
import org.akvo.flow.activity.SurveyActivity;
import org.akvo.flow.activity.TimeCheckActivity;
import org.akvo.flow.service.time.CancelNotificationReceiver;

import static org.akvo.flow.util.ConstantUtil.NOTIFICATION_TIME;

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
        createLowPriorityChannel(context);
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

    public static void showTimeCheckNotification(Context context) {
        createHighPriorityChannel(context);

        Intent intent = new Intent(context, TimeCheckActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent actionIntent = new Intent(context, CancelNotificationReceiver.class);
        actionIntent.putExtra(CancelNotificationReceiver.NOTIFICATION_ID_EXTRA, NOTIFICATION_TIME);
        PendingIntent dismissIntent = PendingIntent.getBroadcast(context, NOTIFICATION_TIME, actionIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder b = new NotificationCompat.Builder(context,
                ConstantUtil.NOTIFICATION_CHANNEL_TIME)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(context.getString(R.string.time_check_notification_title))
                .setVibrate(new long[]{300})
                .setContentText(context.getString(R.string.time_check_notification_message))
                .setTicker(context.getString(R.string.time_check_notification_message))
                .setColor(ContextCompat.getColor(context, R.color.orange_main))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .addAction(R.drawable.notification_icon, context.getString(R.string.time_check_action_fix), pendingIntent)
                .addAction(R.drawable.notification_icon, context.getString(R.string.time_check_action_dismiss), dismissIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_TIME, b.build());
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
        cancelNotification(context, PENDING_WORK_NOTIFICATION_ID);
    }

    public static void cancelNotification(Context context, int pendingWorkNotificationId) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(pendingWorkNotificationId);
    }

    private static void notifyWithDummyIntent(Context context, int notificationId,
            NotificationCompat.Builder builder) {
        Intent resultIntent = new Intent(context, SurveyActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        createLowPriorityChannel(context);

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
                .setAutoCancel(true) //hide when user presses it
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

    private static void createLowPriorityChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context, NotificationManager.IMPORTANCE_LOW,
                    ConstantUtil.NOTIFICATION_CHANNEL_ID);
        }
    }

    private static void createHighPriorityChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context, NotificationManager.IMPORTANCE_HIGH,
                    ConstantUtil.NOTIFICATION_CHANNEL_TIME);
        }
    }

    private static void createNotificationChannel(Context context, int importance, String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.channel_name);
            String description = context.getString(R.string.channel_description);
            NotificationChannel channel = new NotificationChannel(
                    channelId, name,
                    importance);
            channel.setDescription(description);
            channel.enableVibration(false);
            NotificationManager notificationManager = context
                    .getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
