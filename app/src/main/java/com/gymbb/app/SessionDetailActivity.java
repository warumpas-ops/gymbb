package com.gymbb.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionDetailActivity extends Activity {

    private DatabaseHelper db;
    private long sessionId;
    private WorkoutSession session;
    private List<SessionSet> sets;

    private TextView tvPlanDate, tvDuration, tvStatsSummary;
    private ListView lvSets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_detail);

        db = new DatabaseHelper(this);
        sessionId = getIntent().getLongExtra("session_id", -1);
        session = db.getSessionById(sessionId);

        if (session == null || sessionId == -1) {
            finish();
            return;
        }

        tvPlanDate = (TextView) findViewById(R.id.tvPlanDate);
        tvDuration = (TextView) findViewById(R.id.tvDuration);
        tvStatsSummary = (TextView) findViewById(R.id.tvStatsSummary);
        lvSets = (ListView) findViewById(R.id.lvSets);

        ((TextView) findViewById(R.id.tvBack)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        String dateStr = new SimpleDateFormat("EEE dd MMM yyyy", Locale.US).format(new Date(session.dateMs));
        tvPlanDate.setText((session.planName != null ? session.planName : "Workout") + "  ·  " + dateStr.toLowerCase());

        int m = session.durationSeconds / 60;
        int s = session.durationSeconds % 60;
        tvDuration.setText(String.format(Locale.US, "%02d:%02d", m, s));

        sets = db.getSessionSets(sessionId);
        int totalSets = sets.size();
        int totalExercises = db.countDistinctExercisesInSession(sessionId);
        tvStatsSummary.setText(totalExercises + " exercises  ·  " + totalSets + " logged sets");

        lvSets.setAdapter(new ArrayAdapter<SessionSet>(this, R.layout.item_session_set, sets) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_session_set, parent, false);
                }
                SessionSet current = sets.get(position);
                SessionSet prev = position > 0 ? sets.get(position - 1) : null;

                TextView tvHeader = (TextView) convertView.findViewById(R.id.tvHeaderExercise);
                TextView tvSetNum = (TextView) convertView.findViewById(R.id.tvSetNumber);
                TextView tvSetDetail = (TextView) convertView.findViewById(R.id.tvSetDetail);

                // Show exercise header if this is the first set of this exercise
                if (prev == null || !current.exerciseName.equalsIgnoreCase(prev.exerciseName)) {
                    String prefix = current.isStretch ? "[STRETCH] " : "";
                    tvHeader.setText(prefix + current.exerciseName.toUpperCase());
                    tvHeader.setVisibility(View.VISIBLE);
                } else {
                    tvHeader.setVisibility(View.GONE);
                }

                tvSetNum.setText("  set " + current.setNumber + ":");

                if (current.isStretch) {
                    tvSetDetail.setText(current.reps + "s hold");
                } else {
                    String weightStr = current.weight > 0
                            ? String.format(Locale.US, "%.1f kg", current.weight)
                            : "bodyweight";
                    tvSetDetail.setText(current.reps + " reps @ " + weightStr);
                }

                return convertView;
            }
        });
    }
}
