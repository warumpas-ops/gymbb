package com.gymbb.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper db;
    private TextView tvAppTitle, tvDayName, tvPlanName, tvPlanExercises, tvStartWorkout, tvRestDay, tvDate;
    private Plan todayPlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);
        db.ensureDefaultsSeeded();

        tvAppTitle = (TextView) findViewById(R.id.tvAppTitle);
        tvDayName = (TextView) findViewById(R.id.tvDayName);
        tvPlanName = (TextView) findViewById(R.id.tvPlanName);
        tvPlanExercises = (TextView) findViewById(R.id.tvPlanExercises);
        tvStartWorkout = (TextView) findViewById(R.id.tvStartWorkout);
        tvRestDay = (TextView) findViewById(R.id.tvRestDay);
        tvDate = (TextView) findViewById(R.id.tvDate);

        tvAppTitle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showSystemMenu();
            }
        });

        ((TextView) findViewById(R.id.navPlans)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PlansActivity.class));
            }
        });
        ((TextView) findViewById(R.id.navExercises)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ExerciseLibraryActivity.class));
            }
        });
        ((TextView) findViewById(R.id.navHistory)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
            }
        });

        tvStartWorkout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (todayPlan != null) {
                    Intent i = new Intent(MainActivity.this, WorkoutActivity.class);
                    i.putExtra("plan_id", todayPlan.id);
                    startActivity(i);
                }
            }
        });
    }

    private void showSystemMenu() {
        String backupInfo = BackupHelper.getBackupInfo();
        String[] options = new String[]{
                "backup data now",
                "restore data from backup (" + backupInfo + ")",
                "cancel"
        };

        new AlertDialog.Builder(this)
                .setTitle("gymbb system (v1.4.1)")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            boolean ok = BackupHelper.exportDatabase(MainActivity.this);
                            if (ok) {
                                Toast.makeText(MainActivity.this, "✓ backup saved to /sdcard/GymBB", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(MainActivity.this, "backup failed: storage error", Toast.LENGTH_SHORT).show();
                            }
                        } else if (which == 1) {
                            confirmRestore();
                        }
                    }
                })
                .show();
    }

    private void confirmRestore() {
        String backupInfo = BackupHelper.getBackupInfo();
        new AlertDialog.Builder(this)
                .setTitle("restore data?")
                .setMessage("backup: " + backupInfo + "\n\nwarning: this will replace current data with the backup file.")
                .setPositiveButton("restore", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean ok = BackupHelper.importDatabase(MainActivity.this);
                        if (ok) {
                            Toast.makeText(MainActivity.this, "✓ data restored successfully", Toast.LENGTH_LONG).show();
                            try {
                                db.close();
                            } catch (Exception ignored) {}
                            db = new DatabaseHelper(MainActivity.this);
                            loadTodayInfo();
                        } else {
                            Toast.makeText(MainActivity.this, "restore failed: no valid backup file found", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTodayInfo();
    }

    private void loadTodayInfo() {
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        String[] dayNames = {"", "sun", "mon", "tue", "wed", "thu", "fri", "sat"};
        String todayName = dayNames[dayOfWeek];
        String[] fullDayNames = {"", "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};

        tvDayName.setText(fullDayNames[dayOfWeek].toLowerCase());
        tvDate.setText(new SimpleDateFormat("dd MMM", Locale.US).format(new Date()));

        // Find a plan for today
        todayPlan = null;
        List<Plan> plans = db.getAllPlans();
        for (Plan p : plans) {
            if (p.includesDay(todayName)) {
                todayPlan = p;
                break;
            }
        }

        if (todayPlan != null) {
            tvPlanName.setText(todayPlan.name);
            List<PlanExercise> exs = db.getPlanExercises(todayPlan.id);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < exs.size(); i++) {
                if (i > 0) sb.append("  ·  ");
                sb.append(exs.get(i).exerciseName);
            }
            tvPlanExercises.setText(sb.toString());
            tvStartWorkout.setVisibility(View.VISIBLE);
            tvRestDay.setVisibility(View.GONE);
        } else {
            tvPlanName.setText("");
            tvPlanExercises.setText("");
            tvStartWorkout.setVisibility(View.GONE);
            tvRestDay.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // BB Q5 keyboard shortcuts
        if (keyCode == KeyEvent.KEYCODE_P) {
            startActivity(new Intent(this, PlansActivity.class));
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_E) {
            startActivity(new Intent(this, ExerciseLibraryActivity.class));
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_H) {
            startActivity(new Intent(this, HistoryActivity.class));
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_B || keyCode == KeyEvent.KEYCODE_S) {
            showSystemMenu();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
