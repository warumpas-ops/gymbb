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

        loadHistory();
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
    }
}
