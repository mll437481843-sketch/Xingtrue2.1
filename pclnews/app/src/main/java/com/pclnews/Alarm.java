package com.pclnews;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

/** 每日定时：AlarmManager 一次性闹钟 + 自我重排（比 repeating 更准时）。 */
public final class Alarm {
    static final int REQ = 1001;

    public static PendingIntent pi(Context c) {
        Intent i = new Intent(c, DailyReceiver.class);
        i.setAction("com.pclnews.DAILY");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, REQ, i, flags);
    }

    public static long nextAt(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        Calendar t = Calendar.getInstance();
        t.set(Calendar.HOUR_OF_DAY, hour);
        t.set(Calendar.MINUTE, minute);
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);
        if (!t.after(cal)) t.add(Calendar.DAY_OF_YEAR, 1);
        return t.getTimeInMillis();
    }

    public static String schedule(Context c, int hour, int minute) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return "AlarmManager 不可用";
        long at = nextAt(hour, minute);
        PendingIntent p = pi(c);
        am.cancel(p);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
            } else if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, at, p);
            }
        } catch (SecurityException e) {
            Log.w("PCLNews", "exact alarm denied, fallback", e);
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
        }
        Store.putInt(c, Store.K_HOUR, hour);
        Store.putInt(c, Store.K_MIN, minute);
        return String.format(java.util.Locale.US, "%02d:%02d", hour, minute);
    }

    /** 从已保存的设置重排下一次。 */
    public static void reschedule(Context c) {
        int h = Store.getInt(c, Store.K_HOUR, 8);
        int m = Store.getInt(c, Store.K_MIN, 0);
        schedule(c, h, m);
    }
}
