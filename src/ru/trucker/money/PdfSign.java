package ru.trucker.money;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/** Сохранение подписанного PDF через PDFBox (исходный текст сохраняется). */
public class PdfSign {

    public static class Place {
        public float nx, ny, nw; // top-left, в долях страницы (0..1)
        public Place(float nx, float ny, float nw) {
            this.nx = nx;
            this.ny = ny;
            this.nw = nw;
        }
    }

    private static boolean inited = false;

    public static void init(Context c) {
        if (inited) return;
        inited = true;
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(c.getApplicationContext());
    }

    public static void save(Context c, Uri src, OutputStream out,
                            Map<Integer, List<Place>> places,
                            Bitmap signature) throws Exception {
        init(c);
        InputStream in = c.getContentResolver().openInputStream(src);
        if (in == null) throw new Exception("Не удалось открыть PDF");
        PDDocument doc = PDDocument.load(in);
        in.close();
        try {
            byte[] png = png(signature);
            PDImageXObject img = PDImageXObject.createFromByteArray(doc, png, "signature");
            float aspect = (float) signature.getHeight() / (float) signature.getWidth();
            for (Map.Entry<Integer, List<Place>> e : places.entrySet()) {
                if (e.getValue().isEmpty()) continue;
                PDPage page = doc.getPage(e.getKey());
                float pw = page.getMediaBox().getWidth();
                float ph = page.getMediaBox().getHeight();
                PDPageContentStream cs = new PDPageContentStream(doc, page,
                        PDPageContentStream.AppendMode.APPEND, true, true);
                for (Place p : e.getValue()) {
                    float w = p.nw * pw;
                    float h = w * aspect;
                    float x = p.nx * pw;
                    float y = ph - p.ny * ph - h;
                    cs.drawImage(img, x, y, w, h);
                }
                cs.close();
            }
            doc.save(out);
        } finally {
            doc.close();
        }
    }

    private static byte[] png(Bitmap bmp) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (!bmp.compress(Bitmap.CompressFormat.PNG, 100, bos)) {
            throw new Exception("Не удалось сжать подпись");
        }
        return bos.toByteArray();
    }
}
