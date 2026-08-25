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
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } finally {
            fos.close();
        }
    }

    public static Bitmap load(Context c) {
        if (!exists(c)) return null;
        return BitmapFactory.decodeFile(file(c).getAbsolutePath());
    }
}
