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
    private boolean stretchIsRepsMode = false;

    private TextView tvTitle, tvRepsLabel;
    private TextView btnPlanStretchHold, btnPlanStretchReps;
    private LinearLayout layoutStretchModeToggle;
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
        btnPlanStretchHold = (TextView) findViewById(R.id.btnPlanStretchHold);
        btnPlanStretchReps = (TextView) findViewById(R.id.btnPlanStretchReps);
        layoutStretchModeToggle = (LinearLayout) findViewById(R.id.layoutStretchModeToggle);
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
            if (layoutStretchModeToggle != null) layoutStretchModeToggle.setVisibility(View.VISIBLE);

            btnPlanStretchHold.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    stretchIsRepsMode = false;
                    updateStretchModeButtons();
                }
            });
            btnPlanStretchReps.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    stretchIsRepsMode = true;
                    updateStretchModeButtons();
                }
            });
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

    private void updateStretchModeButtons() {
        if (stretchIsRepsMode) {
            btnPlanStretchHold.setTextColor(0xFFA8D8A8);
            btnPlanStretchHold.setBackgroundColor(0xFF1A1C20);
            btnPlanStretchReps.setTextColor(0xFF0D0F11);
            btnPlanStretchReps.setBackgroundColor(0xFFA8D8A8);
            tvRepsLabel.setText("reps");
            etReps.setText("10");
        } else {
            btnPlanStretchHold.setTextColor(0xFF0D0F11);
            btnPlanStretchHold.setBackgroundColor(0xFFA8D8A8);
            btnPlanStretchReps.setTextColor(0xFFA8D8A8);
            btnPlanStretchReps.setBackgroundColor(0xFF1A1C20);
            tvRepsLabel.setText("hold duration (seconds)");
            etReps.setText("30");
        }
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
        // For stretches: weight=-1 means reps mode, weight=0 means hold mode
        double weight = isStretch ? (stretchIsRepsMode ? -1.0 : 0.0) : parseDoubleSafe(etWeight.getText().toString(), 0);
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
