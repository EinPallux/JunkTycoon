package com.pallux.junktycoon.models;

import org.bukkit.Particle;
import org.bukkit.Sound;

public class TrashType {

    private final String id;
    private final String name;
    private final double chance;
    private final int minValue;
    private final int maxValue;
    private final String color;
    private Particle particle;
    private Sound sound;

    public TrashType(String id, String name, double chance, int minValue, int maxValue, String color) {
        this.id = id;
        this.name = name;
        this.chance = chance;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.color = color;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getChance() {
        return chance;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public String getColor() {
        return color;
    }

    public Particle getParticle() {
        return particle;
    }

    public void setParticle(Particle particle) {
        this.particle = particle;
    }

    public Sound getSound() {
        return sound;
    }

    public void setSound(Sound sound) {
        this.sound = sound;
    }

    public boolean hasParticle() {
        return particle != null;
    }

    public boolean hasSound() {
        return sound != null;
    }

    public String getFormattedName() {
        return color + name;
    }
}