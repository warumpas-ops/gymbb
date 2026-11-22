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
    private TextView btnTypeToggle;
    private boolean isStretch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_exercise);

        db = new DatabaseHelper(this);

        etName = (EditText) findViewById(R.id.etName);
        etMuscleGroup = (EditText) findViewById(R.id.etMuscleGroup);
        etNotes = (EditText) findViewById(R.id.etNotes);
        btnTypeToggle = (TextView) findViewById(R.id.btnTypeToggle);

        btnTypeToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                isStretch = !isStretch;
                if (isStretch) {
                    btnTypeToggle.setText("[type: stretch / mobility]");
                    btnTypeToggle.setTextColor(0xFFA8D8A8);
                    etName.setHint("e.g. hamstring stretch, cobra pose");
                    etMuscleGroup.setHint("e.g. hamstrings, hips, chest");
                } else {
                    btnTypeToggle.setText("[type: strength exercise]");
                    btnTypeToggle.setTextColor(0xFFA8D8A8);
                    etName.setHint("e.g. bench press, pull-ups");
                    etMuscleGroup.setHint("e.g. chest, back, legs");
                }
            }
        });

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
            Toast.makeText(this, isStretch ? "enter stretch name" : "enter exercise name", Toast.LENGTH_SHORT).show();
            return;
        }
        String muscle = etMuscleGroup.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        db.insertExercise(name, muscle, notes, isStretch);
        finish();
    }
}
