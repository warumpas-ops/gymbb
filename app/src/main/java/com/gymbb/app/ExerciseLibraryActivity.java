package com.gymbb.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.content.Intent;

import java.util.List;

public class ExerciseLibraryActivity extends Activity {

    private DatabaseHelper db;
    private ListView lvExercises;
    private TextView tvEmpty;
    private List<Exercise> exercises;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_library);

        db = new DatabaseHelper(this);
        lvExercises = (ListView) findViewById(R.id.lvExercises);
        tvEmpty = (TextView) findViewById(R.id.tvEmpty);

        ((TextView) findViewById(R.id.tvBack)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        ((TextView) findViewById(R.id.tvNewExercise)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(ExerciseLibraryActivity.this, NewExerciseActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadExercises();
    }

    private void loadExercises() {
        exercises = db.getAllExercises();
        if (exercises.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            lvExercises.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            lvExercises.setVisibility(View.VISIBLE);
            lvExercises.setAdapter(new ArrayAdapter<Exercise>(this, R.layout.item_exercise, R.id.tvExerciseName, exercises) {
                @Override
                public View getView(final int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    final Exercise e = exercises.get(position);
                    ((TextView) v.findViewById(R.id.tvExerciseName)).setText(e.name);
                    String muscle = e.muscleGroup != null ? e.muscleGroup : "";
                    ((TextView) v.findViewById(R.id.tvMuscleGroup)).setText(muscle);
                    v.findViewById(R.id.tvDelete).setOnClickListener(new View.OnClickListener() {
                        public void onClick(View view) {
                            db.deleteExercise(e.id);
                            exercises.remove(position);
                            notifyDataSetChanged();
                            if (exercises.isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                                lvExercises.setVisibility(View.GONE);
                            }
                        }
                    });
                    return v;
                }
            });
        }
    }
}
