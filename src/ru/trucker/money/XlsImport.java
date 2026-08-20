package ru.trucker.money;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Reader for legacy .xls (BIFF8) files. */
public class XlsImport {

    public static List<XlsxImport.Row> parse(InputStream in) throws Exception {
        byte[] all = readAll(in);
        int bof = findGlobalsBof(all);
        if (bof < 0) throw new Exception("Не найден реестр в файле .xls");
        String[][] table = parseBiff8(all, bof);
        return XlsxImport.mapRows(table);
    }

    /** Locates the workbook-globals BOF record by scanning bytes. */
    private static int findGlobalsBof(byte[] b) {
        for (int i = 0; i + 8 <= b.length; i++) {
            if (b[i] == 0x09 && b[i + 1] == 0x08) {
                int len = u16(b, i + 2);
                if (i + 4 + 4 <= b.length) {
                    int sub = u16(b, i + 4 + 2);
                    if (sub == 0x0005) return i;
                }
            }
        }
        return -1;
    }

    private static byte[] readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        return b.toByteArray();
    }

    private static String[][] parseBiff8(byte[] wb, int startPos) throws Exception {
        List<String> sst = new ArrayList<>();
        List<Map<Long, String>> sheets = new ArrayList<>();
        Map<Long, String> cur = null;

        int pos = startPos;
        while (pos + 4 <= wb.length) {
            int op = u16(wb, pos);
            int len = u16(wb, pos + 2);
            int start = pos + 4;
            if (start + len > wb.length) break;
            // EOF (0x000A) ends a substream; parsing continues into the next one.
            switch (op) {
                case 0x0809: { // BOF
                    if (len >= 4) {
                        int type = u16(wb, start + 2);
                        if (type == 0x0010) {
                            cur = new HashMap<>();
                            sheets.add(cur);
                        }
                    }
                    break;
                }
                case 0x00FC: parseSst(wb, start, len, sst); break;
                case 0x00FD: { // LABELSST
                    if (cur != null && len >= 10) {
                        int row = u16(wb, start);
                        int col = u16(wb, start + 2);
                        long isst = u32(wb, start + 6);
                        String v = isst >= 0 && isst < sst.size() ? sst.get((int) isst) : "";
                        set(cur, row, col, v);
                    }
                    break;
                }
                case 0x0203: { // NUMBER
                    if (cur != null && len >= 14) {
                        int row = u16(wb, start);
                        int col = u16(wb, start + 2);
                        double v = leDouble(wb, start + 6);
                        set(cur, row, col, fmtNum(v));
                    }
                    break;
                }
                case 0x027E: { // RK
                    if (cur != null && len >= 10) {
                        int row = u16(wb, start);
                        int col = u16(wb, start + 2);
                        long rk = u32(wb, start + 6);
                        set(cur, row, col, fmtNum(rkDecode(rk)));
                    }
                    break;
                }
                case 0x00BD: { // MULRK
                    if (cur != null && len >= 6) {
                        int row = u16(wb, start);
                        int col = u16(wb, start + 2);
                        int off = start + 4;
                        int c = col;
                        while (off + 6 <= start + len - 2) {
                            long rk = u32(wb, off + 2);
                            set(cur, row, c, fmtNum(rkDecode(rk)));
                            off += 6;
                            c++;
                        }
                    }
                    break;
                }
                case 0x0201: { // BLANK
                    if (cur != null && len >= 6) {
                        int row = u16(wb, start);
                        int col = u16(wb, start + 2);
                        set(cur, row, col, "");
                    }
                    break;
                }
                case 0x00BE: { // MULBLANK
                    if (cur != null && len >= 4) {
                        int row = u16(wb, start);
                        int col = u16(wb, start + 2);
                        int off = start + 4;
                        int c = col;
                        while (off + 4 <= start + len - 2) {
                            set(cur, row, c, "");
                            off += 4;
                            c++;
                        }
                    }
                    break;
                }
                case 0x0006: { // FORMULA (cached value in last 8 bytes)
                    if (cur != null && len >= 22) {
                        int row = u16(wb, start);
                        int col = u16(wb, start + 2);
                        int cached = start + len - 8;
                        byte t = wb[cached];
                        if (t == 0) {
                            double v = leDouble(wb, cached + 2);
                            set(cur, row, col, fmtNum(v));
                        } else if (t == 0x02) {
                            set(cur, row, col, "");
                        } else if (t == 0x01) {
                            set(cur, row, col, wb[cached + 2] == 0 ? "0" : "1");
                        } else if (t == 0xFF) {
                            long isst = u16(wb, cached + 2);
                            if (isst == 0xFFFF) {
                                // inline string
                                int sp = cached + 4;
                                int grbit = u16(wb, sp);
                                sp += 2;
                                int cch = u16(wb, sp);
                                sp += 2;
                                boolean hi = (grbit & 0x01) != 0;
                                String s = hi
                                        ? new String(wb, sp, cch * 2, java.nio.charset.StandardCharsets.UTF_16LE)
                                        : new String(wb, sp, cch, Charset.forName("windows-1251"));
                                set(cur, row, col, s);
                            } else if (isst >= 0 && isst < sst.size()) {
                                set(cur, row, col, sst.get((int) isst));
                            }
                        }
                    }
                    break;
                }
                case 0x0205: { // BOOLERR
                    if (cur != null && len >= 8) {
                        int row = u16(wb, start);
                        int col = u16(wb, start + 2);
                        set(cur, row, col, String.valueOf(wb[start + 6]));
                    }
                    break;
                }
                default:
                    break;
            }
            pos = start + len;
        }

        // choose sheet with most cells
        Map<Long, String> best = null;
        int bestCount = -1;
        for (Map<Long, String> s : sheets) {
            if (s.size() > bestCount) {
                bestCount = s.size();
                best = s;
            }
        }
        if (best == null) return new String[0][0];
        return toTable(best);
    }

    private static String[][] toTable(Map<Long, String> cells) {
        TreeMap<Integer, TreeMap<Integer, String>> rows = new TreeMap<>();
        for (Map.Entry<Long, String> e : cells.entrySet()) {
            long key = e.getKey();
            int row = (int) (key / 10000);
            int col = (int) (key % 10000);
            TreeMap<Integer, String> r = rows.get(row);
            if (r == null) {
                r = new TreeMap<>();
                rows.put(row, r);
            }
            r.put(col, e.getValue());
        }
        if (rows.isEmpty()) return new String[0][0];
        int maxRow = rows.lastKey();
        int maxCol = 0;
        for (TreeMap<Integer, String> r : rows.values()) {
            if (!r.isEmpty()) maxCol = Math.max(maxCol, r.lastKey());
        }
        String[][] table = new String[maxRow + 1][maxCol + 1];
        for (int r = 0; r < table.length; r++) {
            java.util.Arrays.fill(table[r], "");
        }
        for (Map.Entry<Integer, TreeMap<Integer, String>> e : rows.entrySet()) {
            for (Map.Entry<Integer, String> c : e.getValue().entrySet()) {
                table[e.getKey()][c.getKey()] = c.getValue();
            }
        }
        return table;
    }

    private static void set(Map<Long, String> cells, int row, int col, String v) {
        cells.put(row * 10000L + col, v == null ? "" : v.trim());
    }

    private static void parseSst(byte[] b, int start, int len, List<String> sst) {
        if (len < 8) return;
        List<String> variant = trySstVariant(b, start, len);
        if (variant != null) {
            sst.addAll(variant);
            return;
        }
        // standard BIFF8 rich/extended strings
        int pos = start + 8;
        long cst = u32(b, start + 4);
        for (long u = 0; u < cst && pos + 4 <= start + len; u++) {
            int grbit = u16(b, pos);
            int cch = u16(b, pos + 2);
            pos += 4;
            boolean ext = (grbit & 0x04) != 0;
            boolean rich = (grbit & 0x08) != 0;
            if (ext && pos + 4 <= start + len) pos += 4;
            if (rich && pos + 2 <= start + len) {
                int runs = u16(b, pos);
                pos += 2 + runs * 4;
            }
            boolean hi = (grbit & 0x01) != 0;
            String s;
            if (hi) {
                if (pos + cch * 2 > start + len) break;
                s = new String(b, pos, cch * 2, java.nio.charset.StandardCharsets.UTF_16LE);
                pos += cch * 2;
            } else {
                if (pos + cch > start + len) break;
                s = new String(b, pos, cch, Charset.forName("windows-1251"));
                pos += cch;
            }
            sst.add(s);
        }
    }

    /** Some non-standard writers store strings as: length(u16) + flag(u8) + UTF-16 chars. */
    private static List<String> trySstVariant(byte[] b, int start, int len) {
        long cst = u32(b, start + 4);
        int pos = start + 8;
        List<String> out = new ArrayList<>();
        for (long u = 0; u < cst; u++) {
            if (pos + 3 > start + len) return null;
            int cch = u16(b, pos);
            int flag = b[pos + 2] & 0xFF;
            if (cch < 0 || cch > 20000) return null;
            int byteLen = (flag & 0x01) != 0 ? cch * 2 : cch;
            if (pos + 3 + byteLen > start + len) return null;
            String s = (flag & 0x01) != 0
                    ? new String(b, pos + 3, byteLen, java.nio.charset.StandardCharsets.UTF_16LE)
                    : new String(b, pos + 3, byteLen, Charset.forName("windows-1251"));
            out.add(s);
            pos += 3 + byteLen;
        }
        // must consume the whole SST record
        if (pos != start + len) return null;
        return out;
    }

    private static double rkDecode(long rk) {
        boolean div100 = (rk & 0x02) != 0;
        boolean isInt = (rk & 0x01) != 0;
        double num;
        if (isInt) {
            num = (rk >> 2) / 100.0;
        } else {
            long bits = rk & 0xFFFFFFFCL;
            num = Double.longBitsToDouble(bits << 32);
        }
        return div100 ? num / 100.0 : num;
    }

    private static String fmtNum(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "";
        if (v == Math.rint(v) && Math.abs(v) < 1e15) {
            return String.valueOf((long) v);
        }
        return BigDecimal.valueOf(v).toPlainString();
    }

    private static double leDouble(byte[] b, int off) {
        long bits = 0;
        for (int i = 0; i < 8; i++) {
            bits |= ((long) (b[off + i] & 0xFF)) << (8 * i);
        }
        return Double.longBitsToDouble(bits);
    }

    private static int u16(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
    }

    private static long u32(byte[] b, int off) {
        return (b[off] & 0xFFL) | ((b[off + 1] & 0xFFL) << 8)
                | ((b[off + 2] & 0xFFL) << 16) | ((b[off + 3] & 0xFFL) << 24);
    }
}
