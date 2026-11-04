package com.gymbb.app;

public class Plan {
    public long id;
    public String name;
    public String days; // comma-separated: "MON,WED,FRI"

    public Plan() {}

    public Plan(long id, String name, String days) {
        this.id = id;
        this.name = name;
        this.days = days;
    }

    public String getFormattedDays() {
        if (days == null || days.isEmpty()) return "no days";
        StringBuilder sb = new StringBuilder();
        String[] parts = days.split(",");
        for (String d : parts) {
            if (sb.length() > 0) sb.append(" ");
            if (d.length() >= 3) sb.append(d.substring(0, 3).toLowerCase());
        }
        return sb.toString();
    }

    public boolean includesDay(String dayName) {
        if (days == null || days.isEmpty()) return false;
        for (String d : days.split(",")) {
            if (d.trim().equalsIgnoreCase(dayName)) return true;
        }
        return false;
    }
}
