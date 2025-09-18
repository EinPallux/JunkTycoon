package com.pallux.junktycoon.data;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;

    // Trash Pick Data
    private String trashPickTier;
    private int trashPickLevel;
    private int trashPickXP;
    private long lastPickTime;

    // Perk Levels
    private int cooldownPerkLevel;
    private int multiplierPerkLevel;
    private int rarityPerkLevel;
    private int pointFinderPerkLevel;
    private int xpMultiplierPerkLevel;

    // Prestige Data
    private int prestigeLevel;

    // Statistics
    private int totalTrashPicked;
    private double totalMoneyEarned;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    // Getters and Setters
    public UUID getUuid() {
        return uuid;
    }

    public String getTrashPickTier() {
        return trashPickTier;
    }

    public void setTrashPickTier(String trashPickTier) {
        this.trashPickTier = trashPickTier;
    }

    public int getTrashPickLevel() {
        return trashPickLevel;
    }

    public void setTrashPickLevel(int trashPickLevel) {
        this.trashPickLevel = trashPickLevel;
    }

    public int getTrashPickXP() {
        return trashPickXP;
    }

    public void setTrashPickXP(int trashPickXP) {
        this.trashPickXP = trashPickXP;
    }

    public long getLastPickTime() {
        return lastPickTime;
    }

    public void setLastPickTime(long lastPickTime) {
        this.lastPickTime = lastPickTime;
    }

    public int getCooldownPerkLevel() {
        return cooldownPerkLevel;
    }

    public void setCooldownPerkLevel(int cooldownPerkLevel) {
        this.cooldownPerkLevel = cooldownPerkLevel;
    }

    public int getMultiplierPerkLevel() {
        return multiplierPerkLevel;
    }

    public void setMultiplierPerkLevel(int multiplierPerkLevel) {
        this.multiplierPerkLevel = multiplierPerkLevel;
    }

    public int getRarityPerkLevel() {
        return rarityPerkLevel;
    }

    public void setRarityPerkLevel(int rarityPerkLevel) {
        this.rarityPerkLevel = rarityPerkLevel;
    }

    public int getPointFinderPerkLevel() {
        return pointFinderPerkLevel;
    }

    public void setPointFinderPerkLevel(int pointFinderPerkLevel) {
        this.pointFinderPerkLevel = pointFinderPerkLevel;
    }

    public int getXpMultiplierPerkLevel() {
        return xpMultiplierPerkLevel;
    }

    public void setXpMultiplierPerkLevel(int xpMultiplierPerkLevel) {
        this.xpMultiplierPerkLevel = xpMultiplierPerkLevel;
    }

    public int getPrestigeLevel() {
        return prestigeLevel;
    }

    public void setPrestigeLevel(int prestigeLevel) {
        this.prestigeLevel = prestigeLevel;
    }

    public int getTotalTrashPicked() {
        return totalTrashPicked;
    }

    public void setTotalTrashPicked(int totalTrashPicked) {
        this.totalTrashPicked = totalTrashPicked;
    }

    public void addTrashPicked(int amount) {
        this.totalTrashPicked += amount;
    }

    public double getTotalMoneyEarned() {
        return totalMoneyEarned;
    }

    public void setTotalMoneyEarned(double totalMoneyEarned) {
        this.totalMoneyEarned = totalMoneyEarned;
    }

    public void addMoneyEarned(double amount) {
        this.totalMoneyEarned += amount;
    }

    // Utility methods
    public void addXP(int xp) {
        this.trashPickXP += xp;
    }

    public boolean canLevelUp(int requiredXP) {
        return this.trashPickXP >= requiredXP;
    }

    public void levelUp() {
        this.trashPickLevel++;
        // XP carries over after level up
    }

    public boolean canUpgradeTier(int requiredLevel) {
        return this.trashPickLevel >= requiredLevel;
    }

    public void upgradeTier(String newTier) {
        this.trashPickTier = newTier;
        this.trashPickLevel = 1;  // Reset level when upgrading tier
        this.trashPickXP = 0;     // Reset XP when upgrading tier for clean progression
        // Keep perks intact
    }
}