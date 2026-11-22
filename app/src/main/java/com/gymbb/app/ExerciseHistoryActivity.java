package com.gymbb.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExerciseHistoryActivity extends Activity {

    private DatabaseHelper db;
    private long exerciseId;
    private Exercise exercise;

    private TextView tvExerciseTitle, tvTypeBadge, tvPRValue, tvStatsSummary, tvEmpty;
    private ListView lvProgression;

    // Helper class representing one workout session's performance for this exercise
    private static class SessionGroup {
        long sessionId;
        long dateMs;
        String planName;
        List<SessionSet> sets = new ArrayList<SessionSet>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_history);

        db = new DatabaseHelper(this);
        exerciseId = getIntent().getLongExtra("exercise_id", -1);
        exercise = db.getExerciseById(exerciseId);

        if (exercise == null || exerciseId == -1) {
            finish();
            return;
        }

        tvExerciseTitle = (TextView) findViewById(R.id.tvExerciseTitle);
        tvTypeBadge = (TextView) findViewById(R.id.tvTypeBadge);
        tvPRValue = (TextView) findViewById(R.id.tvPRValue);
        tvStatsSummary = (TextView) findViewById(R.id.tvStatsSummary);
        tvEmpty = (TextView) findViewById(R.id.tvEmpty);
        lvProgression = (ListView) findViewById(R.id.lvProgression);

        ((TextView) findViewById(R.id.tvBack)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        tvExerciseTitle.setText(exercise.name.toUpperCase());
        tvTypeBadge.setText(exercise.isStretch ? "[STRETCH]" : "[LIFT]");

        int totalSessions = db.getExerciseTotalSessions(exerciseId);
        String muscle = (exercise.muscleGroup != null && !exercise.muscleGroup.isEmpty())
                ? exercise.muscleGroup : "general";

        if (exercise.isStretch) {
            int maxHold = db.getExerciseMaxHold(exerciseId);
            tvPRValue.setText("LONGEST HOLD: " + (maxHold > 0 ? maxHold + "s" : "--"));
            tvStatsSummary.setText("Target: " + muscle + "  ·  " + totalSessions + " sessions logged");
        } else {
            double maxWeight = db.getExerciseMaxWeight(exerciseId);
            String prStr = maxWeight > 0 ? String.format(Locale.US, "%.1f kg", maxWeight) : "bodyweight";
            tvPRValue.setText("PERSONAL RECORD: " + (totalSessions > 0 ? prStr : "--"));
            tvStatsSummary.setText("Muscle: " + muscle + "  ·  " + totalSessions + " sessions logged");
        }

        loadHistoryList();
    }

    private void loadHistoryList() {
        List<SessionSet> rawSets = db.getExerciseHistory(exerciseId);

        if (rawSets.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            lvProgression.setVisibility(View.GONE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        lvProgression.setVisibility(View.VISIBLE);

        // Group sets by sessionId preserving order (newest first)
        Map<Long, SessionGroup> groupMap = new LinkedHashMap<Long, SessionGroup>();
        for (SessionSet s : rawSets) {
            SessionGroup group = groupMap.get(s.sessionId);
            if (group == null) {
                group = new SessionGroup();
                group.sessionId = s.sessionId;
                group.dateMs = s.dateMs;
                group.planName = s.planName;
                groupMap.put(s.sessionId, group);
            }
            group.sets.add(s);
        }

        final List<SessionGroup> sessionList = new ArrayList<SessionGroup>(groupMap.values());

        lvProgression.setAdapter(new ArrayAdapter<SessionGroup>(this, R.layout.item_progression_session, sessionList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_progression_session, parent, false);
                }
                SessionGroup sg = sessionList.get(position);

                TextView tvDatePlan = (TextView) convertView.findViewById(R.id.tvSessionDatePlan);
                TextView tvBreakdown = (TextView) convertView.findViewById(R.id.tvSetsBreakdown);

                String dateStr = new SimpleDateFormat("EEE dd MMM yyyy", Locale.US).format(new Date(sg.dateMs));
                tvDatePlan.setText(dateStr.toLowerCase() + "  ·  " + (sg.planName != null ? sg.planName : "workout"));

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < sg.sets.size(); i++) {
                    if (i > 0) sb.append("  ·  ");
                    SessionSet s = sg.sets.get(i);
                    if (s.isStretch) {
                        sb.append(s.reps).append("s hold");
                    } else {
                        String w = s.weight > 0 ? String.format(Locale.US, "%.1fkg", s.weight) : "bw";
                        sb.append(s.reps).append("x").append(w);
                    }
                }
                tvBreakdown.setText(sb.toString());

                return convertView;
            }
        });
    }
}
