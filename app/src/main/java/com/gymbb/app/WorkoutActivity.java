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

    // UI — Common Header & Controls
    private TextView tvPlanName, tvTimer, tvProgress;
    private TextView tvExerciseName, tvSetCounter, tvTargetHint;
    private TextView tvDone, tvSkip, tvQuit;
    private LinearLayout layoutRest, layoutActions, layoutComplete;
    private TextView tvRestTimer, tvCompleteSummary, tvFinish;

    // UI — Strength Logging
    private LinearLayout layoutStrengthLogging;
    private TextView btnRepsMinus, tvRepsVal, btnRepsPlus;
    private TextView btnWeightMinus, tvWeightVal, btnWeightPlus;
    private int currentReps = 10;
    private double currentWeight = 0.0;

    // UI — Stretch Logging
    private LinearLayout layoutStretchLogging;
    private TextView tvStretchTimer, tvStretchHoldLabel;
    private TextView btnStretchMinus, btnStretchToggleTimer, btnStretchPlus;
    private int stretchHoldSeconds = 30;
    private int stretchSecondsRemaining = 30;
    private boolean isStretchHoldRunning = false;
    private Runnable stretchTimerRunnable;

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

        // Bind common views
        tvPlanName = (TextView) findViewById(R.id.tvPlanName);
        tvTimer = (TextView) findViewById(R.id.tvTimer);
        tvProgress = (TextView) findViewById(R.id.tvProgress);
        tvExerciseName = (TextView) findViewById(R.id.tvExerciseName);
        tvSetCounter = (TextView) findViewById(R.id.tvSetCounter);
        tvTargetHint = (TextView) findViewById(R.id.tvTargetHint);
        tvDone = (TextView) findViewById(R.id.tvDone);
        tvSkip = (TextView) findViewById(R.id.tvSkip);
        tvQuit = (TextView) findViewById(R.id.tvQuit);
        layoutRest = (LinearLayout) findViewById(R.id.layoutRest);
        layoutActions = (LinearLayout) findViewById(R.id.layoutActions);
        layoutComplete = (LinearLayout) findViewById(R.id.layoutComplete);
        tvRestTimer = (TextView) findViewById(R.id.tvRestTimer);
        tvCompleteSummary = (TextView) findViewById(R.id.tvCompleteSummary);
        tvFinish = (TextView) findViewById(R.id.tvFinish);

        // Bind strength logging views
        layoutStrengthLogging = (LinearLayout) findViewById(R.id.layoutStrengthLogging);
        btnRepsMinus = (TextView) findViewById(R.id.btnRepsMinus);
        tvRepsVal = (TextView) findViewById(R.id.tvRepsVal);
        btnRepsPlus = (TextView) findViewById(R.id.btnRepsPlus);
        btnWeightMinus = (TextView) findViewById(R.id.btnWeightMinus);
        tvWeightVal = (TextView) findViewById(R.id.tvWeightVal);
        btnWeightPlus = (TextView) findViewById(R.id.btnWeightPlus);

        // Bind stretch logging views
        layoutStretchLogging = (LinearLayout) findViewById(R.id.layoutStretchLogging);
        tvStretchTimer = (TextView) findViewById(R.id.tvStretchTimer);
        tvStretchHoldLabel = (TextView) findViewById(R.id.tvStretchHoldLabel);
        btnStretchMinus = (TextView) findViewById(R.id.btnStretchMinus);
        btnStretchToggleTimer = (TextView) findViewById(R.id.btnStretchToggleTimer);
        btnStretchPlus = (TextView) findViewById(R.id.btnStretchPlus);

        tvPlanName.setText(plan.name);

        // Button Listeners
        tvQuit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { confirmQuit(); }
        });

        tvDone.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isResting) {
                    endRest();
                } else {
                    completedSet();
                }
            }
        });

        tvSkip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isResting) {
                    endRest();
                } else {
                    skipSet();
                }
            }
        });

        tvFinish.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        // Strength adjusters
        btnRepsMinus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (currentReps > 1) {
                    currentReps--;
                    updateStrengthUI();
                }
            }
        });

        btnRepsPlus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                currentReps++;
                updateStrengthUI();
            }
        });

        btnWeightMinus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                currentWeight = Math.max(0.0, currentWeight - 2.5);
                updateStrengthUI();
            }
        });

        btnWeightPlus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                currentWeight += 2.5;
                updateStrengthUI();
            }
        });

        // Stretch adjusters & controls
        btnStretchMinus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                adjustStretchHold(-5);
            }
        });

        btnStretchPlus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                adjustStretchHold(5);
            }
        });

        btnStretchToggleTimer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleStretchTimer();
            }
        });

        startWorkoutTimer();
        showCurrentSet();
    }

    private void showCurrentSet() {
        if (exerciseIndex >= exercises.size()) {
            showComplete();
            return;
        }

        isResting = false;
        if (restTimerRunnable != null) {
            handler.removeCallbacks(restTimerRunnable);
            restTimerRunnable = null;
        }

        PlanExercise pe = exercises.get(exerciseIndex);

        tvProgress.setText("exercise " + (exerciseIndex + 1) + " / " + exercises.size());
        tvSetCounter.setText("set " + (setIndex + 1) + " / " + pe.sets);
        layoutRest.setVisibility(View.GONE);
        layoutActions.setVisibility(View.VISIBLE);
        tvTargetHint.setVisibility(View.VISIBLE);
        tvDone.setText("[✓ done]");

        if (pe.isStretch) {
            tvExerciseName.setText("[STRETCH] " + pe.exerciseName);
            layoutStrengthLogging.setVisibility(View.GONE);
            layoutStretchLogging.setVisibility(View.VISIBLE);

            stretchHoldSeconds = pe.reps > 0 ? pe.reps : 30;
            stretchSecondsRemaining = stretchHoldSeconds;
            tvTargetHint.setText("target: " + stretchHoldSeconds + "s hold");
            tvStretchHoldLabel.setText("-- hold stretch --");
            updateStretchUI();
            startStretchTimer();
        } else {
            tvExerciseName.setText(pe.exerciseName);
            stopStretchTimer();
            layoutStretchLogging.setVisibility(View.GONE);
            layoutStrengthLogging.setVisibility(View.VISIBLE);

            currentReps = pe.reps > 0 ? pe.reps : 10;
            currentWeight = pe.weight;
            String targetWeightStr = pe.weight > 0
                    ? String.format(Locale.US, "%.1fkg", pe.weight)
                    : "bodyweight";
            tvTargetHint.setText("target: " + pe.reps + " reps @ " + targetWeightStr);
            updateStrengthUI();
        }
    }

    private void updateStrengthUI() {
        tvRepsVal.setText(currentReps + " reps");
        if (currentWeight <= 0) {
            tvWeightVal.setText("bodyweight");
        } else {
            tvWeightVal.setText(String.format(Locale.US, "%.1f kg", currentWeight));
        }
    }

    private void startStretchTimer() {
        stopStretchTimer();
        isStretchHoldRunning = true;
        btnStretchToggleTimer.setText("[pause]");
        stretchTimerRunnable = new Runnable() {
            public void run() {
                if (isStretchHoldRunning && stretchSecondsRemaining > 0) {
                    stretchSecondsRemaining--;
                    updateStretchUI();
                    if (stretchSecondsRemaining <= 0) {
                        tvStretchHoldLabel.setText("✓ HOLD COMPLETED");
                    }
                }
                if (stretchSecondsRemaining > 0 && isStretchHoldRunning) {
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.postDelayed(stretchTimerRunnable, 1000);
    }

    private void stopStretchTimer() {
        isStretchHoldRunning = false;
        if (stretchTimerRunnable != null) {
            handler.removeCallbacks(stretchTimerRunnable);
            stretchTimerRunnable = null;
        }
    }

    private void toggleStretchTimer() {
        if (isStretchHoldRunning) {
            isStretchHoldRunning = false;
            btnStretchToggleTimer.setText("[resume]");
        } else {
            isStretchHoldRunning = true;
            btnStretchToggleTimer.setText("[pause]");
            if (stretchSecondsRemaining <= 0) {
                stretchSecondsRemaining = stretchHoldSeconds;
                tvStretchHoldLabel.setText("-- hold stretch --");
            }
            stretchTimerRunnable = new Runnable() {
                public void run() {
                    if (isStretchHoldRunning && stretchSecondsRemaining > 0) {
                        stretchSecondsRemaining--;
                        updateStretchUI();
                        if (stretchSecondsRemaining <= 0) {
                            tvStretchHoldLabel.setText("✓ HOLD COMPLETED");
                        }
                    }
                    if (stretchSecondsRemaining > 0 && isStretchHoldRunning) {
                        handler.postDelayed(this, 1000);
                    }
                }
            };
            handler.postDelayed(stretchTimerRunnable, 1000);
        }
    }

    private void adjustStretchHold(int delta) {
        stretchHoldSeconds = Math.max(5, stretchHoldSeconds + delta);
        stretchSecondsRemaining = Math.max(0, stretchSecondsRemaining + delta);
        updateStretchUI();
    }

    private void updateStretchUI() {
        tvStretchTimer.setText(stretchSecondsRemaining + "s");
    }

    private void completedSet() {
        if (isResting) return;

        PlanExercise pe = exercises.get(exerciseIndex);
        stopStretchTimer();

        // Log actual performed values
        if (pe.isStretch) {
            db.insertSessionSet(sessionId, pe.exerciseId, pe.exerciseName,
                    setIndex + 1, stretchHoldSeconds, 0.0, true);
        } else {
            db.insertSessionSet(sessionId, pe.exerciseId, pe.exerciseName,
                    setIndex + 1, currentReps, currentWeight, false);
        }

        advanceSet(pe);
    }

    private void skipSet() {
        if (isResting) return;
        stopStretchTimer();
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
                startRest(pe.restSeconds);
            }
        } else {
            startRest(pe.restSeconds);
        }
    }

    private void startRest(int seconds) {
        if (seconds <= 0) {
            showCurrentSet();
            return;
        }
        isResting = true;
        layoutStrengthLogging.setVisibility(View.GONE);
        layoutStretchLogging.setVisibility(View.GONE);
        tvTargetHint.setVisibility(View.GONE);
        layoutRest.setVisibility(View.VISIBLE);
        layoutActions.setVisibility(View.VISIBLE);
        tvDone.setText("[skip rest]");
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
        stopStretchTimer();
        if (restTimerRunnable != null) {
            handler.removeCallbacks(restTimerRunnable);
            restTimerRunnable = null;
        }

        int durationSec = (int) ((System.currentTimeMillis() - startTimeMs) / 1000);
        db.finalizeSession(sessionId, durationSec);

        int totalSets = db.countSessionSets(sessionId);
        int totalEx = db.countDistinctExercisesInSession(sessionId);
        int mins = durationSec / 60;
        int secs = durationSec % 60;

        tvCompleteSummary.setText(String.format(Locale.US,
                "%d exercises  ·  %d sets  ·  %02d:%02d", totalEx, totalSets, mins, secs));

        findViewById(R.id.tvProgress).setVisibility(View.GONE);
        layoutActions.setVisibility(View.GONE);
        layoutComplete.setVisibility(View.VISIBLE);

        ((View) tvExerciseName.getParent()).setVisibility(View.GONE);
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
            workoutTimerRunnable = null;
        }
        if (restTimerRunnable != null) {
            handler.removeCallbacks(restTimerRunnable);
            restTimerRunnable = null;
        }
    }

    private void confirmQuit() {
        new AlertDialog.Builder(this)
                .setTitle("quit workout?")
                .setMessage("progress will not be saved.")
                .setPositiveButton("quit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        stopWorkoutTimer();
                        stopStretchTimer();
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
        stopStretchTimer();
    }

    @Override
    public void onBackPressed() {
        confirmQuit();
    }
}
