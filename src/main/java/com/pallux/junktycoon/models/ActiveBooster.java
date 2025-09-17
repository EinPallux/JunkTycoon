package com.pallux.junktycoon.models;

public class ActiveBooster {

    private final String id;
    private final String name;
    private final String type;
    private final double multiplier;
    private final long endTime;
    private final String activatedBy;

    public ActiveBooster(String id, String name, String type, double multiplier, long endTime, String activatedBy) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.multiplier = multiplier;
        this.endTime = endTime;
        this.activatedBy = activatedBy;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getActivatedBy() {
        return activatedBy;
    }

    public boolean isActive() {
        return System.currentTimeMillis() < endTime;
    }

    public long getRemainingTime() {
        return Math.max(0, endTime - System.currentTimeMillis());
    }
}