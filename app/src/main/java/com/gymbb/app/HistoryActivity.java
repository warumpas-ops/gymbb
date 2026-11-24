package com.gymbb.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends Activity {

    private DatabaseHelper db;
    private ListView lvHistory;
    private TextView tvEmpty;
    private List<WorkoutSession> sessions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = new DatabaseHelper(this);
        lvHistory = (ListView) findViewById(R.id.lvHistory);
        tvEmpty = (TextView) findViewById(R.id.tvEmpty);

        ((TextView) findViewById(R.id.tvBack)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        TextView btnBackup = (TextView) findViewById(R.id.btnBackup);
        if (btnBackup != null) {
            btnBackup.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showBackupMenu();
                }
            });
        }

        loadHistory();
    }

    private void showBackupMenu() {
        String backupInfo = BackupHelper.getBackupInfo();
        String[] options = new String[]{
                "save backup now (/sdcard/GymBB)",
                "restore data from backup (" + backupInfo + ")",
                "cancel"
        };

        new AlertDialog.Builder(this)
                .setTitle("gymbb data & backup")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            boolean ok = BackupHelper.exportDatabase(HistoryActivity.this);
                            if (ok) {
                                String info = BackupHelper.getBackupInfo();
                                Toast.makeText(HistoryActivity.this, "✓ backup saved: " + info + "\nlocation: /sdcard/GymBB/gymbb_backup.db", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(HistoryActivity.this, "backup failed: storage permission error", Toast.LENGTH_SHORT).show();
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
                .setTitle("restore all gym data?")
                .setMessage("backup file: " + backupInfo + "\n\nwarning: this will restore all your saved plans, custom exercises, and workout history from the SD card.")
                .setPositiveButton("restore", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean ok = BackupHelper.importDatabase(HistoryActivity.this);
                        if (ok) {
                            Toast.makeText(HistoryActivity.this, "✓ all data restored successfully", Toast.LENGTH_LONG).show();
                            try {
                                db.close();
                            } catch (Exception ignored) {}
                            db = new DatabaseHelper(HistoryActivity.this);
                            loadHistory();
                        } else {
                            Toast.makeText(HistoryActivity.this, "restore failed: no backup found at /sdcard/GymBB", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("cancel", null)
                .show();
    }

    private void loadHistory() {
        sessions = db.getAllSessions();
        if (sessions.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            lvHistory.setVisibility(View.GONE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        lvHistory.setVisibility(View.VISIBLE);

        lvHistory.setAdapter(new ArrayAdapter<WorkoutSession>(this, R.layout.item_history, sessions) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_history, parent, false);
                }
                WorkoutSession s = sessions.get(position);

                TextView tvDate = (TextView) convertView.findViewById(R.id.tvDate);
                TextView tvDuration = (TextView) convertView.findViewById(R.id.tvDuration);
                TextView tvPlanName = (TextView) convertView.findViewById(R.id.tvPlanName);
                TextView tvSummary = (TextView) convertView.findViewById(R.id.tvSummary);

                String dateStr = new SimpleDateFormat("EEE dd MMM", Locale.US).format(new Date(s.dateMs));
                tvDate.setText(dateStr.toLowerCase());

                int m = s.durationSeconds / 60;
                int sec = s.durationSeconds % 60;
                tvDuration.setText(String.format(Locale.US, "%02d:%02d", m, sec));
                tvPlanName.setText(s.planName != null ? s.planName : "workout");

                int totalSets = db.countSessionSets(s.id);
                int totalEx = db.countDistinctExercisesInSession(s.id);
                tvSummary.setText(totalEx + " exercises  ·  " + totalSets + " sets");

                return convertView;
            }
        });

        lvHistory.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                WorkoutSession s = sessions.get(position);
                android.content.Intent intent = new android.content.Intent(HistoryActivity.this, SessionDetailActivity.class);
                intent.putExtra("session_id", s.id);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_B) {
            showBackupMenu();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
