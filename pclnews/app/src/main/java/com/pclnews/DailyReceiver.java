package com.pclnews;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

/** 每日闹钟 / 手动刷新 触发点。抓取在后台线程进行，用 goAsync 保活。 */
public class DailyReceiver extends BroadcastReceiver {
    public void onReceive(final Context ctx, Intent intent) {
        final PendingResult pr = goAsync();
        final boolean silent = intent != null && intent.getBooleanExtra("silent", false);
        new Thread(new Runnable() {
            public void run() {
                try {
                    List<Fetcher.Item> items = Fetcher.fetchAllWithCache(ctx, true);
                    if (!items.isEmpty()) {
                        Store.saveCache(ctx, items);
                        if (!silent) {
                            Notify.daily(ctx, items);
                            Fetcher.Item firstLty = null;
                            String lastSeen = Store.get(ctx, "lty_last", "");
                            for (Fetcher.Item it : items) {
                                if ("lty".equals(it.cat)) { firstLty = it; break; }
                            }
                            if (firstLty != null && !firstLty.title.equals(lastSeen)) {
                                boolean boot = lastSeen.isEmpty();
                                Store.putString(ctx, "lty_last", firstLty.title);
                                if (!boot) Notify.lty(ctx, "\u6d1b\u5929\u4f9d\u6709\u65b0\u52a8\u6001", firstLty.title);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w("PCLNews", "daily fetch failed", e);
                } finally {
                    Alarm.reschedule(ctx);
                    pr.finish();
                }
            }
        }).start();
    }
}
