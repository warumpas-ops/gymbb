package com.gymbb.app;

public class Exercise {
    public long id;
    public String name;
    public String muscleGroup;
    public String notes;
    public boolean isStretch;

    public Exercise() {}

    public Exercise(long id, String name, String muscleGroup, String notes, boolean isStretch) {
        this.id = id;
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.notes = notes;
        this.isStretch = isStretch;
    }

    public Exercise(long id, String name, String muscleGroup, String notes) {
        this(id, name, muscleGroup, notes, false);
    }

    @Override
    public String toString() {
        return name;
    }
}
