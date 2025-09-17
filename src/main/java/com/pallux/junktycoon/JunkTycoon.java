package com.pallux.junktycoon;

import com.pallux.junktycoon.commands.JunkTycoonCommand;
import com.pallux.junktycoon.commands.JunkReloadCommand;
import com.pallux.junktycoon.config.ConfigManager;
import com.pallux.junktycoon.data.PlayerDataManager;
import com.pallux.junktycoon.hooks.PlayerPointsHook;
import com.pallux.junktycoon.hooks.VaultHook;
import com.pallux.junktycoon.listeners.PlayerInteractListener;
import com.pallux.junktycoon.listeners.PlayerJoinListener;
import com.pallux.junktycoon.listeners.InventoryListener;
import com.pallux.junktycoon.managers.TrashPickManager;
import com.pallux.junktycoon.managers.TrashManager;
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