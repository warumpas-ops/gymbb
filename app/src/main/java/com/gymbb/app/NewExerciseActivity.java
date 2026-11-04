package com.gymbb.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class NewExerciseActivity extends Activity {

    private DatabaseHelper db;
    private EditText etName, etMuscleGroup, etNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_exercise);

        db = new DatabaseHelper(this);

        etName = (EditText) findViewById(R.id.etName);
        etMuscleGroup = (EditText) findViewById(R.id.etMuscleGroup);
        etNotes = (EditText) findViewById(R.id.etNotes);

        ((TextView) findViewById(R.id.tvCancel)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        ((TextView) findViewById(R.id.tvSave)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { saveExercise(); }
        });
    }

    private void saveExercise() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "enter exercise name", Toast.LENGTH_SHORT).show();
            return;
        }
        String muscle = etMuscleGroup.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        db.insertExercise(name, muscle, notes);
        finish();
    }
}
