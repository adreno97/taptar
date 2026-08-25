package ru.trucker.money;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Облачная синхронизация (резервная копия) в Supabase Storage. */
public class SyncManager {

    private static final String PREFS = "cloud";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASS = "password";
    private static final String KEY_LAST = "last_sync";

    private static final String SUPABASE_URL = "https://uywryoxjdvjcsmmjjhlk.supabase.co";
    private static final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV5d3J5b3hqZHZqY3NtbWpqaGxrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODc2MzU5MjksImV4cCI6MjEwMzIxMTkyOX0.gbz6ryzeZLQ6gPsmn3u4bTxziQztcBDVf1jd-lNInqY";
    private static final String BUCKET = "taptar-buckup";
    public static final String OBJECT = "backup.json";
    private static final String SNAP_PREFIX = "snap-";
    private static final int MAX_SNAPS = 60;

    public interface Callback {
        void done(boolean ok, String msg);
    }

    public interface ListCallback {
        void done(boolean ok, String msg, String listJson);
    }

    private SyncManager() {}

    public static String email(Context c) { return c.getSharedPreferences(PREFS, 0).getString(KEY_EMAIL, ""); }
    public static String password(Context c) { return c.getSharedPreferences(PREFS, 0).getString(KEY_PASS, ""); }

    public static void setCredentials(Context c, String email, String password) {
        c.getSharedPreferences(PREFS, 0).edit()
                .putString(KEY_EMAIL, email == null ? "" : email.trim())
                .putString(KEY_PASS, password == null ? "" : password)
                .apply();
    }

    public static boolean hasCredentials(Context c) {
        return !email(c).isEmpty() && !password(c).isEmpty();
    }

    public static void setLastSync(Context c) {
        c.getSharedPreferences(PREFS, 0).edit().putLong(KEY_LAST, System.currentTimeMillis()).apply();
    }

    public static String lastSyncText(Context c) {
        long t = c.getSharedPreferences(PREFS, 0).getLong(KEY_LAST, 0);
        return t > 0 ? android.text.format.DateFormat.format("dd.MM.yyyy HH:mm", t).toString() : "";
    }

    /** Полный синк: уникальная копия (история) + текущая backup.json + обрезка старых. */
    public static void sync(final Context a, final Callback cb) {
        final Context app = a;
        thread(new Runnable() {
            @Override public void run() {
                String msg; boolean ok = false;
                try {
                    String jwt = login(app);
                    DbHelper db = new DbHelper(app);
                    String json = exportSnapshot(app, db);
                    uploadObject(app, json, jwt, snapName(), true);
                    uploadObject(app, json, jwt, OBJECT, true);
                    prune(app, jwt);
                    setLastSync(app);
                    ok = true;
                    msg = "Синхронизировано " + lastSyncText(app);
                } catch (Exception e) {
                    msg = "Ошибка синхронизации: " + e.getMessage() + " (bucket: " + BUCKET + ")";
                }
                post(app, cb, ok, msg);
            }
        });
    }

    /** Быстрый синк: только текущая копия backup.json, без файлов истории. */
    public static void syncLatest(final Context a, final Callback cb) {
        final Context app = a;
        thread(new Runnable() {
            @Override public void run() {
                String msg; boolean ok = false;
                try {
                    String jwt = login(app);
                    DbHelper db = new DbHelper(app);
                    String json = exportSnapshot(app, db);
                    uploadObject(app, json, jwt, OBJECT, true);
                    setLastSync(app);
                    ok = true;
                    msg = "Синхронизировано " + lastSyncText(app);
                } catch (Exception e) {
                    msg = "Ошибка синхронизации: " + e.getMessage() + " (bucket: " + BUCKET + ")";
                }
                post(app, cb, ok, msg);
            }
        });
    }

    /** Восстановить последнюю копию (заменяет всё на устройстве). */
    public static void restore(final Context a, final Callback cb) {
        restoreObject(a, OBJECT, cb);
    }

