package ru.trucker.money;

import android.content.Context;
import android.text.format.DateFormat;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

public class Util {

    public static int dp(Context ctx, int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }

    public static String rub(long kopecks) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("ru", "RU"));
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        return nf.format(kopecks / 100.0);
    }

    public static String date(long millis) {
        return DateFormat.format("dd.MM.yyyy", millis).toString();
    }

    public static long parseKopecks(String s) {
        if (s == null) return 0;
        s = s.replaceAll("[^0-9.,]", "").replace(',', '.');
        try {
            double v = Double.parseDouble(s);
            return Math.round(v * 100.0);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String num(double v) {
        String s = String.format(Locale.US, "%.2f", v);
        if (s.endsWith(".00")) s = s.substring(0, s.length() - 3);
        else if (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        return s;
    }

    public static String monthYear() {
        return DateFormat.format("MMMM yyyy", Calendar.getInstance()).toString();
    }
}
