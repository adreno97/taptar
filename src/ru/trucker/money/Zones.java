package ru.trucker.money;

import android.content.Context;
import android.content.SharedPreferences;

public class Zones {

    public static final String[] NAMES = {
            "Зона 1 (0–50 км)",
            "Зона 2 (51–100 км)",
            "Зона 3 (101–150 км)",
            "Зона 4 (151–200 км)",
            "Зона 5 (201–300 км)",
            "Зона 6 (301–400 км)"
    };

    private static final long[] DEFAULT_KOP = {
            150000, 220000, 290000, 360000, 430000, 500000
    };

    public static final int MAX = 6;
    public static final int MIN = 3;

    public static int getCount(Context ctx) {
        int c = ctx.getSharedPreferences("zones", 0).getInt("count", 3);
        if (c < MIN) c = MIN;
        if (c > MAX) c = MAX;
        return c;
    }

    public static void setCount(Context ctx, int count) {
        ctx.getSharedPreferences("zones", 0).edit().putInt("count", count).apply();
    }

    public static long[] getPrices(Context ctx) {
        int c = getCount(ctx);
        long[] out = new long[c];
        for (int i = 0; i < c; i++) {
            out[i] = i < DEFAULT_KOP.length ? DEFAULT_KOP[i] : 0;
        }
        String s = ctx.getSharedPreferences("zones", 0).getString("prices", null);
        if (s != null) {
            String[] parts = s.split(";");
            for (int i = 0; i < parts.length && i < c; i++) {
                try {
                    out[i] = Long.parseLong(parts[i]);
                } catch (Exception ignored) {
                }
            }
        }
        return out;
    }

    public static void setPrices(Context ctx, long[] kop) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kop.length; i++) {
            if (i > 0) sb.append(';');
            sb.append(kop[i]);
        }
        ctx.getSharedPreferences("zones", 0).edit().putString("prices", sb.toString()).apply();
    }

    public static long priceWithReturn(long base) {
        return base * 3 / 2;
    }

    public static String name(int idx) {
        return idx >= 0 && idx < NAMES.length ? NAMES[idx] : "Зона " + (idx + 1);
    }

    public static String shortName(int idx) {
        return String.valueOf(idx + 1);
    }

    public static final int MAX_EXTRA = 25;

    public static long getExtraPrice(Context ctx) {
        return ctx.getSharedPreferences("zones", 0).getLong("extra_price", 30000);
    }

    public static void setExtraPrice(Context ctx, long kop) {
        ctx.getSharedPreferences("zones", 0).edit().putLong("extra_price", kop).apply();
    }

    public static int getExtraStart(Context ctx) {
        int s = ctx.getSharedPreferences("zones", 0).getInt("extra_start", 3);
        if (s < 1) s = 1;
        return s;
    }

    public static void setExtraStart(Context ctx, int start) {
        if (start < 1) start = 1;
        ctx.getSharedPreferences("zones", 0).edit().putInt("extra_start", start).apply();
    }

    public static int paidExtraPoints(int numPoints, int start) {
        if (numPoints <= 0) return 0;
        int paid = numPoints - (start - 1);
        return paid < 0 ? 0 : paid;
    }

    public static long extraCost(Context ctx, int numPoints) {
        return paidExtraPoints(numPoints, getExtraStart(ctx)) * getExtraPrice(ctx);
    }
}