    /** Восстановить конкретную копию по имени объекта. */
    public static void restoreObject(final Context a, final String objectName, final Callback cb) {
        final Context app = a;
        thread(new Runnable() {
            @Override public void run() {
                String msg; boolean ok = false;
                try {
                    String jwt = login(app);
                    String json = downloadObject(app, jwt, objectName);
                    if (json == null) {
                        msg = "В облаке нет резервной копии";
                    } else {
                        DbHelper db = new DbHelper(app);
                        db.clearAll();
                        applySnapshot(app, db, json);
                        setLastSync(app);
                        ok = true;
                        msg = "Данные восстановлены " + lastSyncText(app);
                    }
                } catch (Exception e) {
                    msg = "Ошибка восстановления: " + e.getMessage() + " (bucket: " + BUCKET + ")";
                }
                post(app, cb, ok, msg);
            }
        });
    }

    /** Проверка: есть ли резервная копия в облаке. */
    public static void hasBackup(final Context a, final Callback cb) {
        final Context app = a;
        thread(new Runnable() {
            @Override public void run() {
                boolean ok = false; String msg = "";
                try {
                    String jwt = login(app);
                    HttpURLConnection conn = conn(SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + OBJECT, "GET", jwt, null);
                    int code = conn.getResponseCode();
                    conn.disconnect();
                    ok = code == 200;
                    msg = code == 404 ? "копии нет" : ("HTTP " + code);
                } catch (Exception e) {
                    msg = e.getMessage();
                }
                post(app, cb, ok, msg);
            }
        });
    }

