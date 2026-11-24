package com.gymbb.app;

public class SessionSet {
    public long id;
    public long sessionId;
    public long exerciseId;
    public String exerciseName;
    public int setNumber;
    public int reps;
    public double weight;
    public int completed;
    public boolean isStretch;
    public int stretchType; // 0: strength, 1: static hold, 2: dynamic reps
    public long dateMs;
    public String planName;

    public SessionSet() {}
}
