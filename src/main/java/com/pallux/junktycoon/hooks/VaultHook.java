package com.pallux.junktycoon.hooks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VaultHook {

    private final JavaPlugin plugin;
    private Economy economy = null;

    public VaultHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean hasEnoughMoney(Player player, double amount) {
        return economy.has(player, amount);
    }

    public boolean withdrawMoney(Player player, double amount) {
        if (hasEnoughMoney(player, amount)) {
            economy.withdrawPlayer(player, amount);
            return true;
        }
        return false;
    }

    public void depositMoney(Player player, double amount) {
        economy.depositPlayer(player, amount);
    }

    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    public String formatMoney(double amount) {
        return economy.format(amount);
    }
}