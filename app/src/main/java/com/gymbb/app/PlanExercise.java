package com.gymbb.app;

public class PlanExercise {
    public long id;
    public long planId;
    public long exerciseId;
    public String exerciseName;
    public String muscleGroup;
    public int sets;
    public int reps;
    public double weight;
    public int restSeconds;
    public int sortOrder;
    public boolean isStretch;

    public PlanExercise() {}

    public String getDetailText() {
        if (isStretch) {
            return sets + "x" + reps + "s hold  " + restSeconds + "s rest";
        }
        String w = weight > 0
                ? String.format("%.1fkg", weight)
                : "bw";
        return sets + "x" + reps + "  " + w + "  " + restSeconds + "s rest";
    }
}
