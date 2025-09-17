package com.pallux.junktycoon;

import com.pallux.junktycoon.commands.JunkTycoonCommand;
import com.pallux.junktycoon.commands.JunkReloadCommand;
import com.pallux.junktycoon.config.ConfigManager;
import com.pallux.junktycoon.data.PlayerData;
import com.pallux.junktycoon.data.PlayerDataManager;
import com.pallux.junktycoon.hooks.PlayerPointsHook;
import com.pallux.junktycoon.hooks.VaultHook;
import com.pallux.junktycoon.listeners.PlayerInteractListener;
import com.pallux.junktycoon.listeners.PlayerJoinListener;
import com.pallux.junktycoon.listeners.InventoryListener;
import com.pallux.junktycoon.managers.TrashPickManager;
import com.pallux.junktycoon.managers.TrashManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class JunkTycoon extends JavaPlugin {

    private static JunkTycoon instance;

    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private TrashPickManager trashPickManager;
    private TrashManager trashManager;
    private VaultHook vaultHook;
    private PlayerPointsHook playerPointsHook;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize config manager
        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        // Initialize hooks
        setupHooks();

        // Initialize managers
        playerDataManager = new PlayerDataManager(this);
        trashPickManager = new TrashPickManager(this);
        trashManager = new TrashManager(this);

        // Register listeners
        registerListeners();

        // Register commands
        registerCommands();

        // Start cooldown update task
        startCooldownUpdateTask();

        getLogger().info("JunkTycoon has been enabled!");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }
        getLogger().info("JunkTycoon has been disabled!");
    }

    private void setupHooks() {
        // Setup Vault hook
        vaultHook = new VaultHook(this);
        if (!vaultHook.setupEconomy()) {
            getLogger().severe("Vault dependency not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Setup PlayerPoints hook (optional)
        playerPointsHook = new PlayerPointsHook(this);
        playerPointsHook.setupPlayerPoints();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
    }

    private void registerCommands() {
        getCommand("junktycoon").setExecutor(new JunkTycoonCommand(this));
        getCommand("junkreload").setExecutor(new JunkReloadCommand(this));
    }

    private void startCooldownUpdateTask() {
        // Update cooldown indicators every 2 ticks (0.1 seconds) for smooth animation
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                updateCooldownIndicator(player);
            }
        }, 0L, 2L);
    }

    public void updateCooldownIndicator(Player player) {
        ItemStack trashPick = player.getInventory().getItem(0);

        if (trashPick != null && trashPickManager.isTrashPick(trashPick)) {
            PlayerData playerData = playerDataManager.getPlayerData(player);

            // Don't update if player has bypass permission
            if (player.hasPermission("junktycoon.admin.nocooldown")) {
                // Remove durability damage if present
                ItemMeta meta = trashPick.getItemMeta();
                if (meta instanceof Damageable damageable && damageable.getDamage() > 0) {
                    damageable.setDamage(0);
                    meta.setUnbreakable(false);
                    trashPick.setItemMeta(meta);
                }
                return;
            }

            long currentTime = System.currentTimeMillis();
            long lastPickTime = playerData.getLastPickTime();

            // If never picked before, no cooldown
            if (lastPickTime == 0) {
                ItemMeta meta = trashPick.getItemMeta();
                if (meta instanceof Damageable damageable && damageable.getDamage() > 0) {
                    damageable.setDamage(0);
                    meta.setUnbreakable(false);
                    trashPick.setItemMeta(meta);
                }
                return;
            }

            double cooldownSeconds = calculateCooldownForPlayer(playerData);
            long cooldownMs = (long) (cooldownSeconds * 1000);
            long timePassed = currentTime - lastPickTime;
            long remainingMs = Math.max(0, cooldownMs - timePassed);

            ItemMeta meta = trashPick.getItemMeta();

            if (remainingMs <= 0) {
                // Cooldown finished, remove durability damage
                if (meta instanceof Damageable damageable && damageable.getDamage() > 0) {
                    damageable.setDamage(0);
                    meta.setUnbreakable(false);
                    trashPick.setItemMeta(meta);
                }
            } else {
                // Cooldown active, apply durability
                if (meta instanceof Damageable damageable) {
                    double cooldownPercentage = 1.0 - ((double) remainingMs / (double) cooldownMs);

                    short maxDurability = trashPick.getType().getMaxDurability();
                    if (maxDurability > 0) {
                        short durabilityDamage = (short) (maxDurability * (1.0 - cooldownPercentage));
                        durabilityDamage = (short) Math.min(durabilityDamage, maxDurability - 1);

                        damageable.setDamage(durabilityDamage);
                        meta.setUnbreakable(true);
                        trashPick.setItemMeta(meta);
                    }
                }
            }
        }
    }

    private double calculateCooldownForPlayer(PlayerData playerData) {
        double baseCooldown = configManager.getMainConfig().getDouble("settings.default_picking_cooldown", 2.0);

        int cooldownPerkLevel = playerData.getCooldownPerkLevel();
        if (cooldownPerkLevel > 0) {
            double reductionPerLevel = configManager.getMainConfig().getDouble("perks.picking_cooldown.cooldown_reduction", 0.1);
            double reduction = cooldownPerkLevel * reductionPerLevel;
            baseCooldown = Math.max(0.1, baseCooldown - reduction);
        }

        return baseCooldown;
    }

    public void reload() {
        configManager.loadConfigs();
        trashPickManager.reload();
        trashManager.reload();
    }

    // Getters
    public static JunkTycoon getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public TrashPickManager getTrashPickManager() {
        return trashPickManager;
    }

    public TrashManager getTrashManager() {
        return trashManager;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public PlayerPointsHook getPlayerPointsHook() {
        return playerPointsHook;
    }
}