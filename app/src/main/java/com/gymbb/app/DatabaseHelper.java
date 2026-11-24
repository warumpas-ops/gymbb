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
    private static final int DB_VERSION = 5;

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
                "notes TEXT," +
                "is_stretch INTEGER DEFAULT 0)");

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
                "sort_order INTEGER DEFAULT 0," +
                "is_stretch INTEGER DEFAULT 0)");

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
                "completed INTEGER DEFAULT 0," +
                "is_stretch INTEGER DEFAULT 0)");

        seedExercises(db);
    }

    private void seedExercises(SQLiteDatabase db) {
        String[][] defaults = {
                {"Bench Press", "chest", "0"},
                {"Incline Dumbbell Press", "chest", "0"},
                {"Push-Up", "chest", "0"},
                {"Pull-Up", "back", "0"},
                {"Barbell Row", "back", "0"},
                {"Lat Pulldown", "back", "0"},
                {"Overhead Press", "shoulders", "0"},
                {"Lateral Raise", "shoulders", "0"},
                {"Squat", "legs", "0"},
                {"Deadlift", "legs", "0"},
                {"Leg Press", "legs", "0"},
                {"Lunge", "legs", "0"},
                {"Bicep Curl", "biceps", "0"},
                {"Hammer Curl", "biceps", "0"},
                {"Tricep Dip", "triceps", "0"},
                {"Skull Crusher", "triceps", "0"},
                {"Plank", "core", "0"},
                {"Crunch", "core", "0"},
                // User's Stretches & Mobility (Curated List - No Duplicates)
                {"Hip Flexor", "hips", "1"},
                {"Sciatic Nerve Glide", "hamstrings", "1"},
                {"Glute Bridges", "glutes", "1"},
                {"Scapular Wall Slides", "upper back", "1"},
                {"Chest Doorway", "chest", "1"},
                {"Thoracic Extension Over Towel", "upper back", "1"},
                {"Sphinx Pose", "lower back", "1"},
                {"Cobra Pose", "lower back", "1"},
                {"Chin Tucks", "neck", "1"},
                {"Upper Trap Stretch", "neck", "1"},
                {"Levator Scapulae Stretch", "neck", "1"},
                {"Cat-Cow", "spine", "1"},
                {"Thread the Needle", "upper back", "1"},
                {"90/90 Hips Rotation", "hips", "1"},
                {"Figure 4", "glutes", "1"},

                // Standard baseline stretches
                {"Hamstring Stretch", "legs", "1"},
                {"Quad Stretch", "legs", "1"},
                {"Calf Stretch", "legs", "1"},
                {"Shoulder Stretch", "shoulders", "1"},
                {"Child's Pose", "back", "1"}
        };
        for (String[] ex : defaults) {
            Cursor c = db.query(TABLE_EXERCISES, new String[]{"id"}, "LOWER(name)=?",
                    new String[]{ex[0].trim().toLowerCase()}, null, null, null);
            if (!c.moveToFirst()) {
                ContentValues cv = new ContentValues();
                cv.put("name", ex[0]);
                cv.put("muscle_group", ex[1]);
                cv.put("notes", "");
                cv.put("is_stretch", Integer.parseInt(ex[2]));
                db.insert(TABLE_EXERCISES, null, cv);
            }
            c.close();
        }
    }

    public void deduplicateExercises(SQLiteDatabase db) {
        mergeExercises(db, "Hip Flexor", "Hip Flexor Stretch");
        mergeExercises(db, "Chest Doorway", "Chest Doorway Stretch");
        mergeExercises(db, "Chest Doorway", "Chest Opener Stretch");
        mergeExercises(db, "Cobra Pose", "Cobra Stretch");
        mergeExercises(db, "Figure 4", "Figure 4 Stretch");
        mergeExercises(db, "90/90 Hips Rotation", "90/90 Hip Rotations");
    }

    private void mergeExercises(SQLiteDatabase db, String keepName, String removeName) {
        try {
            Cursor cKeep = db.query(TABLE_EXERCISES, new String[]{"id"}, "LOWER(name)=?",
                    new String[]{keepName.trim().toLowerCase()}, null, null, null);
            Cursor cRemove = db.query(TABLE_EXERCISES, new String[]{"id"}, "LOWER(name)=?",
                    new String[]{removeName.trim().toLowerCase()}, null, null, null);

            boolean hasKeep = cKeep.moveToFirst();
            boolean hasRemove = cRemove.moveToFirst();

            if (hasKeep && hasRemove) {
                long keepId = cKeep.getLong(0);
                long removeId = cRemove.getLong(0);
                // Re-point plan_exercises
                db.execSQL("UPDATE " + TABLE_PLAN_EXERCISES + " SET exercise_id=" + keepId + " WHERE exercise_id=" + removeId);
                // Re-point session_sets
                db.execSQL("UPDATE " + TABLE_SESSION_SETS + " SET exercise_id=" + keepId + ", exercise_name='" + keepName + "' WHERE exercise_id=" + removeId);
                // Delete duplicate exercise
                db.execSQL("DELETE FROM " + TABLE_EXERCISES + " WHERE id=" + removeId);
            } else if (!hasKeep && hasRemove) {
                ContentValues cv = new ContentValues();
                cv.put("name", keepName);
                db.update(TABLE_EXERCISES, cv, "LOWER(name)=?", new String[]{removeName.trim().toLowerCase()});
                db.update(TABLE_SESSION_SETS, cv, "LOWER(exercise_name)=?", new String[]{removeName.trim().toLowerCase()});
            }

            cKeep.close();
            cRemove.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ensureDefaultsSeeded() {
        SQLiteDatabase db = getWritableDatabase();
        seedExercises(db);
        deduplicateExercises(db);
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
            return;
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_EXERCISES + " ADD COLUMN is_stretch INTEGER DEFAULT 0");
            } catch (Exception e) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_PLAN_EXERCISES + " ADD COLUMN is_stretch INTEGER DEFAULT 0");
            } catch (Exception e) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_SESSION_SETS + " ADD COLUMN is_stretch INTEGER DEFAULT 0");
            } catch (Exception e) {}
        }
        if (oldVersion < 4) {
            seedExercises(db);
        }
        if (oldVersion < 5) {
            seedExercises(db);
            deduplicateExercises(db);
        }
    }

    // ==================== EXERCISES ====================

    public long insertExercise(String name, String muscleGroup, String notes, boolean isStretch) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name.trim());
        cv.put("muscle_group", muscleGroup != null ? muscleGroup.trim().toLowerCase() : "");
        cv.put("notes", notes != null ? notes.trim() : "");
        cv.put("is_stretch", isStretch ? 1 : 0);
        return db.insert(TABLE_EXERCISES, null, cv);
    }

    public long insertExercise(String name, String muscleGroup, String notes) {
        return insertExercise(name, muscleGroup, notes, false);
    }

    public long getOrCreateExercise(String name, String muscleGroup, boolean isStretch) {
        String trimmed = name.trim();
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(TABLE_EXERCISES, new String[]{"id", "is_stretch"}, "LOWER(name)=?",
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
        cv.put("is_stretch", isStretch ? 1 : 0);
        return db.insert(TABLE_EXERCISES, null, cv);
    }

    public long getOrCreateExercise(String name, String muscleGroup) {
        return getOrCreateExercise(name, muscleGroup, false);
    }

    public Exercise getExerciseById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_EXERCISES, null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        if (c.moveToFirst()) {
            Exercise e = new Exercise();
            e.id = c.getLong(c.getColumnIndex("id"));
            e.name = c.getString(c.getColumnIndex("name"));
            e.muscleGroup = c.getString(c.getColumnIndex("muscle_group"));
            e.notes = c.getString(c.getColumnIndex("notes"));
            int isStrIdx = c.getColumnIndex("is_stretch");
            e.isStretch = (isStrIdx >= 0 && c.getInt(isStrIdx) == 1);
            c.close();
            return e;
        }
        c.close();
        return null;
    }

    public List<Exercise> getAllExercises() {
        List<Exercise> list = new ArrayList<Exercise>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_EXERCISES, null, null, null, null, null, "is_stretch ASC, name ASC");
        while (c.moveToNext()) {
            Exercise e = new Exercise();
            e.id = c.getLong(c.getColumnIndex("id"));
            e.name = c.getString(c.getColumnIndex("name"));
            e.muscleGroup = c.getString(c.getColumnIndex("muscle_group"));
            e.notes = c.getString(c.getColumnIndex("notes"));
            int isStrIdx = c.getColumnIndex("is_stretch");
            e.isStretch = (isStrIdx >= 0 && c.getInt(isStrIdx) == 1);
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

    public long insertPlanExercise(long planId, long exerciseId, int sets, int reps, double weight, int restSec, boolean isStretch) {
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
        cv.put("is_stretch", isStretch ? 1 : 0);
        return db.insert(TABLE_PLAN_EXERCISES, null, cv);
    }

    public long insertPlanExercise(long planId, long exerciseId, int sets, int reps, double weight, int restSec) {
        return insertPlanExercise(planId, exerciseId, sets, reps, weight, restSec, false);
    }

    public List<PlanExercise> getPlanExercises(long planId) {
        List<PlanExercise> list = new ArrayList<PlanExercise>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT pe.*, e.name AS ex_name, e.muscle_group, e.is_stretch AS e_is_stretch " +
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
            int peStrIdx = c.getColumnIndex("is_stretch");
            int eStrIdx = c.getColumnIndex("e_is_stretch");
            pe.isStretch = (peStrIdx >= 0 && c.getInt(peStrIdx) == 1) ||
                           (eStrIdx >= 0 && c.getInt(eStrIdx) == 1);
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

    public WorkoutSession getSessionById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_SESSIONS, null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        if (c.moveToFirst()) {
            WorkoutSession s = new WorkoutSession();
            s.id = c.getLong(c.getColumnIndex("id"));
            s.planId = c.getLong(c.getColumnIndex("plan_id"));
            s.planName = c.getString(c.getColumnIndex("plan_name"));
            s.dateMs = c.getLong(c.getColumnIndex("date_ms"));
            s.durationSeconds = c.getInt(c.getColumnIndex("duration_seconds"));
            s.completed = c.getInt(c.getColumnIndex("completed"));
            c.close();
            return s;
        }
        c.close();
        return null;
    }

    // ==================== SESSION SETS ====================

    public long insertSessionSet(long sessionId, long exerciseId, String exerciseName,
                                  int setNumber, int reps, double weight, int stretchType) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("session_id", sessionId);
        cv.put("exercise_id", exerciseId);
        cv.put("exercise_name", exerciseName);
        cv.put("set_number", setNumber);
        cv.put("reps", reps);
        cv.put("weight", weight);
        cv.put("completed", 1);
        cv.put("is_stretch", stretchType);
        return db.insert(TABLE_SESSION_SETS, null, cv);
    }

    public long insertSessionSet(long sessionId, long exerciseId, String exerciseName,
                                  int setNumber, int reps, double weight, boolean isStretch) {
        return insertSessionSet(sessionId, exerciseId, exerciseName, setNumber, reps, weight, isStretch ? 1 : 0);
    }

    public long insertSessionSet(long sessionId, long exerciseId, String exerciseName,
                                  int setNumber, int reps, double weight) {
        return insertSessionSet(sessionId, exerciseId, exerciseName, setNumber, reps, weight, 0);
    }

    public List<SessionSet> getSessionSets(long sessionId) {
        List<SessionSet> list = new ArrayList<SessionSet>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_SESSION_SETS, null, "session_id=?",
                new String[]{String.valueOf(sessionId)}, null, null, "id ASC");
        while (c.moveToNext()) {
            SessionSet s = new SessionSet();
            s.id = c.getLong(c.getColumnIndex("id"));
            s.sessionId = sessionId;
            s.exerciseId = c.getLong(c.getColumnIndex("exercise_id"));
            s.exerciseName = c.getString(c.getColumnIndex("exercise_name"));
            s.setNumber = c.getInt(c.getColumnIndex("set_number"));
            s.reps = c.getInt(c.getColumnIndex("reps"));
            s.weight = c.getDouble(c.getColumnIndex("weight"));
            s.completed = c.getInt(c.getColumnIndex("completed"));
            int strIdx = c.getColumnIndex("is_stretch");
            int sType = (strIdx >= 0) ? c.getInt(strIdx) : 0;
            s.stretchType = sType;
            s.isStretch = (sType > 0);
            list.add(s);
        }
        c.close();
        return list;
    }

    public List<SessionSet> getExerciseHistory(long exerciseId) {
        List<SessionSet> list = new ArrayList<SessionSet>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT ss.*, s.date_ms, s.plan_name " +
                "FROM " + TABLE_SESSION_SETS + " ss " +
                "JOIN " + TABLE_SESSIONS + " s ON ss.session_id = s.id " +
                "WHERE ss.exercise_id=? AND ss.completed=1 " +
                "ORDER BY s.date_ms DESC, ss.set_number ASC";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(exerciseId)});
        while (c.moveToNext()) {
            SessionSet s = new SessionSet();
            s.id = c.getLong(c.getColumnIndex("id"));
            s.sessionId = c.getLong(c.getColumnIndex("session_id"));
            s.exerciseId = exerciseId;
            s.exerciseName = c.getString(c.getColumnIndex("exercise_name"));
            s.setNumber = c.getInt(c.getColumnIndex("set_number"));
            s.reps = c.getInt(c.getColumnIndex("reps"));
            s.weight = c.getDouble(c.getColumnIndex("weight"));
            s.completed = c.getInt(c.getColumnIndex("completed"));
            int strIdx = c.getColumnIndex("is_stretch");
            int sType = (strIdx >= 0) ? c.getInt(strIdx) : 0;
            s.stretchType = sType;
            s.isStretch = (sType > 0);
            s.dateMs = c.getLong(c.getColumnIndex("date_ms"));
            s.planName = c.getString(c.getColumnIndex("plan_name"));
            list.add(s);
        }
        c.close();
        return list;
    }

    public double getExerciseMaxWeight(long exerciseId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT MAX(weight) FROM " + TABLE_SESSION_SETS +
                " WHERE exercise_id=? AND completed=1", new String[]{String.valueOf(exerciseId)});
        double max = 0;
        if (c.moveToFirst() && !c.isNull(0)) max = c.getDouble(0);
        c.close();
        return max;
    }

    public int getExerciseMaxHold(long exerciseId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT MAX(reps) FROM " + TABLE_SESSION_SETS +
                " WHERE exercise_id=? AND completed=1 AND is_stretch=1", new String[]{String.valueOf(exerciseId)});
        int max = 0;
        if (c.moveToFirst() && !c.isNull(0)) max = c.getInt(0);
        c.close();
        return max;
    }

    public int getExerciseMaxStretchReps(long exerciseId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT MAX(reps) FROM " + TABLE_SESSION_SETS +
                " WHERE exercise_id=? AND completed=1 AND is_stretch=2", new String[]{String.valueOf(exerciseId)});
        int max = 0;
        if (c.moveToFirst() && !c.isNull(0)) max = c.getInt(0);
        c.close();
        return max;
    }

    public int getExerciseMaxReps(long exerciseId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT MAX(reps) FROM " + TABLE_SESSION_SETS +
                " WHERE exercise_id=? AND completed=1", new String[]{String.valueOf(exerciseId)});
        int max = 0;
        if (c.moveToFirst() && !c.isNull(0)) max = c.getInt(0);
        c.close();
        return max;
    }

    public int getExerciseTotalSessions(long exerciseId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(DISTINCT session_id) FROM " + TABLE_SESSION_SETS +
                " WHERE exercise_id=? AND completed=1", new String[]{String.valueOf(exerciseId)});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
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
