package com.pallux.junktycoon.placeholders;

import com.pallux.junktycoon.JunkTycoon;
import com.pallux.junktycoon.data.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JunkTycoonExpansion extends PlaceholderExpansion {

    private final JunkTycoon plugin;

    public JunkTycoonExpansion(JunkTycoon plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "jt";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Pallux";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return ""; // Placeholder is not for a player
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null) {
            return ""; // Player data not found
        }

        // Parse the placeholder parameters
        return switch (params.toLowerCase()) {
            case "prestige_level" -> String.valueOf(playerData.getPrestigeLevel());
            case "trash_pick_level" -> String.valueOf(playerData.getTrashPickLevel());
            case "trash_pick_xp" -> String.valueOf(playerData.getTrashPickXP());
            case "trash_picked" -> String.valueOf(playerData.getTotalTrashPicked());
            case "money_earned" -> formatMoney(playerData.getTotalMoneyEarned());

            // Additional useful placeholders
            case "trash_pick_tier" -> getTierDisplayName(playerData.getTrashPickTier());
            case "prestige_name" -> getPrestigeName(playerData.getPrestigeLevel());
            case "money_bonus" -> getMoneyBonusPercentage(playerData.getPrestigeLevel());
            case "xp_bonus" -> String.valueOf(plugin.getPrestigeManager().getXPBonus(playerData.getPrestigeLevel()));
            case "cooldown_perk" -> String.valueOf(playerData.getCooldownPerkLevel());
            case "multiplier_perk" -> String.valueOf(playerData.getMultiplierPerkLevel());
            case "rarity_perk" -> String.valueOf(playerData.getRarityPerkLevel());
            case "point_finder_perk" -> String.valueOf(playerData.getPointFinderPerkLevel());
            case "xp_multiplier_perk" -> String.valueOf(playerData.getXpMultiplierPerkLevel());
            case "required_xp" -> String.valueOf(plugin.getTrashPickManager().calculateRequiredXP(playerData.getTrashPickLevel()));
            case "xp_progress_percentage" -> calculateXPProgress(playerData);

            default -> null; // Placeholder is not recognized by the Expansion
        };
    }

    private String formatMoney(double amount) {
        if (plugin.getVaultHook() != null && plugin.getVaultHook().getEconomy() != null) {
            return plugin.getVaultHook().formatMoney(amount);
        }

        // Fallback formatting if Vault is not available
        if (amount >= 1000000000) {
            return String.format("%.1fB", amount / 1000000000.0);
        } else if (amount >= 1000000) {
            return String.format("%.1fM", amount / 1000000.0);
        } else if (amount >= 1000) {
            return String.format("%.1fK", amount / 1000.0);
        } else {
            return String.format("%.2f", amount);
        }
    }

    private String getTierDisplayName(String tierId) {
        if (tierId == null) return "Unknown";

        String tierName = plugin.getConfigManager().getMainConfig().getString("trash_picks." + tierId + ".name", "Unknown Tier");
        return plugin.getConfigManager().formatText(tierName);
    }

    private String getPrestigeName(int prestigeLevel) {
        if (prestigeLevel <= 0) return "None";

        return plugin.getPrestigeManager().getPrestigeLevelName(prestigeLevel);
    }

    private String getMoneyBonusPercentage(int prestigeLevel) {
        return plugin.getPrestigeManager().getMoneyBonusPercentage(prestigeLevel) + "%";
    }

    private String calculateXPProgress(PlayerData playerData) {
        int currentXP = playerData.getTrashPickXP();
        int requiredXP = plugin.getTrashPickManager().calculateRequiredXP(playerData.getTrashPickLevel());

        if (requiredXP <= 0) return "100";

        double percentage = ((double) currentXP / requiredXP) * 100.0;
        return String.format("%.1f", Math.min(100.0, percentage));
    }
}