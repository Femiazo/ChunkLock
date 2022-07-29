package me.misleaded.chunklock;

import org.bukkit.plugin.java.JavaPlugin;

import me.misleaded.chunklock.commands.Commands;
import me.misleaded.chunklock.events.Events;

public final class Chunklock extends JavaPlugin {
    public static JavaPlugin plugin;


    @Override
    public void onEnable() {
        plugin = this;

        getServer().getPluginManager().registerEvents(new Events(), this);
        this.getCommand("start").setExecutor(new Commands());
        this.getCommand("unlock").setExecutor(new Commands());

        this.saveDefaultConfig();

        ChunkManager.loadData();
    }

    @Override
    public void onDisable() {
        ChunkManager.saveData();
    }
}
