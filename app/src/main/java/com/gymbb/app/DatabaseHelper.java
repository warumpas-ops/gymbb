package com.gymbb.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "gymbb.db";
    private static final int DB_VERSION = 2;

    // Table names
    static final String TABLE_EXERCISES = "exercises";
    static final String TABLE_PLANS = "plans";
    static final String TABLE_PLAN_EXERCISES = "plan_exercises";
    static final String TABLE_SESSIONS = "sessions";
    static final String TABLE_SESSION_SETS = "session_sets";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_EXERCISES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "muscle_group TEXT," +
                "notes TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_PLANS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "days TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_PLAN_EXERCISES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "plan_id INTEGER," +
                "exercise_id INTEGER," +
                "sets INTEGER DEFAULT 3," +
                "reps INTEGER DEFAULT 10," +
                "weight REAL DEFAULT 0," +
                "rest_seconds INTEGER DEFAULT 60," +
                "sort_order INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_SESSIONS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "plan_id INTEGER," +
                "plan_name TEXT," +
                "date_ms INTEGER," +
                "duration_seconds INTEGER DEFAULT 0," +
                "completed INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_SESSION_SETS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "session_id INTEGER," +
                "exercise_id INTEGER," +
                "exercise_name TEXT," +
                "set_number INTEGER," +
                "reps INTEGER," +
                "weight REAL," +
                "completed INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSION_SETS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAN_EXERCISES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLANS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXERCISES);
            onCreate(db);
        }
    }

    // ==================== EXERCISES ====================

    public long insertExercise(String name, String muscleGroup, String notes) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name.trim());
        cv.put("muscle_group", muscleGroup != null ? muscleGroup.trim().toLowerCase() : "");
        cv.put("notes", notes != null ? notes.trim() : "");
        return db.insert(TABLE_EXERCISES, null, cv);
    }

    public long getOrCreateExercise(String name, String muscleGroup) {
        String trimmed = name.trim();
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(TABLE_EXERCISES, new String[]{"id"}, "LOWER(name)=?",
                new String[]{trimmed.toLowerCase()}, null, null, null);
        if (c.moveToFirst()) {
            long id = c.getLong(0);
            c.close();
            return id;
        }
        c.close();

        ContentValues cv = new ContentValues();
        cv.put("name", trimmed);
        cv.put("muscle_group", muscleGroup != null ? muscleGroup.trim().toLowerCase() : "");
        cv.put("notes", "");
        return db.insert(TABLE_EXERCISES, null, cv);
    }

    public List<Exercise> getAllExercises() {
        List<Exercise> list = new ArrayList<Exercise>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_EXERCISES, null, null, null, null, null, "name ASC");
        while (c.moveToNext()) {
            Exercise e = new Exercise();
            e.id = c.getLong(c.getColumnIndex("id"));
            e.name = c.getString(c.getColumnIndex("name"));
            e.muscleGroup = c.getString(c.getColumnIndex("muscle_group"));
            e.notes = c.getString(c.getColumnIndex("notes"));
            list.add(e);
        }
        c.close();
        return list;
    }

    public void deleteExercise(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_EXERCISES, "id=?", new String[]{String.valueOf(id)});
    }

    // ==================== PLANS ====================

    public long insertPlan(String name, String days) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name.trim());
        cv.put("days", days);
        return db.insert(TABLE_PLANS, null, cv);
    }

    public List<Plan> getAllPlans() {
        List<Plan> list = new ArrayList<Plan>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_PLANS, null, null, null, null, null, "name ASC");
        while (c.moveToNext()) {
            Plan p = new Plan();
            p.id = c.getLong(c.getColumnIndex("id"));
            p.name = c.getString(c.getColumnIndex("name"));
            p.days = c.getString(c.getColumnIndex("days"));
            list.add(p);
        }
        c.close();
        return list;
    }

    public Plan getPlanById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_PLANS, null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        if (c.moveToFirst()) {
            Plan p = new Plan();
            p.id = c.getLong(c.getColumnIndex("id"));
            p.name = c.getString(c.getColumnIndex("name"));
            p.days = c.getString(c.getColumnIndex("days"));
            c.close();
            return p;
        }
        c.close();
        return null;
    }

    public void deletePlan(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_PLANS, "id=?", new String[]{String.valueOf(id)});
        db.delete(TABLE_PLAN_EXERCISES, "plan_id=?", new String[]{String.valueOf(id)});
    }

    // ==================== PLAN EXERCISES ====================

    public long insertPlanExercise(long planId, long exerciseId, int sets, int reps, double weight, int restSec) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT MAX(sort_order) FROM " + TABLE_PLAN_EXERCISES + " WHERE plan_id=?",
                new String[]{String.valueOf(planId)});
        int sortOrder = 0;
        if (c.moveToFirst() && !c.isNull(0)) sortOrder = c.getInt(0) + 1;
        c.close();

        ContentValues cv = new ContentValues();
        cv.put("plan_id", planId);
        cv.put("exercise_id", exerciseId);
        cv.put("sets", sets);
        cv.put("reps", reps);
        cv.put("weight", weight);
        cv.put("rest_seconds", restSec);
        cv.put("sort_order", sortOrder);
        return db.insert(TABLE_PLAN_EXERCISES, null, cv);
    }

    public List<PlanExercise> getPlanExercises(long planId) {
        List<PlanExercise> list = new ArrayList<PlanExercise>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT pe.*, e.name AS ex_name, e.muscle_group " +
                "FROM " + TABLE_PLAN_EXERCISES + " pe " +
                "JOIN " + TABLE_EXERCISES + " e ON pe.exercise_id = e.id " +
                "WHERE pe.plan_id=? ORDER BY pe.sort_order ASC";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(planId)});
        while (c.moveToNext()) {
            PlanExercise pe = new PlanExercise();
            pe.id = c.getLong(c.getColumnIndex("id"));
            pe.planId = planId;
            pe.exerciseId = c.getLong(c.getColumnIndex("exercise_id"));
            pe.exerciseName = c.getString(c.getColumnIndex("ex_name"));
            pe.muscleGroup = c.getString(c.getColumnIndex("muscle_group"));
            pe.sets = c.getInt(c.getColumnIndex("sets"));
            pe.reps = c.getInt(c.getColumnIndex("reps"));
            pe.weight = c.getDouble(c.getColumnIndex("weight"));
            pe.restSeconds = c.getInt(c.getColumnIndex("rest_seconds"));
            pe.sortOrder = c.getInt(c.getColumnIndex("sort_order"));
            list.add(pe);
        }
        c.close();
        return list;
    }

    public void deletePlanExercise(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_PLAN_EXERCISES, "id=?", new String[]{String.valueOf(id)});
    }

    // ==================== SESSIONS ====================

    public long insertSession(long planId, String planName, long dateMs) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("plan_id", planId);
        cv.put("plan_name", planName);
        cv.put("date_ms", dateMs);
        cv.put("completed", 0);
        return db.insert(TABLE_SESSIONS, null, cv);
    }

    public void finalizeSession(long sessionId, int durationSeconds) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("duration_seconds", durationSeconds);
        cv.put("completed", 1);
        db.update(TABLE_SESSIONS, cv, "id=?", new String[]{String.valueOf(sessionId)});
    }

    public List<WorkoutSession> getAllSessions() {
        List<WorkoutSession> list = new ArrayList<WorkoutSession>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_SESSIONS, null, null, null, null, null, "date_ms DESC");
        while (c.moveToNext()) {
            WorkoutSession s = new WorkoutSession();
            s.id = c.getLong(c.getColumnIndex("id"));
            s.planId = c.getLong(c.getColumnIndex("plan_id"));
            s.planName = c.getString(c.getColumnIndex("plan_name"));
            s.dateMs = c.getLong(c.getColumnIndex("date_ms"));
            s.durationSeconds = c.getInt(c.getColumnIndex("duration_seconds"));
            s.completed = c.getInt(c.getColumnIndex("completed"));
            list.add(s);
        }
        c.close();
        return list;
    }

    // ==================== SESSION SETS ====================

    public long insertSessionSet(long sessionId, long exerciseId, String exerciseName,
                                  int setNumber, int reps, double weight) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("session_id", sessionId);
        cv.put("exercise_id", exerciseId);
        cv.put("exercise_name", exerciseName);
        cv.put("set_number", setNumber);
        cv.put("reps", reps);
        cv.put("weight", weight);
        cv.put("completed", 1);
        return db.insert(TABLE_SESSION_SETS, null, cv);
    }

    public int countSessionSets(long sessionId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SESSION_SETS +
                " WHERE session_id=? AND completed=1", new String[]{String.valueOf(sessionId)});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public int countDistinctExercisesInSession(long sessionId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(DISTINCT exercise_id) FROM " + TABLE_SESSION_SETS +
                " WHERE session_id=? AND completed=1", new String[]{String.valueOf(sessionId)});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }
}
