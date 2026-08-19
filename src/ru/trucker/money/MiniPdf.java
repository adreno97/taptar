package ru.trucker.money;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal PDF generator with embedded TrueType font (Cyrillic support).
 * No external dependencies — safe for Android.
 */
public class MiniPdf {

    public static final float PAGE_W = 595f; // A4
    public static final float PAGE_H = 842f;

    private final List<String> pages = new ArrayList<>();
    private final List<Integer> glyphUsed = new ArrayList<>();
    private final Map<Integer, Integer> glyphToUnicode = new LinkedHashMap<>();

    private final int unitsPerEm;
    private final int ascent, descent;
    private final int[] advances;
    private final Map<Character, Integer> cmap = new LinkedHashMap<>();
    private final byte[] ttf;
    private float r = 0f, g = 0f, b = 0f;
    private String title = "Документ";

    public MiniPdf(byte[] ttfBytes) throws Exception {
        this.ttf = ttfBytes;
        int[] dir = tableDirectory(ttfBytes);
        int headOff = dir[0];
        int hheaOff = dir[1];
        int hmtxOff = dir[2];
        int cmapOff = dir[3];
        this.unitsPerEm = u16(ttfBytes, headOff + 18);
        this.ascent = s16(ttfBytes, hheaOff + 4);
        this.descent = s16(ttfBytes, hheaOff + 6);
        int numberOfHMetrics = u16(ttfBytes, hheaOff + 34);
        advances = new int[numberOfHMetrics];
        for (int i = 0; i < numberOfHMetrics; i++) {
            advances[i] = u16(ttfBytes, hmtxOff + i * 4);
        }
        parseCmap(ttfBytes, cmapOff);
    }

    private int[] tableDirectory(byte[] t) {
        int numTables = u16(t, 4);
        int head = 0, hhea = 0, hmtx = 0, cmap = 0;
        for (int i = 0; i < numTables; i++) {
            int rec = 12 + i * 16;
            String tag = new String(t, rec, 4, StandardCharsets.ISO_8859_1);
            int off = u32(t, rec + 8);
            if ("head".equals(tag)) head = off;
            else if ("hhea".equals(tag)) hhea = off;
            else if ("hmtx".equals(tag)) hmtx = off;
            else if ("cmap".equals(tag)) cmap = off;
        }
        return new int[]{head, hhea, hmtx, cmap};
    }

    private void parseCmap(byte[] t, int cmapOff) {
        int numTables = u16(t, cmapOff + 2);
        int best = -1;
        for (int i = 0; i < numTables; i++) {
            int rec = cmapOff + 4 + i * 8;
            int platform = u16(t, rec);
            int encoding = u16(t, rec + 2);
            int off = u32(t, rec + 4);
            if (platform == 3 && encoding == 1) { best = cmapOff + off; break; }
            if (platform == 0 && (encoding == 3 || encoding == 4) && best < 0) best = cmapOff + off;
            if (platform == 0 && encoding == 1 && best < 0) best = cmapOff + off;
        }
        if (best < 0) return;
        int format = u16(t, best);
        if (format != 4) return;
        int segCountX2 = u16(t, best + 6);
        int segCount = segCountX2 / 2;
        int endCode = best + 14;
        int startCode = endCode + segCountX2 + 2;
        int idDelta = startCode + segCountX2;
        int idRangeOffset = idDelta + segCountX2;
        for (int s = 0; s < segCount; s++) {
            int sc = u16(t, startCode + s * 2);
            int ec = u16(t, endCode + s * 2);
            int ido = u16(t, idRangeOffset + s * 2);
            int delta = s16(t, idDelta + s * 2);
            for (int c = sc; c <= ec; c++) {
                int glyph;
                if (ido == 0) {
                    glyph = (c + delta) & 0xFFFF;
                } else {
                    int addr = idRangeOffset + s * 2 + ido + (c - sc) * 2;
                    glyph = u16(t, addr);
                    if (glyph != 0) glyph = (glyph + delta) & 0xFFFF;
                }
                if (glyph != 0) cmap.put((char) c, glyph);
            }
        }
    }

    public void setTitle(String t) { title = t; }

    public void setColor(float rr, float gg, float bb) { r = rr; g = gg; b = bb; }

    public void addPage() {
        pages.add("");
    }

    public float lineHeight(float size) {
        return (ascent - descent) / (float) unitsPerEm * size * 1.35f;
    }

