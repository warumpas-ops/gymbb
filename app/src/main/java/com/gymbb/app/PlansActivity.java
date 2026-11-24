package com.gymbb.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class PlansActivity extends Activity {

    private DatabaseHelper db;
    private ListView lvPlans;
    private TextView tvEmpty, tabPlans, tabGroups;
    private List<Plan> plans;
    private boolean showingGroups = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plans);

        db = new DatabaseHelper(this);
        lvPlans = (ListView) findViewById(R.id.lvPlans);
        tvEmpty = (TextView) findViewById(R.id.tvEmpty);
        tabPlans = (TextView) findViewById(R.id.tabPlans);
        tabGroups = (TextView) findViewById(R.id.tabGroups);

        ((TextView) findViewById(R.id.tvBack)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        tabPlans.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (showingGroups) {
                    showingGroups = false;
                    updateTabs();
                    loadPlans();
                }
            }
        });

        tabGroups.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!showingGroups) {
                    showingGroups = true;
                    updateTabs();
                    loadPlans();
                }
            }
        });

        ((TextView) findViewById(R.id.tvNewPlan)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(PlansActivity.this, NewPlanActivity.class);
                i.putExtra("is_group", showingGroups);
                startActivity(i);
            }
        });

        lvPlans.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Plan p = plans.get(position);
                Intent i = new Intent(PlansActivity.this, PlanDetailActivity.class);
                i.putExtra("plan_id", p.id);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPlans();
    }

    private void updateTabs() {
        if (showingGroups) {
            tabPlans.setTextColor(0xFF555A64);
            tabPlans.setBackgroundColor(0xFF1A1C20);
            tabGroups.setTextColor(0xFF0D0F11);
            tabGroups.setBackgroundColor(0xFFA8D8A8);
        } else {
            tabPlans.setTextColor(0xFF0D0F11);
            tabPlans.setBackgroundColor(0xFFA8D8A8);
            tabGroups.setTextColor(0xFF555A64);
            tabGroups.setBackgroundColor(0xFF1A1C20);
        }
    }

    private void loadPlans() {
        plans = showingGroups ? db.getAllGroups() : db.getAllPlans();
        if (plans.isEmpty()) {
            tvEmpty.setText(showingGroups
                    ? "no groups yet. press [+] to create one (e.g. morning routine)."
                    : "no plans yet. press [+] to create one.");
            tvEmpty.setVisibility(View.VISIBLE);
            lvPlans.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            lvPlans.setVisibility(View.VISIBLE);
            lvPlans.setAdapter(new ArrayAdapter<Plan>(this, R.layout.item_plan, R.id.tvPlanName, plans) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    Plan p = plans.get(position);
                    TextView tvName = (TextView) v.findViewById(R.id.tvPlanName);
                    TextView tvDays = (TextView) v.findViewById(R.id.tvPlanDays);
                    tvName.setText(p.name);
                    if (showingGroups) {
                        int count = db.getPlanExerciseCount(p.id);
                        tvDays.setText(count + (count == 1 ? " item" : " items"));
                        tvName.setTextColor(0xFFA8D8A8);
                    } else {
                        tvDays.setText(p.getFormattedDays());
                        tvName.setTextColor(0xFFE0E2E6);
                    }
                    return v;
                }
            });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_N) {
            Intent i = new Intent(this, NewPlanActivity.class);
            i.putExtra("is_group", showingGroups);
            startActivity(i);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_G) {
            showingGroups = !showingGroups;
            updateTabs();
            loadPlans();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
