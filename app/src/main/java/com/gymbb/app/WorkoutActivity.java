package com.gymbb.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class WorkoutActivity extends Activity {

    private DatabaseHelper db;
    private long planId;
    private Plan plan;
    private List<PlanExercise> exercises;

    // Tracking
    private int exerciseIndex = 0;
    private int setIndex = 0;       // current set within current exercise (0-based)
    private long sessionId = -1;
    private long startTimeMs;

    // UI
    private TextView tvPlanName, tvTimer, tvProgress;
    private TextView tvExerciseName, tvSetCounter, tvTarget, tvTargetSub;
    private TextView tvDone, tvSkip, tvQuit;
    private LinearLayout layoutRest, layoutActions, layoutComplete;
    private TextView tvRestTimer, tvCompleteSummary, tvFinish;

    // Timers
    private Handler handler = new Handler();
    private Runnable workoutTimerRunnable;
    private Runnable restTimerRunnable;
    private int restCountdown = 0;
    private boolean isResting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        db = new DatabaseHelper(this);
        planId = getIntent().getLongExtra("plan_id", -1);
        plan = db.getPlanById(planId);

        if (plan == null || planId == -1) { finish(); return; }

        exercises = db.getPlanExercises(planId);
        if (exercises.isEmpty()) { finish(); return; }

        // Create session
        startTimeMs = System.currentTimeMillis();
        sessionId = db.insertSession(planId, plan.name, startTimeMs);

        // Bind views
        tvPlanName = (TextView) findViewById(R.id.tvPlanName);
        tvTimer = (TextView) findViewById(R.id.tvTimer);
        tvProgress = (TextView) findViewById(R.id.tvProgress);
        tvExerciseName = (TextView) findViewById(R.id.tvExerciseName);
        tvSetCounter = (TextView) findViewById(R.id.tvSetCounter);
        tvTarget = (TextView) findViewById(R.id.tvTarget);
        tvTargetSub = (TextView) findViewById(R.id.tvTargetSub);
        tvDone = (TextView) findViewById(R.id.tvDone);
        tvSkip = (TextView) findViewById(R.id.tvSkip);
        tvQuit = (TextView) findViewById(R.id.tvQuit);
        layoutRest = (LinearLayout) findViewById(R.id.layoutRest);
        layoutActions = (LinearLayout) findViewById(R.id.layoutActions);
        layoutComplete = (LinearLayout) findViewById(R.id.layoutComplete);
        tvRestTimer = (TextView) findViewById(R.id.tvRestTimer);
        tvCompleteSummary = (TextView) findViewById(R.id.tvCompleteSummary);
        tvFinish = (TextView) findViewById(R.id.tvFinish);

        tvPlanName.setText(plan.name);

        tvQuit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { confirmQuit(); }
        });

        tvDone.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { completedSet(); }
        });

        tvSkip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { skipSet(); }
        });

        tvFinish.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        startWorkoutTimer();
        showCurrentSet();
    }

    private void showCurrentSet() {
        if (exerciseIndex >= exercises.size()) {
            showComplete();
            return;
        }

        PlanExercise pe = exercises.get(exerciseIndex);

        tvProgress.setText("exercise " + (exerciseIndex + 1) + " / " + exercises.size());
        tvExerciseName.setText(pe.exerciseName);
        tvSetCounter.setText("set " + (setIndex + 1) + " / " + pe.sets);

        String weightStr = pe.weight > 0
                ? String.format(Locale.US, "%.1fkg", pe.weight)
                : "bodyweight";
        tvTarget.setText(pe.reps + " reps");
        tvTargetSub.setText("@ " + weightStr);

        layoutRest.setVisibility(View.GONE);
        layoutActions.setVisibility(View.VISIBLE);
        isResting = false;
    }

    private void completedSet() {
        if (isResting) return;

        PlanExercise pe = exercises.get(exerciseIndex);

        // Log this set
        db.insertSessionSet(sessionId, pe.exerciseId, pe.exerciseName,
                setIndex + 1, pe.reps, pe.weight);

        advanceSet(pe);
    }

    private void skipSet() {
        if (isResting) return;
        PlanExercise pe = exercises.get(exerciseIndex);
        advanceSet(pe);
    }

    private void advanceSet(PlanExercise pe) {
        setIndex++;
        if (setIndex >= pe.sets) {
            // move to next exercise
            exerciseIndex++;
            setIndex = 0;
            if (exerciseIndex >= exercises.size()) {
                showComplete();
            } else {
                // rest between exercises
                startRest(pe.restSeconds);
            }
        } else {
            // rest between sets
            startRest(pe.restSeconds);
        }
    }

    private void startRest(int seconds) {
        isResting = true;
        layoutRest.setVisibility(View.VISIBLE);
        layoutActions.setVisibility(View.VISIBLE);
        restCountdown = seconds;
        updateRestTimer();

        restTimerRunnable = new Runnable() {
            public void run() {
                restCountdown--;
                if (restCountdown <= 0) {
                    endRest();
                } else {
                    updateRestTimer();
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.postDelayed(restTimerRunnable, 1000);
    }

    private void updateRestTimer() {
        tvRestTimer.setText(String.valueOf(restCountdown));
    }

    private void endRest() {
        if (restTimerRunnable != null) {
            handler.removeCallbacks(restTimerRunnable);
            restTimerRunnable = null;
        }
        isResting = false;
        showCurrentSet();
    }

    private void showComplete() {
        stopWorkoutTimer();
        int durationSec = (int) ((System.currentTimeMillis() - startTimeMs) / 1000);
        db.finalizeSession(sessionId, durationSec);

        int totalSets = db.countSessionSets(sessionId);
        int totalEx = db.countDistinctExercisesInSession(sessionId);
        int mins = durationSec / 60;
        int secs = durationSec % 60;

        tvCompleteSummary.setText(String.format(Locale.US,
                "%d exercises  ·  %d sets  ·  %02d:%02d", totalEx, totalSets, mins, secs));

        // Hide main workout UI, show complete
        findViewById(R.id.tvProgress).setVisibility(View.GONE);
        layoutActions.setVisibility(View.GONE);
        layoutComplete.setVisibility(View.VISIBLE);

        // Hide the main center block
        ((LinearLayout) tvExerciseName.getParent()).setVisibility(View.GONE);
    }

    private void startWorkoutTimer() {
        workoutTimerRunnable = new Runnable() {
            public void run() {
                long elapsed = (System.currentTimeMillis() - startTimeMs) / 1000;
                int m = (int) (elapsed / 60);
                int s = (int) (elapsed % 60);
                tvTimer.setText(String.format(Locale.US, "%02d:%02d", m, s));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(workoutTimerRunnable);
    }

    private void stopWorkoutTimer() {
        if (workoutTimerRunnable != null) {
            handler.removeCallbacks(workoutTimerRunnable);
        }
        if (restTimerRunnable != null) {
            handler.removeCallbacks(restTimerRunnable);
        }
    }

    private void confirmQuit() {
        new AlertDialog.Builder(this)
                .setTitle("quit workout?")
                .setMessage("progress will not be saved.")
                .setPositiveButton("quit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        stopWorkoutTimer();
                        // delete incomplete session
                        getWritableDatabase().delete("sessions", "id=?",
                                new String[]{String.valueOf(sessionId)});
                        getWritableDatabase().delete("session_sets", "session_id=?",
                                new String[]{String.valueOf(sessionId)});
                        finish();
                    }
                })
                .setNegativeButton("keep going", null)
                .show();
    }

    private android.database.sqlite.SQLiteDatabase getWritableDatabase() {
        return db.getWritableDatabase();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopWorkoutTimer();
    }

    @Override
    public void onBackPressed() {
        confirmQuit();
    }
}