    public float measure(String s, float size) {
        float w = 0;
        for (int i = 0; i < s.length(); i++) {
            int gid = glyphFor(s.charAt(i));
            w += advance(gid) / (float) unitsPerEm * 1000f;
        }
        return w * size / 1000f;
    }

    private int advance(int gid) {
        if (gid < advances.length) return advances[gid];
        return advances[0];
    }

    private int glyphFor(char c) {
        Integer g = cmap.get(c);
        return g != null ? g : glyphFor(' ');
    }

    private void markUsed(char c) {
        int gid = glyphFor(c);
        if (gid != 0 && !glyphToUnicode.containsKey(gid)) {
            glyphToUnicode.put(gid, (int) c);
            glyphUsed.add(gid);
        }
    }

    private String textOps(String s, float x, float y, float size) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            markUsed(c);
            int gid = glyphFor(c);
            hex.append(String.format("%04X", gid));
        }
        return String.format(java.util.Locale.US, "BT /F1 %f Tf %.3f %.3f %.3f rg %.2f %.2f Td <%s> Tj ET",
                size, r, g, b, x, y, hex.toString());
    }

    /** y measured from top of the page; places the glyph TOP at y. */
    public void text(String s, float x, float yFromTop, float size) {
        markAll(s);
        String s2 = s.isEmpty() ? " " : s;
        int page = pages.size() - 1;
        float ascentPt = ascent / (float) unitsPerEm * size;
        float pdfY = PAGE_H - yFromTop - ascentPt;
        append(page, textOps(s2, x, pdfY, size));
    }

    public void textRight(String s, float xRight, float yFromTop, float size) {
        float w = measure(s, size);
        text(s, xRight - w, yFromTop, size);
    }

    public void textCenter(String s, float xCenter, float yFromTop, float size) {
        float w = measure(s, size);
        text(s, xCenter - w / 2f, yFromTop, size);
    }

    public void rect(float x, float yFromTop, float w, float h, float lw) {
        int page = pages.size() - 1;
        float pdfY = PAGE_H - yFromTop - h;
        append(page, String.format(java.util.Locale.US,
                "%.3f %.3f %.3f RG %.2f w %.2f %.2f %.2f %.2f re S",
                r, g, b, lw, x, pdfY, w, h));
    }

    public void fillRect(float x, float yFromTop, float w, float h) {
        int page = pages.size() - 1;
        float pdfY = PAGE_H - yFromTop - h;
        append(page, String.format(java.util.Locale.US,
                "%.3f %.3f %.3f rg %.2f %.2f %.2f %.2f re f",
                r, g, b, x, pdfY, w, h));
    }

    /** Greedy word wrap returning list of lines not exceeding maxWidth. */
    public List<String> wrap(String s, float maxWidth, float size) {
        List<String> lines = new ArrayList<>();
        String[] words = s.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String wd : words) {
            String trial = cur.length() == 0 ? wd : cur + " " + wd;
            if (measure(trial, size) <= maxWidth || cur.length() == 0) {
                cur = new StringBuilder(trial);
            } else {
                lines.add(cur.toString());
                cur = new StringBuilder(wd);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    private void markAll(String s) {
        for (int i = 0; i < s.length(); i++) markUsed(s.charAt(i));
    }

    private void append(int page, String op) {
        pages.set(page, pages.get(page) + op + "\n");
    }

    public byte[] build() throws Exception {
        if (pages.isEmpty()) addPage();
        List<String> out = new ArrayList<>();
        int pageBase = 9;
        List<Integer> pageObjs = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            pageObjs.add(pageBase + i * 2);
        }
        out.add("<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder("<< /Type /Pages /Kids [");
        for (int p : pageObjs) kids.append(p).append(" 0 R ");
        kids.append("] /Count ").append(pages.size()).append(" >>");
        out.add(kids.toString());

        out.add("<< /Type /Font /Subtype /Type0 /BaseFont /DejaVuSans /Encoding /Identity-H " +
                "/DescendantFonts [4 0 R] /ToUnicode 7 0 R >>");
        out.add("<< /Type /Font /Subtype /CIDFontType2 /BaseFont /DejaVuSans " +
                "/CIDSystemInfo << /Registry (Adobe) /Ordering (Identity) /Supplement 0 >> " +
                "/FontDescriptor 5 0 R /DW " + (advances.length > 0 ? (advances[0] * 1000 / unitsPerEm) : 1000) + " /CIDToGIDMap /Identity >>");

        int[] head = tableDirectory(ttf);
        int xMin = s16(ttf, head[0] + 36), yMin = s16(ttf, head[0] + 38);
        int xMax = s16(ttf, head[0] + 40), yMax = s16(ttf, head[0] + 42);
        out.add("<< /Type /FontDescriptor /FontName /DejaVuSans /Flags 32 " +
                "/FontBBox [" + xMin + " " + yMin + " " + xMax + " " + yMax + "] /ItalicAngle 0 " +
                "/Ascent " + ascent + " /Descent " + descent + " /CapHeight " + ascent + " /StemV 80 " +
                "/FontFile2 6 0 R >>");

        out.add("<< /Length " + ttf.length + " /Length1 " + ttf.length + " >>\nstream\n"
                + new String(ttf, StandardCharsets.ISO_8859_1) + "\nendstream");

        out.add("<< /Length " + usedCMap().length() + " >>\nstream\n" + usedCMap() + "\nendstream");

        out.add("<< /Producer (TaPTar) /Title " + pdfHexString(title) + " " +
                "/CreationDate (D:20260819120000Z) >>");

        for (int i = 0; i < pages.size(); i++) {
            int pObj = pageObjs.get(i);
            int cObj = pObj + 1;
            out.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_W + " " + PAGE_H + "] " +
                    "/Resources << /Font << /F1 3 0 R >> >> /Contents " + cObj + " 0 R >>");
            String contentStr = pages.get(i);
            out.add("<< /Length " + contentStr.length() + " >>\nstream\n" + contentStr + "\nendstream");
        }

        StringBuilder sb = new StringBuilder("%PDF-1.4\n");
        int totalObjs = out.size() + 1; // +1 for free object 0
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int i = 0; i < out.size(); i++) {
            offsets.add(sb.length());
            sb.append(i + 1).append(" 0 obj\n").append(out.get(i)).append("\nendobj\n");
        }

        long xrefOffset = sb.length();
        sb.append("xref\n0 ").append(totalObjs).append("\n");
        sb.append("0000000000 65535 f \n");
        for (int i = 1; i < totalObjs; i++) {
            sb.append(String.format("%010d 00000 n \n", offsets.get(i)));
        }
        sb.append("trailer\n<< /Size ").append(totalObjs).append(" /Root 1 0 R /Info 8 0 R >>\n");
        sb.append("startxref\n").append(xrefOffset).append("\n%%EOF");
        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private String usedCMap() {
        StringBuilder cm = new StringBuilder();
        cm.append("/CIDInit /ProcSet findresource begin\n12 dict begin\nbegincmap\n");
        cm.append("/CIDSystemInfo << /Registry (Adobe) /Ordering (UCS) /Supplement 0 >> def\n");
        cm.append("/CMapName /Adobe-Identity-UCS def\n/CMapType 2 def\n");
        cm.append("1 begincodespacerange\n<0000> <FFFF>\nendcodespacerange\n");
        cm.append(glyphToUnicode.size()).append(" beginbfchar\n");
        for (Map.Entry<Integer, Integer> e : glyphToUnicode.entrySet()) {
            cm.append(String.format("<%04X> <%04X>\n", e.getKey(), e.getValue()));
        }
        cm.append("endbfchar\nendcmap\nCMapName currentdict /CMap defineresource pop\nend\nend\n");
        return cm.toString();
    }

    private String pdfHexString(String s) throws Exception {
        byte[] utf16 = s.getBytes("UTF-16BE");
        StringBuilder sb = new StringBuilder("<FEFF");
        for (byte bb : utf16) sb.append(String.format("%02X", bb & 0xFF));
        sb.append(">");
        return sb.toString();
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static int u16(byte[] t, int off) {
        return ((t[off] & 0xFF) << 8) | (t[off + 1] & 0xFF);
    }

    private static int s16(byte[] t, int off) {
        int v = u16(t, off);
        return v >= 0x8000 ? v - 0x10000 : v;
    }

    private static int u32(byte[] t, int off) {
        return ((t[off] & 0xFF) << 24) | ((t[off + 1] & 0xFF) << 16)
                | ((t[off + 2] & 0xFF) << 8) | (t[off + 3] & 0xFF);
    }
}
