package com.gymbb.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class AddExerciseToPlanActivity extends Activity {

    private DatabaseHelper db;
    private long planId;
    private boolean isStretch;

    private TextView tvTitle, tvRepsLabel;
    private AutoCompleteTextView etExerciseName;
    private EditText etMuscleGroup, etSets, etReps, etWeight, etRest;
    private LinearLayout layoutWeightSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_exercise_to_plan);

        db = new DatabaseHelper(this);
        planId = getIntent().getLongExtra("plan_id", -1);
        isStretch = getIntent().getBooleanExtra("is_stretch", false);

        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvRepsLabel = (TextView) findViewById(R.id.tvRepsLabel);
        etExerciseName = (AutoCompleteTextView) findViewById(R.id.etExerciseName);
        etMuscleGroup = (EditText) findViewById(R.id.etMuscleGroup);
        etSets = (EditText) findViewById(R.id.etSets);
        etReps = (EditText) findViewById(R.id.etReps);
        etWeight = (EditText) findViewById(R.id.etWeight);
        etRest = (EditText) findViewById(R.id.etRest);
        layoutWeightSection = (LinearLayout) findViewById(R.id.layoutWeightSection);

        if (isStretch) {
            tvTitle.setText("add stretch");
            etExerciseName.setHint("e.g. hamstring stretch, pigeon pose...");
            etMuscleGroup.setHint("e.g. hamstrings, hips, chest...");
            tvRepsLabel.setText("hold duration (seconds)");
            etSets.setText("2");
            etReps.setText("30");
            etRest.setText("15");
            if (layoutWeightSection != null) layoutWeightSection.setVisibility(View.GONE);
        }

        // Auto-complete suggestions matching type
        List<Exercise> existing = db.getAllExercises();
        List<String> names = new ArrayList<String>();
        for (Exercise e : existing) {
            if (e.name != null && !e.name.isEmpty()) {
                if (e.isStretch == isStretch) {
                    names.add(e.name);
                }
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_dropdown_item_1line, names);
        etExerciseName.setAdapter(adapter);
        etExerciseName.setThreshold(1);

        ((TextView) findViewById(R.id.tvCancel)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        ((TextView) findViewById(R.id.tvSave)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { addExercise(); }
        });
    }

    private void addExercise() {
        String name = etExerciseName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, isStretch ? "enter stretch name" : "enter exercise name", Toast.LENGTH_SHORT).show();
            return;
        }

        String muscle = etMuscleGroup.getText().toString().trim();
        int sets = parseIntSafe(etSets.getText().toString(), isStretch ? 2 : 3);
        int reps = parseIntSafe(etReps.getText().toString(), isStretch ? 30 : 10);
        double weight = isStretch ? 0 : parseDoubleSafe(etWeight.getText().toString(), 0);
        int rest = parseIntSafe(etRest.getText().toString(), isStretch ? 15 : 60);

        long exerciseId = db.getOrCreateExercise(name, muscle, isStretch);
        db.insertPlanExercise(planId, exerciseId, sets, reps, weight, rest, isStretch);
        finish();
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private double parseDoubleSafe(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }
}
