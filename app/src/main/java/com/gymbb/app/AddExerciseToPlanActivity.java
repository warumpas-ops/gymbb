package com.gymbb.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class AddExerciseToPlanActivity extends Activity {

    private DatabaseHelper db;
    private long planId;
    private Spinner spinnerExercise;
    private EditText etSets, etReps, etWeight, etRest;
    private List<Exercise> exercises;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_exercise_to_plan);

        db = new DatabaseHelper(this);
        planId = getIntent().getLongExtra("plan_id", -1);

        spinnerExercise = (Spinner) findViewById(R.id.spinnerExercise);
        etSets = (EditText) findViewById(R.id.etSets);
        etReps = (EditText) findViewById(R.id.etReps);
        etWeight = (EditText) findViewById(R.id.etWeight);
        etRest = (EditText) findViewById(R.id.etRest);

        exercises = db.getAllExercises();
        ArrayAdapter<Exercise> adapter = new ArrayAdapter<Exercise>(
                this, android.R.layout.simple_spinner_item, exercises) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(0xFFE0E2E6);
                tv.setTypeface(android.graphics.Typeface.MONOSPACE);
                tv.setBackgroundColor(0xFF1A1C20);
                return tv;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(0xFFE0E2E6);
                tv.setTypeface(android.graphics.Typeface.MONOSPACE);
                tv.setBackgroundColor(0xFF1A1C20);
                tv.setPadding(16, 12, 16, 12);
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExercise.setAdapter(adapter);

        ((TextView) findViewById(R.id.tvCancel)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        ((TextView) findViewById(R.id.tvSave)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { addExercise(); }
        });
    }

    private void addExercise() {
        if (exercises.isEmpty()) {
            Toast.makeText(this, "no exercises in library", Toast.LENGTH_SHORT).show();
            return;
        }

        Exercise selected = exercises.get(spinnerExercise.getSelectedItemPosition());
        int sets = parseIntSafe(etSets.getText().toString(), 3);
        int reps = parseIntSafe(etReps.getText().toString(), 10);
        double weight = parseDoubleSafe(etWeight.getText().toString(), 0);
        int rest = parseIntSafe(etRest.getText().toString(), 60);

        db.insertPlanExercise(planId, selected.id, sets, reps, weight, rest);
        finish();
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private double parseDoubleSafe(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }
}
