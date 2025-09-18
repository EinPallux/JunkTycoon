package com.pallux.junktycoon;

import com.pallux.junktycoon.commands.JunkTycoonCommand;
import com.pallux.junktycoon.commands.JunkReloadCommand;
import com.pallux.junktycoon.commands.BoostersCommand;
import com.pallux.junktycoon.commands.JunkResetCommand;
import com.pallux.junktycoon.commands.PrestigeCommand;
import com.pallux.junktycoon.config.ConfigManager;
import com.pallux.junktycoon.data.PlayerData;
import com.pallux.junktycoon.data.PlayerDataManager;
import com.pallux.junktycoon.hooks.PlayerPointsHook;
import com.pallux.junktycoon.hooks.VaultHook;
import com.pallux.junktycoon.hooks.PlaceholderAPIHook;
import com.pallux.junktycoon.listeners.PlayerInteractListener;
import com.pallux.junktycoon.listeners.PlayerJoinListener;
import com.pallux.junktycoon.listeners.InventoryListener;
import com.pallux.junktycoon.managers.TrashPickManager;
import com.pallux.junktycoon.managers.TrashManager;
import com.pallux.junktycoon.managers.BoosterManager;
import com.pallux.junktycoon.managers.PrestigeManager;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class JunkTycoon extends JavaPlugin {

    private static JunkTycoon instance;

    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private TrashPickManager trashPickManager;
    private TrashManager trashManager;
    private BoosterManager boosterManager;
    private PrestigeManager prestigeManager;
    private VaultHook vaultHook;
    private PlayerPointsHook playerPointsHook;
    private PlaceholderAPIHook placeholderAPIHook;

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
        boosterManager = new BoosterManager(this);
        prestigeManager = new PrestigeManager(this);

        // Register listeners
        registerListeners();

        // Register commands
        registerCommands();

        // Start cooldown update task
        startCooldownUpdateTask();

        String enableMessage = configManager.getMessage("general.plugin_enabled");
        getLogger().info(ChatColor.stripColor(enableMessage));
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }
        if (boosterManager != null) {
            boosterManager.shutdown();
        }
        if (placeholderAPIHook != null) {
            placeholderAPIHook.shutdown();
        }
        String disableMessage = configManager.getMessage("general.plugin_disabled");
        getLogger().info(ChatColor.stripColor(disableMessage));
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

        // Setup PlaceholderAPI hook (optional)
        placeholderAPIHook = new PlaceholderAPIHook(this);
        placeholderAPIHook.setupPlaceholderAPI();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
    }

    private void registerCommands() {
        getCommand("junktycoon").setExecutor(new JunkTycoonCommand(this));
        getCommand("junkreload").setExecutor(new JunkReloadCommand(this));
        getCommand("boosters").setExecutor(new BoostersCommand(this));
        getCommand("junkreset").setExecutor(new JunkResetCommand(this));
        getCommand("prestige").setExecutor(new PrestigeCommand(this));
    }

    private void startCooldownUpdateTask() {
        // Update cooldown indicators every 5 ticks (0.25 seconds)
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                updateCooldownIndicator(player);
            }
        }, 0L, 5L);
    }

    public void updateCooldownIndicator(Player player) {
        ItemStack trashPick = player.getInventory().getItem(0);

        if (trashPick == null || !trashPickManager.isTrashPick(trashPick)) {
            return;
        }

        PlayerData playerData = playerDataManager.getPlayerData(player);

        // Don't update if player has bypass permission
        if (player.hasPermission("junktycoon.admin.nocooldown")) {
            removeCooldownIndicator(trashPick);
            return;
        }

        long currentTime = System.currentTimeMillis();
        long lastPickTime = playerData.getLastPickTime();

        // If never picked before, no cooldown
        if (lastPickTime == 0) {
            removeCooldownIndicator(trashPick);
            return;
        }

        double cooldownSeconds = calculateCooldownForPlayer(playerData);
        long cooldownMs = (long) (cooldownSeconds * 1000);
        long timePassed = currentTime - lastPickTime;
        long remainingMs = Math.max(0, cooldownMs - timePassed);

        if (remainingMs <= 0) {
            // Cooldown finished, remove indicator
            removeCooldownIndicator(trashPick);
        } else {
            // Cooldown active, add indicator
            applyCooldownIndicator(trashPick, remainingMs, cooldownMs);
        }
    }

    private void removeCooldownIndicator(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Remove enchantment glint
            meta.removeEnchant(Enchantment.UNBREAKING);

            // Reset durability if it was changed
            if (meta instanceof Damageable damageable && (damageable.getDamage() > 0 || meta.isUnbreakable())) {
                damageable.setDamage(0);
                meta.setUnbreakable(false);
            }

            item.setItemMeta(meta);
        }
    }

    private void applyCooldownIndicator(ItemStack item, long remainingMs, long totalCooldownMs) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Add enchantment glint to indicate cooldown
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);

            item.setItemMeta(meta);
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
        boosterManager.reload();
        prestigeManager.reload();
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

    public BoosterManager getBoosterManager() {
        return boosterManager;
    }

    public PrestigeManager getPrestigeManager() {
        return prestigeManager;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public PlayerPointsHook getPlayerPointsHook() {
        return playerPointsHook;
    }

    public PlaceholderAPIHook getPlaceholderAPIHook() {
        return placeholderAPIHook;
    }
}