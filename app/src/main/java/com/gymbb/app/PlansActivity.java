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
    private TextView tvEmpty;
    private List<Plan> plans;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plans);

        db = new DatabaseHelper(this);
        lvPlans = (ListView) findViewById(R.id.lvPlans);
        tvEmpty = (TextView) findViewById(R.id.tvEmpty);

        ((TextView) findViewById(R.id.tvBack)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        ((TextView) findViewById(R.id.tvNewPlan)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(PlansActivity.this, NewPlanActivity.class));
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

    private void loadPlans() {
        plans = db.getAllPlans();
        if (plans.isEmpty()) {
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
                    ((TextView) v.findViewById(R.id.tvPlanName)).setText(p.name);
                    ((TextView) v.findViewById(R.id.tvPlanDays)).setText(p.getFormattedDays());
                    return v;
                }
            });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_N) {
            startActivity(new Intent(this, NewPlanActivity.class));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
