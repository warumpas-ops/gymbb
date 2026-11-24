package com.gymbb.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class PlanDetailActivity extends Activity {

    private DatabaseHelper db;
    private long planId;
    private Plan plan;
    private List<PlanExercise> exercises;
    private ListView lvExercises;
    private TextView tvPlanName;

    private static final String[] DAY_CODES = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
    private static final String[] DAY_SHORT = {"M", "T", "W", "T", "F", "S", "S"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_detail);

        db = new DatabaseHelper(this);
        planId = getIntent().getLongExtra("plan_id", -1);

        tvPlanName = (TextView) findViewById(R.id.tvPlanName);
        lvExercises = (ListView) findViewById(R.id.lvExercises);

        ((TextView) findViewById(R.id.tvBack)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        ((TextView) findViewById(R.id.tvDelete)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { confirmDelete(); }
        });

        ((TextView) findViewById(R.id.tvAddExercise)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(PlanDetailActivity.this, AddExerciseToPlanActivity.class);
                i.putExtra("plan_id", planId);
                i.putExtra("is_stretch", false);
                startActivity(i);
            }
        });

        ((TextView) findViewById(R.id.tvAddStretch)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(PlanDetailActivity.this, AddExerciseToPlanActivity.class);
                i.putExtra("plan_id", planId);
                i.putExtra("is_stretch", true);
                startActivity(i);
            }
        });

        ((TextView) findViewById(R.id.tvAddGroup)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showGroupPicker();
            }
        });

        ((TextView) findViewById(R.id.tvStartWorkout)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (exercises != null && !exercises.isEmpty()) {
                    Intent i = new Intent(PlanDetailActivity.this, WorkoutActivity.class);
                    i.putExtra("plan_id", planId);
                    startActivity(i);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPlan();
    }

    private void loadPlan() {
        plan = db.getPlanById(planId);
        if (plan == null) { finish(); return; }

        LinearLayout layoutDays = (LinearLayout) findViewById(R.id.layoutDays);
        TextView tvGroupSubtitle = (TextView) findViewById(R.id.tvGroupSubtitle);
        View tvAddGroup = findViewById(R.id.tvAddGroup);
        View divGroup = findViewById(R.id.divGroup);
        View tvStartWorkout = findViewById(R.id.tvStartWorkout);
        View divStart = findViewById(R.id.divStart);

        if (plan.isGroup) {
            tvPlanName.setText("[GROUP] " + plan.name);
            tvPlanName.setTextColor(0xFFA8D8A8);
            layoutDays.setVisibility(View.GONE);
            tvGroupSubtitle.setVisibility(View.VISIBLE);
            tvAddGroup.setVisibility(View.GONE);
            divGroup.setVisibility(View.GONE);
            tvStartWorkout.setVisibility(View.GONE);
            divStart.setVisibility(View.GONE);
        } else {
            tvPlanName.setText(plan.name);
            tvPlanName.setTextColor(0xFFE0E2E6);
            layoutDays.setVisibility(View.VISIBLE);
            tvGroupSubtitle.setVisibility(View.GONE);
            tvAddGroup.setVisibility(View.VISIBLE);
            divGroup.setVisibility(View.VISIBLE);
            tvStartWorkout.setVisibility(View.VISIBLE);
            divStart.setVisibility(View.VISIBLE);

            // Build day strip
            layoutDays.removeAllViews();
            String[] planDays = plan.days != null ? plan.days.split(",") : new String[0];
            for (int i = 0; i < DAY_CODES.length; i++) {
                final String code = DAY_CODES[i];
                boolean active = false;
                for (String d : planDays) {
                    if (d.trim().equalsIgnoreCase(code)) { active = true; break; }
                }
                TextView tv = new TextView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                tv.setLayoutParams(lp);
                tv.setText(DAY_SHORT[i]);
                tv.setTextColor(active ? 0xFFA8D8A8 : 0xFF555A64);
                tv.setTypeface(android.graphics.Typeface.MONOSPACE);
                tv.setTextSize(13f);
                tv.setGravity(android.view.Gravity.CENTER);
                if (active) tv.setTextColor(0xFFA8D8A8);
                layoutDays.addView(tv);
            }
        }

        exercises = db.getPlanExercises(planId);
        lvExercises.setAdapter(new ArrayAdapter<PlanExercise>(this, R.layout.item_plan_exercise, R.id.tvExerciseName, exercises) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                final PlanExercise pe = exercises.get(position);
                String prefix = pe.isStretch ? "[stretch] " : "";
                ((TextView) v.findViewById(R.id.tvExerciseName)).setText(prefix + pe.exerciseName);
                ((TextView) v.findViewById(R.id.tvExerciseDetail)).setText(pe.getDetailText());
                v.findViewById(R.id.tvDelete).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        db.deletePlanExercise(pe.id);
                        exercises.remove(position);
                        notifyDataSetChanged();
                    }
                });
                return v;
            }
        });
    }

    private void showGroupPicker() {
        final List<Plan> groups = db.getAllGroups();
        if (groups.isEmpty()) {
            Toast.makeText(this, "no groups yet — create one under plans > groups", Toast.LENGTH_LONG).show();
            return;
        }
        String[] labels = new String[groups.size()];
        for (int i = 0; i < groups.size(); i++) {
            Plan g = groups.get(i);
            int count = db.getPlanExerciseCount(g.id);
            labels[i] = g.name + " (" + count + (count == 1 ? " item)" : " items)");
        }
        new AlertDialog.Builder(this)
                .setTitle("add group to " + plan.name)
                .setItems(labels, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Plan chosen = groups.get(which);
                        int added = db.copyGroupToPlan(chosen.id, planId);
                        if (added == 0) {
                            Toast.makeText(PlanDetailActivity.this, "\"" + chosen.name + "\" is empty", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(PlanDetailActivity.this, "added " + added + " from " + chosen.name, Toast.LENGTH_SHORT).show();
                            loadPlan();
                        }
                    }
                })
                .setNegativeButton("cancel", null)
                .show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(plan != null && plan.isGroup ? "delete group?" : "delete plan?")
                .setMessage("\"" + plan.name + "\" will be removed.")
                .setPositiveButton("delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        db.deletePlan(planId);
                        finish();
                    }
                })
                .setNegativeButton("cancel", null)
                .show();
    }
}
