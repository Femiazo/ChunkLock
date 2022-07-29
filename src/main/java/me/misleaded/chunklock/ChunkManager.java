package me.misleaded.chunklock;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class ChunkManager {
    private static int[][] directions = { {1, 0, 0}, {0, 1, 1}, {-1, 0, 2}, {0, -1, 3} };

    public static boolean active = false;
    private static Material material = Material.RED_STAINED_GLASS;
    private static HashSet<List<Integer>> unlocked = new HashSet<List<Integer>>();
    private static HashSet<List<Integer>> capped = new HashSet<List<Integer>>();

    public static void capChunk(Chunk c, boolean uncap) {
        if (!uncap)
            if (!active || capped.contains(chunkPos(c)) || unlocked.contains(chunkPos(c))) return;

        int maxHeight = c.getWorld().getMaxHeight()-1;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Block b = c.getBlock(x, maxHeight, z);

                if (uncap && b.getType().equals(material)) {
                    b.setType(Material.AIR, false);
                } else if (!uncap && b.getType().equals(Material.AIR)) {
                    b.setType(material, false);
                }
                
            }
        }

        capped.add(chunkPos(c));
    }

    public static void capLoadedChunks() {
        for (World w : Bukkit.getWorlds()) {
            for (Chunk c : w.getLoadedChunks()) {
                capChunk(c, false);
            }
        }
    }

    public static void unlockChunk(Chunk c) {
        int maxHeight = c.getWorld().getMaxHeight();
        List<Integer> pos = chunkPos(c);

        unlocked.add(pos);

        for (int[] d : directions) {
            int x = pos.get(0) + d[0];
            int z = pos.get(1) + d[1];

            Chunk neighbor = c.getWorld().getChunkAt(x, z);

            if (unlocked.contains(Arrays.asList(x, z, Bukkit.getWorlds().indexOf(neighbor.getWorld())))) {
                // wallChunk(neighbor, d[2], maxHeight, true);
                wallChunk(c, (d[2]+2)%4, maxHeight, true);
            }
            else {
                wallChunk(neighbor, d[2], maxHeight, false);
            }
        }
        
        capChunk(c, true);
    }

    public static void wallChunk(Chunk c, int d, int maxHeight, boolean unwall) {
        int zChunk = d % 2;
        int i = d < 2 ? 0 : 15;

        for (int j = 0; j < 16; j++) {
            for (int y = 0; y < maxHeight; y++) {
                Block b = zChunk == 0 ? c.getBlock(i, y, j) : c.getBlock(j, y, i);
                
                if (unwall && b.getType().equals(material)) {
                    b.setType(Material.AIR, false);
                } else if (!unwall && b.getType().equals(Material.AIR)) {
                    b.setType(material, false);
                }
            }
        }
    }

    public static List<Integer> chunkPos(Chunk c) {
        return Arrays.asList(c.getX(), c.getZ(), Bukkit.getWorlds().indexOf(c.getWorld()));
    }

    public static boolean changeMaterial(Material m) {
        if (active) return false;

        material = m;
        return true;
    }

    public static void saveData() {
        FileConfiguration configFile = Chunklock.plugin.getConfig();
        configFile.set("initialized", true);
        configFile.set("active", active);
        configFile.set("material", material.toString());

        ConfigurationSection unlockedConfig = configFile.createSection("unlocked");
        int i = 0;
        for (List<Integer> c : unlocked) {
            unlockedConfig.set(String.valueOf(i), c);
            i++;
        }

        System.out.println(capped);
        ConfigurationSection cappedConfig = configFile.createSection("capped");
        i = 0;
        for (List<Integer> c : capped) {
            cappedConfig.set(String.valueOf(i), c);
            i++;
        }

        Chunklock.plugin.saveConfig();
    }

    public static void loadData() {
        FileConfiguration configFile = Chunklock.plugin.getConfig();
        if (!configFile.contains("initialized")) return;

        active = configFile.getBoolean("active");
        material = Material.getMaterial(configFile.getString("material"));

        ConfigurationSection unlockedConfig = configFile.getConfigurationSection("unlocked");
        for (String k : unlockedConfig.getKeys(false)) {
            unlocked.add(unlockedConfig.getIntegerList(k));
        }

        ConfigurationSection cappedConfig = configFile.getConfigurationSection("capped");
        for (String k : cappedConfig.getKeys(false)) {
            capped.add(cappedConfig.getIntegerList(k));
        }
    }

    public static boolean isUnlocked(Chunk c) {
        return unlocked.contains(chunkPos(c));
    }
}
