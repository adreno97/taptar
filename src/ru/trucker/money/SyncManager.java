package ru.trucker.money;

import android.app.Activity;
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
    private static final String OBJECT = "backup.json";

    public interface Callback {
        void done(boolean ok, String msg);
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

    /** Синхронизировать (загрузить снимок в облако). */
    public static void sync(final Activity a, final Callback cb) {
        final Context app = a.getApplicationContext();
        thread(new Runnable() {
            @Override public void run() {
                String msg; boolean ok = false;
                try {
                    String jwt = login(app);
                    DbHelper db = new DbHelper(app);
                    String json = exportSnapshot(app, db);
                    upload(app, json, jwt);
                    setLastSync(app);
                    ok = true;
                    msg = "Синхронизировано " + lastSyncText(app);
                } catch (Exception e) {
                    msg = "Ошибка синхронизации: " + e.getMessage() + " (bucket: " + BUCKET + ")";
                }
                post(a, cb, ok, msg);
            }
        });
    }

    /** Восстановить данные из облака (заменяет всё на устройстве). */
    public static void restore(final Activity a, final Callback cb) {
        final Context app = a.getApplicationContext();
        thread(new Runnable() {
            @Override public void run() {
                String msg; boolean ok = false;
                try {
                    String jwt = login(app);
                    String json = download(app, jwt);
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
                post(a, cb, ok, msg);
            }
        });
    }

    /** Проверка: есть ли резервная копия в облаке. */
    public static void hasBackup(final Activity a, final Callback cb) {
        final Context app = a.getApplicationContext();
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
                post(a, cb, ok, msg);
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

    private static void upload(Context c, String json, String jwt) throws Exception {
        HttpURLConnection conn = conn(SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + OBJECT, "PUT", jwt, null);
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setRequestProperty("x-upsert", "true");
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(json.getBytes(StandardCharsets.UTF_8));
        os.close();
        read(conn);
    }

    private static String download(Context c, String jwt) throws Exception {
        HttpURLConnection conn = conn(SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + OBJECT, "GET", jwt, null);
        int code = conn.getResponseCode();
        if (code == 404) {
            conn.disconnect();
            return null;
        }
        String s = read(conn);
        return s;
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

    private static void post(Activity a, final Callback cb, final boolean ok, final String msg) {
        if (cb == null) return;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override public void run() {
                cb.done(ok, msg);
            }
        });
    }
}