    /** Список доступных копий (снапшотов) по убыванию даты. listJson — JSON-массив {name,label}. */
    public static void listBackups(final Context a, final ListCallback cb) {
        final Context app = a;
        thread(new Runnable() {
            @Override public void run() {
                String msg = ""; String listJson = "[]";
                boolean ok = false;
                try {
                    String jwt = login(app);
                    JSONArray arr = listObjects(app, jwt, SNAP_PREFIX);
                    JSONArray out = new JSONArray();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject item = new JSONObject();
                        item.put("name", arr.getJSONObject(i).optString("name"));
                        item.put("label", snapLabel(item.optString("name")));
                        out.put(item);
                    }
                    listJson = out.toString();
                    ok = true;
                } catch (Exception e) {
                    msg = e.getMessage();
                }
                postList(app, cb, ok, msg, listJson);
            }
        });
    }

    // ---------- сеть ----------

    private static String login(Context c) throws Exception {
        JSONObject body = new JSONObject();
        body.put("email", email(c));
        body.put("password", password(c));
        String url = SUPABASE_URL + "/auth/v1/token?grant_type=password";
        HttpURLConnection conn = conn(url, "POST", null, null);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        os.close();
        String resp = read(conn);
        JSONObject j = new JSONObject(resp);
        if (!j.has("access_token")) {
            String err = j.optString("msg", j.optString("error_description", ""));
            throw new Exception(err.isEmpty() ? "Не удалось войти" : err);
        }
        return j.getString("access_token");
    }

    private static void uploadObject(Context c, String json, String jwt, String objectName, boolean upsert) throws Exception {
        HttpURLConnection conn = conn(SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + objectName, "PUT", jwt, null);
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        if (upsert) conn.setRequestProperty("x-upsert", "true");
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(json.getBytes(StandardCharsets.UTF_8));
        os.close();
        read(conn);
    }

    private static String downloadObject(Context c, String jwt, String objectName) throws Exception {
        HttpURLConnection conn = conn(SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + objectName, "GET", jwt, null);
        int code = conn.getResponseCode();
        if (code == 404) {
            conn.disconnect();
            return null;
        }
        return read(conn);
    }

    private static JSONArray listObjects(Context c, String jwt, String prefix) throws Exception {
        String url = SUPABASE_URL + "/storage/v1/object/list/" + BUCKET;
        HttpURLConnection conn = conn(url, "POST", jwt, "application/json");
        conn.setDoOutput(true);
        JSONObject body = new JSONObject();
        body.put("prefix", prefix);
        body.put("limit", 500);
        body.put("offset", 0);
        JSONObject sort = new JSONObject();
        sort.put("column", "name");
        sort.put("order", "desc");
        body.put("sortBy", sort);
        OutputStream os = conn.getOutputStream();
        os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        os.close();
        String resp = read(conn);
        return new JSONArray(resp);
    }

    /** Хранить не больше MAX_SNAPS копий — старые удаляются. */
    private static void prune(Context c, String jwt) throws Exception {
        JSONArray arr = listObjects(c, jwt, SNAP_PREFIX);
        if (arr.length() <= MAX_SNAPS) return;
        for (int i = MAX_SNAPS; i < arr.length(); i++) {
            String name = arr.getJSONObject(i).optString("name");
            try {
                HttpURLConnection conn = conn(SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + name, "DELETE", jwt, null);
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception ignored) {
            }
        }
    }

    private static String snapName() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US);
        return SNAP_PREFIX + sdf.format(new java.util.Date()) + ".json";
    }

    private static String snapLabel(String name) {
        try {
            String base = name.replace(SNAP_PREFIX, "").replace(".json", "");
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US);
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.US);
            return out.format(in.parse(base));
        } catch (Exception e) {
            return name;
        }
    }

    private static HttpURLConnection conn(String urlStr, String method, String jwt, String contentType) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setRequestMethod(method);
        conn.setRequestProperty("apikey", ANON_KEY);
        if (jwt != null) conn.setRequestProperty("Authorization", "Bearer " + jwt);
        if (contentType != null) conn.setRequestProperty("Content-Type", contentType);
        return conn;
    }

    private static String read(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        String body = readAll(in);
        conn.disconnect();
        if (code < 200 || code >= 300) {
            String err = "";
            try {
                err = new JSONObject(body).optString("message", "");
            } catch (Exception ignored) {}
            if (err.isEmpty()) err = body.isEmpty() ? "HTTP " + code : body;
            throw new Exception(err);
        }
        return body;
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    // ---------- снимок ----------

    static String exportSnapshot(Context c, DbHelper db) throws Exception {
        JSONObject root = new JSONObject();
        root.put("app", "taptar");
        root.put("version", 1);

        JSONArray trips = new JSONArray();
        for (DbHelper.Record r : db.dumpTrips()) {
            JSONObject o = new JSONObject();
            o.put("id", r.id);
            o.put("number", r.number == null ? "" : r.number);
            o.put("date", r.date);
            o.put("zone", r.zone);
            o.put("is_return", r.isReturn);
            o.put("base_price", r.basePrice);
            o.put("num_points", r.numPoints);
            o.put("revenue", r.amount);
            o.put("note", r.note == null ? "" : r.note);
            trips.put(o);
        }
        root.put("trips", trips);

        JSONArray expenses = new JSONArray();
        for (DbHelper.Record r : db.dumpExpenses()) {
            JSONObject o = new JSONObject();
            o.put("id", r.id);
            o.put("date", r.date);
            o.put("category", r.category == null ? "" : r.category);
            o.put("amount", r.amount);
            o.put("note", r.note == null ? "" : r.note);
            o.put("liters", r.liters);
            o.put("price_per_liter", r.pricePerLiter);
            o.put("mileage", r.mileage);
            o.put("discount", r.discount);
            expenses.put(o);
        }
        root.put("expenses", expenses);

        JSONArray maint = new JSONArray();
        for (DbHelper.Maint m : db.getMaintAll()) {
            JSONObject o = new JSONObject();
            o.put("id", m.id);
            o.put("date", m.date);
            o.put("mileage", m.mileage);
            o.put("works", m.works == null ? "" : m.works);
            maint.put(o);
        }
        root.put("maintenance", maint);

        JSONObject settings = new JSONObject();
        settings.put("zone_count", Zones.getCount(c));
        long[] prices = Zones.getPrices(c);
        JSONArray pa = new JSONArray();
        for (long p : prices) pa.put(p);
        settings.put("zone_prices", pa);
        settings.put("extra_price", Zones.getExtraPrice(c));
        settings.put("extra_start", Zones.getExtraStart(c));
        settings.put("num_trips", c.getSharedPreferences("app", 0).getBoolean("num_trips", false));
        settings.put("dark_theme", Ui.dark(c));
        settings.put("remind_enabled", Reminders.isEnabled(c));
        settings.put("remind_mileage", Reminders.currentMileage(c));
        settings.put("remind_interval", Reminders.intervalKm(c));
        settings.put("remind_next_date", Reminders.nextDate(c));
        root.put("settings", settings);

        return root.toString();
    }

    static void applySnapshot(Context c, DbHelper db, String json) throws Exception {
        JSONObject root = new JSONObject(json);
        if (!"taptar".equals(root.optString("app"))) {
            throw new Exception("Неверный файл резервной копии");
        }

        JSONArray trips = root.optJSONArray("trips");
        if (trips != null) {
            for (int i = 0; i < trips.length(); i++) {
                JSONObject o = trips.getJSONObject(i);
                db.addTrip(o.optString("number"), o.optLong("date"), o.optInt("zone"),
                        o.optBoolean("is_return"), o.optLong("base_price"), o.optInt("num_points"),
                        o.optLong("revenue"), o.optString("note"));
            }
        }

        JSONArray expenses = root.optJSONArray("expenses");
        if (expenses != null) {
            for (int i = 0; i < expenses.length(); i++) {
                JSONObject o = expenses.getJSONObject(i);
                db.addExpense(o.optLong("date"), o.optString("category"), o.optLong("amount"),
                        o.optString("note"), o.optDouble("liters"), o.optDouble("price_per_liter"),
                        o.optLong("mileage"), o.optDouble("discount"));
            }
        }

        JSONArray maint = root.optJSONArray("maintenance");
        if (maint != null) {
            for (int i = 0; i < maint.length(); i++) {
                JSONObject o = maint.getJSONObject(i);
                db.addMaint(o.optLong("date"), o.optLong("mileage"), o.optString("works"));
            }
        }

        JSONObject s = root.optJSONObject("settings");
        if (s != null) {
            Zones.setCount(c, s.optInt("zone_count", Zones.getCount(c)));
            JSONArray pa = s.optJSONArray("zone_prices");
            if (pa != null && pa.length() > 0) {
                long[] pr = new long[pa.length()];
                for (int i = 0; i < pr.length; i++) pr[i] = pa.optLong(i);
                Zones.setPrices(c, pr);
            }
            if (s.has("extra_price")) Zones.setExtraPrice(c, s.optLong("extra_price"));
            if (s.has("extra_start")) Zones.setExtraStart(c, s.optInt("extra_start"));
            if (s.has("num_trips")) c.getSharedPreferences("app", 0).edit()
                    .putBoolean("num_trips", s.optBoolean("num_trips")).apply();
            if (s.has("dark_theme")) c.getSharedPreferences("app", 0).edit()
                    .putBoolean("dark_theme", s.optBoolean("dark_theme")).apply();
            if (s.has("remind_enabled")) Reminders.setEnabled(c, s.optBoolean("remind_enabled"));
            if (s.has("remind_mileage")) Reminders.setCurrentMileage(c, s.optLong("remind_mileage"));
            if (s.has("remind_interval")) Reminders.setIntervalKm(c, s.optLong("remind_interval"));
            if (s.has("remind_next_date")) Reminders.setNextDate(c, s.optLong("remind_next_date"));
        }
    }

    // ---------- потоки ----------

    private static void thread(Runnable r) {
        new Thread(r).start();
    }

    private static void post(Context a, final Callback cb, final boolean ok, final String msg) {
        if (cb == null) return;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override public void run() {
                cb.done(ok, msg);
            }
        });
    }

    private static void postList(Context a, final ListCallback cb, final boolean ok, final String msg, final String listJson) {
        if (cb == null) return;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override public void run() {
                cb.done(ok, msg, listJson);
            }
        });
    }
}
