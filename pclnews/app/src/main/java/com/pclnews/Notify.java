package com.pclnews;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 通知：日报（头条列表）与天依更新两类。 */
public final class Notify {
    static final String CH_DAILY = "daily";
    static final String CH_LTY = "lty";

    static void ensure(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel a = new NotificationChannel(CH_DAILY, "每日新闻", NotificationManager.IMPORTANCE_DEFAULT);
        a.setDescription("每天定时推送的头条摘要");
        nm.createNotificationChannel(a);
        NotificationChannel b = new NotificationChannel(CH_LTY, "洛天依更新", NotificationManager.IMPORTANCE_HIGH);
        b.setDescription("洛天依官号发布新内容时提醒");
        nm.createNotificationChannel(b);
    }

    static PendingIntent open(Context c, int req) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(c, req, i, flags);
    }

    static Notification.Builder base(Context c, String ch) {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) b = new Notification.Builder(c, ch);
        else b = new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setContentIntent(open(c, 2001));
        return b;
    }

    public static void daily(Context c, List<Fetcher.Item> items) {
        ensure(c);
        int n = items.size();
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (Fetcher.Item it : items) {
            if ("lty".equals(it.cat)) continue;
            if (shown >= 5) break;
            sb.append("\u00b7 ").append(it.title).append('\n');
            shown++;
        }
        Notification.Builder b = base(c, CH_DAILY);
        b.setContentTitle("\u4eca\u65e5\u8981\u95fb \u00b7 \u5171 " + n + " \u6761")
                .setContentText(shown > 0 ? sb.toString().split("\n")[0] : "\u6682\u65e0\u5185\u5bb9")
                .setStyle(new Notification.BigTextStyle().bigText(sb.toString().trim()))
                .setNumber(n);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(1, b.build());
    }

    public static void lty(Context c, String title, String text) {
        ensure(c);
        Notification.Builder b = base(c, CH_LTY);
        b.setContentTitle(title).setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text));
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(2, b.build());
    }
}
