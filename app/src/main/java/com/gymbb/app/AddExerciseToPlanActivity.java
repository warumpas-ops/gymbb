package com.gymbb.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class AddExerciseToPlanActivity extends Activity {

    private DatabaseHelper db;
    private long planId;
    private AutoCompleteTextView etExerciseName;
    private EditText etMuscleGroup, etSets, etReps, etWeight, etRest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_exercise_to_plan);

        db = new DatabaseHelper(this);
        planId = getIntent().getLongExtra("plan_id", -1);

        etExerciseName = (AutoCompleteTextView) findViewById(R.id.etExerciseName);
        etMuscleGroup = (EditText) findViewById(R.id.etMuscleGroup);
        etSets = (EditText) findViewById(R.id.etSets);
        etReps = (EditText) findViewById(R.id.etReps);
        etWeight = (EditText) findViewById(R.id.etWeight);
        etRest = (EditText) findViewById(R.id.etRest);

        // Auto-complete suggestions from existing exercises (if any)
        List<Exercise> existing = db.getAllExercises();
        List<String> names = new ArrayList<String>();
        for (Exercise e : existing) {
            if (e.name != null && !e.name.isEmpty()) {
                names.add(e.name);
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
            Toast.makeText(this, "enter exercise name", Toast.LENGTH_SHORT).show();
            return;
        }

        String muscle = etMuscleGroup.getText().toString().trim();
        int sets = parseIntSafe(etSets.getText().toString(), 3);
        int reps = parseIntSafe(etReps.getText().toString(), 10);
        double weight = parseDoubleSafe(etWeight.getText().toString(), 0);
        int rest = parseIntSafe(etRest.getText().toString(), 60);

        long exerciseId = db.getOrCreateExercise(name, muscle);
        db.insertPlanExercise(planId, exerciseId, sets, reps, weight, rest);
        finish();
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private double parseDoubleSafe(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }
}
