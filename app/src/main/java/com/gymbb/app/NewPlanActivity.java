package com.gymbb.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class NewPlanActivity extends Activity {

    private DatabaseHelper db;
    private boolean isGroup = false;
    private EditText etPlanName;
    private CheckBox cbMon, cbTue, cbWed, cbThu, cbFri, cbSat, cbSun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_plan);

        db = new DatabaseHelper(this);
        isGroup = getIntent().getBooleanExtra("is_group", false);

        etPlanName = (EditText) findViewById(R.id.etPlanName);
        cbMon = (CheckBox) findViewById(R.id.cbMon);
        cbTue = (CheckBox) findViewById(R.id.cbTue);
        cbWed = (CheckBox) findViewById(R.id.cbWed);
        cbThu = (CheckBox) findViewById(R.id.cbThu);
        cbFri = (CheckBox) findViewById(R.id.cbFri);
        cbSat = (CheckBox) findViewById(R.id.cbSat);
        cbSun = (CheckBox) findViewById(R.id.cbSun);

        if (isGroup) {
            ((TextView) findViewById(R.id.tvTitle)).setText("new group");
            ((TextView) findViewById(R.id.tvNameLabel)).setText("group name");
            etPlanName.setHint("e.g. morning routine");
            ((LinearLayout) findViewById(R.id.layoutDaysSection)).setVisibility(View.GONE);
        }

        ((TextView) findViewById(R.id.tvCancel)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        ((TextView) findViewById(R.id.tvSave)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { savePlan(); }
        });
    }

    private void savePlan() {
        String name = etPlanName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, isGroup ? "enter a group name" : "enter a plan name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isGroup) {
            long newId = db.insertPlan(name, "", true);
            Intent i = new Intent(this, PlanDetailActivity.class);
            i.putExtra("plan_id", newId);
            startActivity(i);
            finish();
            return;
        }

        List<String> days = new ArrayList<String>();
        if (cbMon.isChecked()) days.add("MON");
        if (cbTue.isChecked()) days.add("TUE");
        if (cbWed.isChecked()) days.add("WED");
        if (cbThu.isChecked()) days.add("THU");
        if (cbFri.isChecked()) days.add("FRI");
        if (cbSat.isChecked()) days.add("SAT");
        if (cbSun.isChecked()) days.add("SUN");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < days.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(days.get(i));
        }

        db.insertPlan(name, sb.toString());
        finish();
    }
}
