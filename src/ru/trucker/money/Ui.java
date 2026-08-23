package ru.trucker.money;

import android.content.Context;

/** Цветовая палитра приложения: светлая / тёмная тема. */
public class Ui {

    private Ui() {}

    public static boolean dark(Context c) {
        return c.getSharedPreferences("app", 0).getBoolean("dark_theme", false);
    }

    // Фоны
    public static int bg(Context c) { return dark(c) ? 0xFF141218 : 0xFFF0F2F5; }
    public static int card(Context c) { return dark(c) ? 0xFF1E1B24 : 0xFFFFFFFF; }
    public static int field(Context c) { return dark(c) ? 0xFF2A2633 : 0xFFFFFFFF; }
    public static int headerBg(Context c) { return dark(c) ? 0xFF202A38 : 0xFFE3F2FD; }
    public static int headerDiv(Context c) { return dark(c) ? 0xFF37474F : 0xFFBBDEFB; }
    public static int dangerBg(Context c) { return dark(c) ? 0xFF3A2024 : 0xFFFFEBEE; }

    // Режим просмотра (заблокированное редактирование)
    public static int fieldLocked(Context c) { return dark(c) ? 0xFF1A1721 : 0xFFECEFF1; }
    public static int cardLocked(Context c) { return dark(c) ? 0xFF1A1721 : 0xFFF5F5F5; }
    public static int textLocked(Context c) { return dark(c) ? 0xFF7A7682 : 0xFF9E9E9E; }

    // Текст
    public static int title(Context c) { return dark(c) ? 0xFFECEFF1 : 0xFF37474F; }
    public static int primary(Context c) { return dark(c) ? 0xFFECEFF1 : 0xFF263238; }
    public static int sub(Context c) { return dark(c) ? 0xFF90A4AE : 0xFF90A4AE; }
    public static int label(Context c) { return dark(c) ? 0xFFB0BEC5 : 0xFF607D8B; }
    public static int gray(Context c) { return dark(c) ? 0xFFB0BEC5 : 0xFF546E7A; }
    public static int headerText(Context c) { return dark(c) ? 0xFF90CAF9 : 0xFF1565C0; }
    public static int divider(Context c) { return dark(c) ? 0xFF37474F : 0xFFECEFF1; }

    // Акценты (деньги)
    public static int income(Context c) { return dark(c) ? 0xFF81C784 : 0xFF2E7D32; }
    public static int expense(Context c) { return dark(c) ? 0xFFEF9A9A : 0xFFC62828; }
    public static int accent(Context c) { return dark(c) ? 0xFF42A5F5 : 0xFF1565C0; }
    public static int accentText(Context c) { return dark(c) ? 0xFF90CAF9 : 0xFF1565C0; }
    public static int brown(Context c) { return dark(c) ? 0xFF8D6E63 : 0xFF6D4C41; }
    public static int navBtn(Context c) { return dark(c) ? 0xFF37474F : 0xFF546E7A; }
    public static int barFill(Context c) { return dark(c) ? 0xFFEF5350 : 0xFFEF5350; }
    public static int barRest(Context c) { return dark(c) ? 0xFF37474F : 0xFFECEFF1; }

    // Текст на цветных кнопках
    public static int buttonText(Context c) { return dark(c) ? 0xFF121212 : 0xFFFFFFFF; }
}
