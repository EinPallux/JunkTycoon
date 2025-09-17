package com.pallux.junktycoon.models;

import org.bukkit.Material;

public class TrashPickTier {

    private final String id;
    private final String name;
    private final Material material;
    private final int levelRequired;
    private final int maxLevel;
    private final double upgradeCost;

    public TrashPickTier(String id, String name, Material material, int levelRequired, int maxLevel, double upgradeCost) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.levelRequired = levelRequired;
        this.maxLevel = maxLevel;
        this.upgradeCost = upgradeCost;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Material getMaterial() {
        return material;
    }

    public int getLevelRequired() {
        return levelRequired;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public double getUpgradeCost() {
        return upgradeCost;
    }

    public boolean hasMaxLevel() {
        return maxLevel != -1;
    }
}