package ru.trucker.money;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;

/** Хранение подписи в приватном хранилище приложения. */
public class SignatureStore {

    private SignatureStore() {}

    public static File file(Context c) {
        return new File(c.getFilesDir(), "signature.png");
    }

    public static boolean exists(Context c) {
        return file(c).exists();
    }

    public static void save(Context c, Bitmap bmp) throws Exception {
        FileOutputStream fos = new FileOutputStream(file(c));
        try {
            cropToContent(bmp).compress(Bitmap.CompressFormat.PNG, 100, fos);
        } finally {
            fos.close();
        }
    }

    public static Bitmap load(Context c) {
        if (!exists(c)) return null;
        Bitmap bmp = BitmapFactory.decodeFile(file(c).getAbsolutePath());
        return bmp == null ? null : cropToContent(bmp);
    }

    /** Обрезает прозрачные поля по краям, чтобы рамка подписи плотно прилегала к чернилам. */
    public static Bitmap cropToContent(Bitmap src) {
        if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight();
        int[] px = new int[w];
        int top = -1, bottom = -1, left = w, right = -1;
        final int th = 16;
        for (int y = 0; y < h; y++) {
            src.getPixels(px, 0, w, 0, y, w, 1);
            for (int x = 0; x < w; x++) {
                int a = (px[x] >>> 24) & 0xFF;
                if (a >= th) {
                    if (top < 0) top = y;
                    bottom = y;
                    if (x < left) left = x;
                    if (x > right) right = x;
                }
            }
        }
        if (top < 0) return src;
        int cw = right - left + 1;
        int ch = bottom - top + 1;
        if (cw == w && ch == h) return src;
        return Bitmap.createBitmap(src, left, top, cw, ch);
    }
}
