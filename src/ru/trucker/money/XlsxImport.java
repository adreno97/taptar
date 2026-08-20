package ru.trucker.money;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class XlsxImport {

    public static class Row {
        public String number;
        public long date;
        public int zone;
        public boolean isReturn;
        public int numPoints;     // total unload points = 3 + extra
        public int extraCount;
        public long extraPriceKop; // price from "N*price" if present, else 0
        public long baseKop;
        public long revenueKop;
        public long fuelKop;      // |штраф| = топливо за реестр
    }

    public static List<Row> parse(InputStream in) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        ZipInputStream zip = new ZipInputStream(in);
        ZipEntry e;
        while ((e = zip.getNextEntry()) != null) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = zip.read(buf)) > 0) b.write(buf, 0, n);
            entries.put(e.getName(), b.toByteArray());
        }
        zip.close();

        List<String> shared = readSharedStrings(entries.get("xl/sharedStrings.xml"));

        byte[] sheetBytes = entries.get("xl/worksheets/sheet1.xml");
        if (sheetBytes == null) {
            for (Map.Entry<String, byte[]> en : entries.entrySet()) {
                if (en.getKey().startsWith("xl/worksheets/") && en.getKey().endsWith(".xml")) {
                    sheetBytes = en.getValue();
                    break;
                }
            }
        }
        if (sheetBytes == null) throw new Exception("В файле не найдены листы");

        String[][] table = readSheet(sheetBytes, shared);
        return mapRows(table);
    }

    private static List<String> readSharedStrings(byte[] bytes) throws Exception {
        List<String> out = new ArrayList<>();
        if (bytes == null) return out;
        Document doc = parseDoc(bytes);
        NodeList sis = doc.getElementsByTagName("si");
        for (int i = 0; i < sis.getLength(); i++) {
            out.add(sis.item(i).getTextContent());
        }
        return out;
    }

    private static String[][] readSheet(byte[] bytes, List<String> shared) throws Exception {
        Document doc = parseDoc(bytes);
        NodeList rows = doc.getElementsByTagName("row");
        List<String[]> list = new ArrayList<>();
        for (int i = 0; i < rows.getLength(); i++) {
            Element row = (Element) rows.item(i);
            NodeList cells = row.getElementsByTagName("c");
            Map<Integer, String> vals = new HashMap<>();
            int maxCol = -1;
            for (int j = 0; j < cells.getLength(); j++) {
                Element c = (Element) cells.item(j);
                int col = colIndex(c.getAttribute("r"));
                if (col < 0) col = j;
                String t = c.getAttribute("t");
                String val = "";
                if ("s".equals(t)) {
                    NodeList v = c.getElementsByTagName("v");
                    if (v.getLength() > 0) {
                        int idx = (int) Double.parseDouble(v.item(0).getTextContent().trim());
                        if (idx >= 0 && idx < shared.size()) val = shared.get(idx);
                    }
                } else if ("inlineStr".equals(t)) {
                    NodeList is = c.getElementsByTagName("t");
                    if (is.getLength() > 0) val = is.item(0).getTextContent();
                } else {
                    NodeList v = c.getElementsByTagName("v");
                    if (v.getLength() > 0) val = v.item(0).getTextContent();
                }
                vals.put(col, val.trim());
                if (col > maxCol) maxCol = col;
            }
            if (maxCol < 0) continue;
            String[] arr = new String[maxCol + 1];
            for (int k = 0; k < arr.length; k++) arr[k] = vals.containsKey(k) ? vals.get(k) : "";
            list.add(arr);
        }
        return list.toArray(new String[0][]);
    }

    private static int colIndex(String ref) {
        if (ref == null || ref.isEmpty()) return -1;
        int idx = 0;
        for (int i = 0; i < ref.length(); i++) {
            char ch = ref.charAt(i);
            if (ch >= 'A' && ch <= 'Z') idx = idx * 26 + (ch - 'A' + 1);
            else break;
        }
        return idx - 1;
    }

    private static Document parseDoc(byte[] bytes) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        try {
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {
        }
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new ByteArrayInputStream(bytes));
    }

    private static List<Row> mapRows(String[][] table) {
        List<Row> out = new ArrayList<>();
        int headerRow = -1;
        int nCol = -1, dCol = -1, rCol = -1, bCol = -1, xCol = -1, retCol = -1, fCol = -1;
        for (int i = 0; i < table.length; i++) {
            for (int c = 0; c < table[i].length; c++) {
                String h = table[i][c].trim();
                if ("Номер СЛ".equals(h)) { nCol = c; headerRow = i; }
                if ("Дата погрузки".equals(h) || "Дата".equals(h)) dCol = c;
                if ("Маршрут".equals(h)) rCol = c;
                if ("Рейс".equals(h)) bCol = c;
                if ("Доп.магазин".equals(h) || "Доп.точки".equals(h) || "Точки выгрузки".equals(h)) xCol = c;
                if ("Возврат".equals(h)) retCol = c;
                if ("Штраф".equals(h)) fCol = c;
            }
            if (headerRow >= 0) break;
        }
        if (headerRow < 0 || nCol < 0) return out;

        for (int i = headerRow + 1; i < table.length; i++) {
            String[] row = table[i];
            String number = cell(row, nCol);
            if (number.isEmpty()) continue;
            Row r = new Row();
            r.number = number;

            long date = 0;
            if (dCol >= 0) date = parseDate(cell(row, dCol));
            r.date = date > 0 ? date : System.currentTimeMillis();

            String route = rCol >= 0 ? cell(row, rCol) : "";
            r.zone = zoneFromRoute(route);

            double base = bCol >= 0 ? toDouble(cell(row, bCol)) : 0;
            r.baseKop = Math.round(base * 100.0);

            double ret = retCol >= 0 ? toDouble(cell(row, retCol)) : 0;
            r.isReturn = ret > 0;

            String extra = xCol >= 0 ? cell(row, xCol) : "";
            int[] ep = pointsFromExtra(extra);
            r.extraCount = ep[0];
            r.extraPriceKop = ep[1] * 100L;
            r.numPoints = 3 + r.extraCount;

            double fuel = fCol >= 0 ? toDouble(cell(row, fCol)) : 0;
            r.fuelKop = Math.round(Math.abs(fuel) * 100.0);

            // revenue = base + return(+50%) + extra points; final sum in the file is not used
            // because it may already include the fuel deduction
            long rev = r.baseKop + (r.isReturn ? r.baseKop / 2 : 0);
            if (r.extraCount > 0 && r.extraPriceKop > 0) {
                rev += r.extraCount * r.extraPriceKop;
            }
            r.revenueKop = rev;
            out.add(r);
        }
        return out;
    }

    private static String cell(String[] row, int col) {
        if (col < 0 || col >= row.length) return "";
        return row[col].trim();
    }

    private static double toDouble(String s) {
        try {
            return Double.parseDouble(s.replace(',', '.'));
        } catch (Exception e) {
            return 0;
        }
    }

    private static long parseDate(String s) {
        try {
            Matcher m = Pattern.compile("(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})").matcher(s);
            if (m.find()) {
                int d = Integer.parseInt(m.group(1));
                int mo = Integer.parseInt(m.group(2)) - 1;
                int y = Integer.parseInt(m.group(3));
                java.util.Calendar c = java.util.Calendar.getInstance();
                c.set(y, mo, d, 0, 0, 0);
                c.set(java.util.Calendar.MILLISECOND, 0);
                return c.getTimeInMillis();
            }
            double serial = Double.parseDouble(s.replace(',', '.'));
            if (serial > 10000 && serial < 60000) {
                java.util.Calendar c = java.util.Calendar.getInstance();
                c.setTimeInMillis(Math.round((serial - 25569) * 86400000L));
                return c.getTimeInMillis();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static int zoneFromRoute(String route) {
        try {
            Matcher m = Pattern.compile("(\\d+)").matcher(route);
            int up = -1;
            while (m.find()) up = Math.max(up, Integer.parseInt(m.group(1)));
            if (up < 0) return 0;
            if (up <= 50) return 0;
            if (up <= 100) return 1;
            if (up <= 150) return 2;
            if (up <= 200) return 3;
            if (up <= 300) return 4;
            return 5;
        } catch (Exception e) {
            return 0;
        }
    }

    /** Returns {count, priceRubles} from extra cell like "1*600". */
    private static int[] pointsFromExtra(String extra) {
        int count = 0, price = 0;
        try {
            Matcher m = Pattern.compile("(\\d+)\\s*\\*\\s*(\\d+)").matcher(extra);
            if (m.find()) {
                count = Integer.parseInt(m.group(1));
                price = Integer.parseInt(m.group(2));
                return new int[]{count, price};
            }
            Matcher m2 = Pattern.compile("(\\d+)").matcher(extra);
            if (m2.find()) count = Integer.parseInt(m2.group(1));
        } catch (Exception ignored) {
        }
        return new int[]{count, price};
    }
}
