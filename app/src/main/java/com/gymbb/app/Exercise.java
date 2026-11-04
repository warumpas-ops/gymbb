package com.gymbb.app;

public class Exercise {
    public long id;
    public String name;
    public String muscleGroup;
    public String notes;

    public Exercise() {}

    public Exercise(long id, String name, String muscleGroup, String notes) {
        this.id = id;
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.notes = notes;
    }

    @Override
    public String toString() {
        return name;
    }
}
