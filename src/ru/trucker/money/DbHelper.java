package ru.trucker.money;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "trucker.db";
    private static final int DB_VERSION = 6;

    public static final String[] CATEGORIES = {
            "Заправка", "Платные дороги", "Ремонт", "Запчасти", "Шины",
            "ТО", "Страховка", "Налоги", "Штрафы", "Связь",
            "Питание", "Проживание", "Прочее"
    };

    public static class Record {
        public long id;
        public boolean income;
        public long date;
        public String title;
        public String number;
        public int zone;
        public boolean isReturn;
        public long basePrice;
        public int numPoints;
        public long amount; // kopecks, final revenue
        public String sub;
        public String category;
        public String note;
        public double liters;
        public double pricePerLiter;
        public long mileage;
        public double discount;
        public int seq; // display-only: trip index within payment period
    }

    public static class Maint {
        public long id;
        public long date;
        public long mileage;
        public String works;
    }

    public DbHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE trips (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "number TEXT NOT NULL," +
                "date INTEGER NOT NULL," +
                "zone INTEGER NOT NULL," +
                "is_return INTEGER NOT NULL," +
                "base_price INTEGER NOT NULL," +
                "num_points INTEGER NOT NULL," +
                "revenue INTEGER NOT NULL," +
                "note TEXT)");
        db.execSQL("CREATE TABLE expenses (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date INTEGER NOT NULL," +
                "category TEXT NOT NULL," +
                "amount INTEGER NOT NULL," +
                "note TEXT," +
                "liters REAL DEFAULT 0," +
                "price_per_liter REAL DEFAULT 0," +
                "mileage INTEGER DEFAULT 0," +
                "discount REAL DEFAULT 0)");
        db.execSQL("CREATE TABLE maintenance (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date INTEGER NOT NULL," +
                "mileage INTEGER NOT NULL," +
                "works TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 3) {
            db.execSQL("DROP TABLE IF EXISTS trips");
            db.execSQL("DROP TABLE IF EXISTS expenses");
            db.execSQL("DROP TABLE IF EXISTS maintenance");
            onCreate(db);
            return;
        }
        if (oldV < 4) {
            db.execSQL("CREATE TABLE IF NOT EXISTS maintenance (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "date INTEGER NOT NULL," +
                    "mileage INTEGER NOT NULL," +
                    "works TEXT)");
        }
        if (oldV < 5) {
            db.execSQL("ALTER TABLE expenses ADD COLUMN liters REAL DEFAULT 0");
            db.execSQL("ALTER TABLE expenses ADD COLUMN price_per_liter REAL DEFAULT 0");
            db.execSQL("ALTER TABLE expenses ADD COLUMN mileage INTEGER DEFAULT 0");
        }
        if (oldV < 6) {
            db.execSQL("ALTER TABLE expenses ADD COLUMN discount REAL DEFAULT 0");
        }
    }

    public void addTrip(String number, long date, int zone, boolean isReturn, long basePrice, int numPoints, long revenue, String note) {
        ContentValues cv = new ContentValues();
        cv.put("number", number == null ? "" : number);
        cv.put("date", date);
        cv.put("zone", zone);
        cv.put("is_return", isReturn ? 1 : 0);
        cv.put("base_price", basePrice);
        cv.put("num_points", numPoints);
        cv.put("revenue", revenue);
        cv.put("note", note == null ? "" : note);
        getWritableDatabase().insert("trips", null, cv);
    }

    public void updateTrip(long id, String number, long date, int zone, boolean isReturn, long basePrice, int numPoints, long revenue, String note) {
        ContentValues cv = new ContentValues();
        cv.put("number", number == null ? "" : number);
        cv.put("date", date);
        cv.put("zone", zone);
        cv.put("is_return", isReturn ? 1 : 0);
        cv.put("base_price", basePrice);
        cv.put("num_points", numPoints);
        cv.put("revenue", revenue);
        cv.put("note", note == null ? "" : note);
        getWritableDatabase().update("trips", cv, "_id=?", new String[]{String.valueOf(id)});
    }

    public void addExpense(long date, String category, long amount, String note) {
        addExpense(date, category, amount, note, 0, 0, 0, 0);
    }

    public void addExpense(long date, String category, long amount, String note, double liters, double pricePerLiter, long mileage) {
        addExpense(date, category, amount, note, liters, pricePerLiter, mileage, 0);
    }

    public void addExpense(long date, String category, long amount, String note, double liters, double pricePerLiter, long mileage, double discount) {
        ContentValues cv = new ContentValues();
        cv.put("date", date);
        cv.put("category", category);
        cv.put("amount", amount);
        cv.put("note", note == null ? "" : note);
        cv.put("liters", liters);
        cv.put("price_per_liter", pricePerLiter);
        cv.put("mileage", mileage);
        cv.put("discount", discount);
        getWritableDatabase().insert("expenses", null, cv);
    }

    public void updateExpense(long id, long date, String category, long amount, String note) {
        updateExpense(id, date, category, amount, note, 0, 0, 0, 0);
    }

    public void updateExpense(long id, long date, String category, long amount, String note, double liters, double pricePerLiter, long mileage) {
        updateExpense(id, date, category, amount, note, liters, pricePerLiter, mileage, 0);
    }

    public void updateExpense(long id, long date, String category, long amount, String note, double liters, double pricePerLiter, long mileage, double discount) {
        ContentValues cv = new ContentValues();
        cv.put("date", date);
        cv.put("category", category);
        cv.put("amount", amount);
        cv.put("note", note == null ? "" : note);
        cv.put("liters", liters);
        cv.put("price_per_liter", pricePerLiter);
        cv.put("mileage", mileage);
        cv.put("discount", discount);
        getWritableDatabase().update("expenses", cv, "_id=?", new String[]{String.valueOf(id)});
    }

    public void deleteRecord(long id, boolean income) {
        getWritableDatabase().delete(income ? "trips" : "expenses", "_id=?", new String[]{String.valueOf(id)});
    }

    public boolean hasTripNumber(String number) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM trips WHERE number=?", new String[]{number});
        boolean exists = false;
        try {
            exists = c.moveToFirst();
        } finally {
            c.close();
        }
        return exists;
    }

    public void addMaint(long date, long mileage, String works) {
        ContentValues cv = new ContentValues();
        cv.put("date", date);
        cv.put("mileage", mileage);
        cv.put("works", works == null ? "" : works);
        getWritableDatabase().insert("maintenance", null, cv);
    }

    public void updateMaint(long id, long date, long mileage, String works) {
        ContentValues cv = new ContentValues();
        cv.put("date", date);
        cv.put("mileage", mileage);
        cv.put("works", works == null ? "" : works);
        getWritableDatabase().update("maintenance", cv, "_id=?", new String[]{String.valueOf(id)});
    }

    public void deleteMaint(long id) {
        getWritableDatabase().delete("maintenance", "_id=?", new String[]{String.valueOf(id)});
    }

    public Maint getMaintById(long id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT _id, date, mileage, works FROM maintenance WHERE _id=?", new String[]{String.valueOf(id)});
        try {
            if (c.moveToFirst()) return readMaint(c);
        } finally {
            c.close();
        }
        return null;
    }

    public List<Maint> getMaintAll() {
        List<Maint> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT _id, date, mileage, works FROM maintenance ORDER BY date DESC, _id DESC", null);
        try {
            while (c.moveToNext()) out.add(readMaint(c));
        } finally {
            c.close();
        }
        return out;
    }

    private Maint readMaint(Cursor c) {
        Maint m = new Maint();
        m.id = c.getLong(0);
        m.date = c.getLong(1);
        m.mileage = c.getLong(2);
        m.works = c.getString(3);
        return m;
    }

    public Record getRecord(long id, boolean income) {
        String sql = income
                ? "SELECT _id, 1 AS inc, date, number, zone, is_return, base_price, num_points, revenue AS amt, '' AS cat, note, 0, 0, 0, 0 FROM trips WHERE _id=?"
                : "SELECT _id, 0 AS inc, date, category, 0, 0, 0, 0, amount AS amt, category AS cat, note, liters, price_per_liter, mileage, discount FROM expenses WHERE _id=?";
        List<Record> l = queryRecords(sql, new String[]{String.valueOf(id)});
        return l.isEmpty() ? null : l.get(0);
    }

    public List<Record> getRecords(boolean allTime, int typeFilter) {
        // typeFilter: 0 = all, 1 = income, 2 = expense
        String period = "";
        List<String> args = new ArrayList<>();
        if (!allTime) {
            long[] range = currentMonthRange();
            period = "date >= ? AND date < ? AND ";
            args.add(String.valueOf(range[0]));
            args.add(String.valueOf(range[1]));
        }
        String typeA = typeFilter == 2 ? " AND 0" : "";
        String typeB = typeFilter == 1 ? " AND 0" : "";

        String sql = "SELECT _id, 1 AS inc, date, number, zone, is_return, base_price, num_points, revenue AS amt, '' AS cat, note, 0, 0, 0, 0 FROM trips WHERE " + period + "1" + typeA +
                " UNION ALL " +
                "SELECT _id, 0 AS inc, date, category, 0, 0, 0, 0, amount AS amt, category AS cat, note, liters, price_per_liter, mileage, discount FROM expenses WHERE " + period + "1" + typeB +
                " ORDER BY date DESC, _id DESC";
        return queryRecords(sql, args.toArray(new String[0]));
    }

    public List<Record> getRecent(int limit) {
        return queryRecords("SELECT _id, 1 AS inc, date, number, zone, is_return, base_price, num_points, revenue AS amt, '' AS cat, note, 0, 0, 0, 0 FROM trips " +
                "UNION ALL " +
                "SELECT _id, 0 AS inc, date, category, 0, 0, 0, 0, amount AS amt, category AS cat, note, liters, price_per_liter, mileage, discount FROM expenses " +
                "ORDER BY date DESC, _id DESC LIMIT " + limit, null);
    }

    private List<Record> queryRecords(String sql, String[] args) {
        List<Record> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        try {
            while (c.moveToNext()) {
                Record r = new Record();
                r.id = c.getLong(0);
                r.income = c.getInt(1) == 1;
                r.date = c.getLong(2);
                r.title = c.getString(3);
                r.zone = c.getInt(4);
                r.isReturn = c.getInt(5) == 1;
                r.basePrice = c.getLong(6);
                r.numPoints = c.getInt(7);
                r.amount = c.getLong(8);
                r.category = c.getString(9);
                r.note = c.getString(10);
                r.liters = c.getDouble(11);
                r.pricePerLiter = c.getDouble(12);
                r.mileage = c.getLong(13);
                r.discount = c.getDouble(14);
                if (r.income) {
                    r.number = r.title;
                    StringBuilder sub = new StringBuilder(Zones.name(r.zone));
                    if (r.isReturn) sub.append(" · возврат +50%");
                    if (r.numPoints > 0) sub.append(" · выгрузка: ").append(r.numPoints).append(" т.");
                    r.sub = sub.toString();
                } else {
                    if (r.liters > 0) {
                        StringBuilder sub = new StringBuilder(Util.num(r.liters)).append(" л");
                        if (r.pricePerLiter > 0) sub.append(" × ").append(Util.num(r.pricePerLiter)).append(" ₽/л");
                        if (r.discount > 0) sub.append(" · скидка ").append(Util.num(r.discount)).append("%");
                        if (r.mileage > 0) sub.append(" · ").append(r.mileage).append(" км");
                        if (r.note != null && !r.note.isEmpty()) sub.append(" · ").append(r.note);
                        r.sub = sub.toString();
                    } else {
                        r.sub = r.note;
                    }
                }
                out.add(r);
            }
        } finally {
            c.close();
        }
        return out;
    }

    public long[] currentMonthRange() {
        return monthRange();
    }

    private long[] monthRange() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        return new long[]{start.getTimeInMillis(), end.getTimeInMillis()};
    }

    public long[] getMonthTotals(boolean allTime) {
        if (allTime) return getTotals(-1, -1);
        long[] r = monthRange();
        return getTotals(r[0], r[1]);
    }

    public long[] getTotals(long start, long end) {
        long[] res = new long[2]; // income, expense
        if (start < 0) {
            res[0] = sum("SELECT SUM(revenue) FROM trips", null);
            res[1] = sum("SELECT SUM(amount) FROM expenses", null);
        } else {
            String[] args = {String.valueOf(start), String.valueOf(end)};
            res[0] = sum("SELECT SUM(revenue) FROM trips WHERE date>=? AND date<?", args);
            res[1] = sum("SELECT SUM(amount) FROM expenses WHERE date>=? AND date<?", args);
        }
        return res;
    }

    private long sum(String sql, String[] args) {
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        try {
            if (c.moveToFirst() && !c.isNull(0)) return c.getLong(0);
        } finally {
            c.close();
        }
        return 0;
    }

    public Map<String, Long> getCategoryTotals(long start, long end) {
        Map<String, Long> out = new LinkedHashMap<>();
        Cursor c;
        if (start < 0) {
            c = getReadableDatabase().rawQuery("SELECT category, SUM(amount) FROM expenses GROUP BY category ORDER BY SUM(amount) DESC", null);
        } else {
            String[] args = {String.valueOf(start), String.valueOf(end)};
            c = getReadableDatabase().rawQuery("SELECT category, SUM(amount) FROM expenses WHERE date>=? AND date<? GROUP BY category ORDER BY SUM(amount) DESC", args);
        }
        try {
            while (c.moveToNext()) {
                out.put(c.getString(0), c.getLong(1));
            }
        } finally {
            c.close();
        }
        return out;
    }

    public int getTripCount(long start, long end) {
        Cursor c;
        if (start < 0) {
            c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM trips", null);
        } else {
            String[] args = {String.valueOf(start), String.valueOf(end)};
            c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM trips WHERE date>=? AND date<?", args);
        }
        int n = 0;
        try {
            if (c.moveToFirst()) n = c.getInt(0);
        } finally {
            c.close();
        }
        return n;
    }

    /** Latest fuel fill-up with mileage less than the given value, sorted by mileage desc. */
    public Record getLastFuelBefore(long mileage) {
        List<Record> l = queryRecords(
                "SELECT _id, 0, date, category, 0, 0, 0, 0, amount, category, note, liters, price_per_liter, mileage, discount " +
                "FROM expenses WHERE category='Заправка' AND mileage>0 AND mileage<? " +
                "ORDER BY mileage DESC LIMIT 1",
                new String[]{String.valueOf(mileage)});
        return l.isEmpty() ? null : l.get(0);
    }

    /** Fuel stats over a period: {totalLiters, totalAmountKop, distanceKm, fillCount}. start<0 → all time. */
    public double[] getFuelStats(long start, long end) {
        String periodSql = start >= 0 ? " AND date>=? AND date<?" : "";
        String[] periodArgs = start >= 0 ? new String[]{String.valueOf(start), String.valueOf(end)} : null;

        double liters = 0;
        long amount = 0;
        int count = 0;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT liters, amount FROM expenses WHERE category='Заправка' AND liters>0" + periodSql,
                periodArgs);
        try {
            while (c.moveToNext()) {
                liters += c.getDouble(0);
                amount += c.getLong(1);
                count++;
            }
        } finally {
            c.close();
        }

        long minMileage = Long.MAX_VALUE, maxMileage = 0;
        Cursor c2 = getReadableDatabase().rawQuery(
                "SELECT mileage FROM expenses WHERE category='Заправка' AND liters>0 AND mileage>0" + periodSql,
                periodArgs);
        try {
            while (c2.moveToNext()) {
                long m = c2.getLong(0);
                if (m < minMileage) minMileage = m;
                if (m > maxMileage) maxMileage = m;
            }
        } finally {
            c2.close();
        }
        double dist = maxMileage > minMileage ? maxMileage - minMileage : 0;
        return new double[]{liters, amount, dist, count};
    }
}
