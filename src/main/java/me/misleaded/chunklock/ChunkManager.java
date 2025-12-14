package me.misleaded.chunklock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ChunkManager {
    private static int[][] directions = { {1, 0, 0}, {0, 1, 1}, {-1, 0, 2}, {0, -1, 3} };

    // https://minecraft.fandom.com/wiki/Content_inaccessible_in_Survival
    // Not including SPAWN_EGG
    // Manually added FROGSPAWN, TEST_BLOCK, TEST_INSTANCE_BLOCK, VAULT
    private static Set<Material> unobtainables = Set.of(Material.AIR, Material.BARRIER, Material.STRUCTURE_VOID, Material.LIGHT, Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK, Material.STRUCTURE_BLOCK, Material.JIGSAW, Material.PETRIFIED_OAK_SLAB, Material.PLAYER_HEAD, Material.KNOWLEDGE_BOOK, Material.DEBUG_STICK, Material.BEDROCK, Material.REINFORCED_DEEPSLATE, Material.BUDDING_AMETHYST, Material.CHORUS_PLANT, Material.DIRT_PATH, Material.END_PORTAL_FRAME, Material.FARMLAND, Material.INFESTED_CHISELED_STONE_BRICKS, Material.INFESTED_CRACKED_STONE_BRICKS, Material.INFESTED_COBBLESTONE, Material.INFESTED_DEEPSLATE, Material.INFESTED_MOSSY_STONE_BRICKS, Material.INFESTED_STONE, Material.INFESTED_STONE_BRICKS, Material.SPAWNER, Material.COMMAND_BLOCK_MINECART, Material.FROGSPAWN, Material.TEST_BLOCK, Material.TEST_INSTANCE_BLOCK, Material.VAULT);
    public static ArrayList<String> unlockables = new ArrayList<>();
    static {
        for (Material m : Material.values()) {
            if (m.isItem() && !unobtainables.contains(m)) {
                String name = m.name();
                if (!name.contains("SPAWN_EGG")) {
                    unlockables.add(m.name());
                }
            }
        }
    }

    public static boolean active = false;
    private static Material material = Material.RED_STAINED_GLASS;
    private static Material netherMaterial = Material.WHITE_STAINED_GLASS;
    private static Material endMaterial = Material.PURPLE_STAINED_GLASS;
    private static HashSet<List<Integer>> unlocked = new HashSet<List<Integer>>();
    private static HashSet<List<Integer>> capped = new HashSet<List<Integer>>();
    private static HashMap<List<Integer>, String> unlockItems = new HashMap<List<Integer>, String>();
    private static HashMap<UUID, Location> playerLocations = new HashMap<UUID, Location>();
    private static int rerollScale = 0;
    private static HashSet<Inventory> openGuis = new HashSet<Inventory>();

    public static void capChunk(Chunk c, boolean uncap) {
        if (!uncap)
            if (!active || capped.contains(chunkPos(c)) || unlocked.contains(chunkPos(c))) return;

        
        Material m = getMaterial(c.getWorld());
        
        int maxHeight = c.getWorld().getMaxHeight()-1;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Block b = c.getBlock(x, maxHeight, z);

                if (uncap && b.getType().equals(m)) {
                    b.setType(Material.AIR, false);
                } else if (!uncap && b.getType().equals(Material.AIR)) {
                    b.setType(m, false);
                }
                
            }
        }

        capped.add(chunkPos(c));
        unlockItems.put(chunkPos(c), randomUnlockable());
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

            if (unlocked.contains(Arrays.asList(x, z, pos.get(2)))) {
                wallChunk(c, (d[2]+2)%4, maxHeight, true);
            } else {
                wallChunk(neighbor, d[2], maxHeight, false);
            }
        }
        
        capChunk(c, true);
    }

    public static void wallChunk(Chunk c, int d, int maxHeight, boolean unwall) {
        int zChunk = d % 2;
        int i = d < 2 ? 0 : 15;

        Material m = getMaterial(c.getWorld());

        for (int j = 0; j < 16; j++) {
            for (int y = 0; y < maxHeight; y++) {
                Block b = zChunk == 0 ? c.getBlock(i, y, j) : c.getBlock(j, y, i);
                
                if (unwall && b.getType().equals(m)) {
                    b.setType(Material.AIR, false);
                } else if (!unwall && b.getType().equals(Material.AIR)) {
                    b.setType(m, false);
                }
            }
        }
    }

    public static List<Integer> chunkPos(Chunk c) {
        return Arrays.asList(c.getX(), c.getZ(), Bukkit.getWorlds().indexOf(c.getWorld()));
    }

    public static Material getMaterial(World w) {
        switch (Bukkit.getWorlds().indexOf(w)) {
            case 0:
                return material;
            case 1:
                return netherMaterial;
            case 2:
                return endMaterial;
            default:
                return material;
        }
    }

    public static void saveData() {
        FileConfiguration configFile = Chunklock.plugin.getConfig();
        configFile.set("initialized", true);
        configFile.set("active", active);
        configFile.set("material", material.toString());
        configFile.set("netherMaterial", netherMaterial.toString());
        configFile.set("endMaterial", endMaterial.toString());
        configFile.set("rerollScale",rerollScale);

        ConfigurationSection unlockedConfig = configFile.createSection("unlocked");
        int i = 0;
        for (List<Integer> c : unlocked) {
            unlockedConfig.set(String.valueOf(i), c);
            i++;
        }

        ConfigurationSection cappedConfig = configFile.createSection("capped");
        i = 0;
        for (List<Integer> c : capped) {
            cappedConfig.set(String.valueOf(i), c);
            i++;
        }

        ConfigurationSection unlockItemsConfig = configFile.createSection("unlockItems");
        for (List<Integer> c : unlockItems.keySet()) {
            unlockItemsConfig.set(c.toString(), unlockItems.get(c));
        }

        Chunklock.plugin.saveConfig();
    }

    public static void loadData() {
        FileConfiguration configFile = Chunklock.plugin.getConfig();
        if (!configFile.contains("initialized")) return;

        active = configFile.getBoolean("active");
        material = Material.getMaterial(configFile.getString("material"));
        netherMaterial = Material.getMaterial(configFile.getString("netherMaterial"));
        endMaterial = Material.getMaterial(configFile.getString("endMaterial"));
        rerollScale = configFile.getInt("rerollScale");

        ConfigurationSection unlockedConfig = configFile.getConfigurationSection("unlocked");
        for (String k : unlockedConfig.getKeys(false)) {
            unlocked.add(unlockedConfig.getIntegerList(k));
        }

        ConfigurationSection cappedConfig = configFile.getConfigurationSection("capped");
        for (String k : cappedConfig.getKeys(false)) {
            capped.add(cappedConfig.getIntegerList(k));
        }

        ConfigurationSection unlockItemsConfig = configFile.getConfigurationSection("unlockItems");
        for (String k : unlockItemsConfig.getKeys(true)) {
            Integer c[] = new Integer[3];
            int i = 0;
            for (String val : k.substring(1, k.length()-1).split(", ")) {
                c[i] = Integer.parseInt(val);
                i++;
            }
            unlockItems.put(Arrays.asList(c), unlockItemsConfig.getString(k));
        }
    }

    public static boolean isUnlocked(Chunk c) {
        return unlocked.contains(chunkPos(c));
    }

    public static boolean isBorder(Chunk c) {
        List<Integer> pos = chunkPos(c);

        if (unlocked.contains(pos)) return false;

        for (int[] d : directions) {
            int x = pos.get(0) + d[0];
            int z = pos.get(1) + d[1];
            
            if (unlocked.contains(Arrays.asList(x, z, pos.get(2)))) return true;
        }

        return false;
    }

    public static int distance(int bX, int bZ, int cX, int cZ) {
        // Chunk coords to block coords
        cX *= 16;
        cZ *= 16;

        // Get closest block in chunk
        if (bX > cX) cX += Math.min(bX - cX, 15);
        if (bZ > cZ) cZ += Math.min(bZ - cZ, 15);

        int dX = cX - bX;
        int dZ = cZ - bZ;

        return dX * dX + dZ * dZ;

    }

    public static Location nearestUnlockedLocation(Location loc) {
        int world = Bukkit.getWorlds().indexOf(loc.getWorld());

        List<Integer> nearestChunk = Arrays.asList(0, 0, 0);
        int nearestChunkDist = Integer.MAX_VALUE;
        for (List<Integer> c : unlocked) {
            if (c.get(2) != world) continue;

            if (distance(loc.getBlockX(), loc.getBlockZ(), c.get(0), c.get(1)) < nearestChunkDist) {
                nearestChunk = c;
            }
        }

        double x = nearestChunk.get(0) * 16;
        double z = nearestChunk.get(1) * 16;

        if (loc.getX() > x) x += Math.min(loc.getX() - x, 15.999);
        if (loc.getZ() > z) z += Math.min(loc.getZ() - z, 15.999);

        return new Location(loc.getWorld(), x, loc.getY(), z);
    }

    private static ItemStack createItem(Material m, int amount, String name, List<String> lore) {
        ItemStack item = new ItemStack(m, amount);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public static Inventory createGui(Chunk chunk) {
        Inventory inv = Bukkit.createInventory(null, 9, "Unlock Chunk " + chunk.getX() + " " + chunk.getZ());

        Material unlockMaterial = Material.getMaterial(unlockItems.get(chunkPos(chunk)).toUpperCase());
        List<String> unlockLore = Arrays.asList("§r§7Pay §6§lx1 " + unlockMaterial.toString() + " §r§7to unlock.");
        ItemStack unlockItem = createItem(unlockMaterial, 1, "§r§lUnlock", unlockLore);

        int rerollPrice = (int) Math.min(64, 4*Math.pow(1.5,rerollScale)); // 4(1.5)^x
        List<String> rerollLore = Arrays.asList("§r§7Pay §b§lx" + rerollPrice + " DIAMOND §r§7to reroll.");
        ItemStack rerollItem = createItem(Material.DIAMOND, rerollPrice, "§r§lReroll", rerollLore);

        inv.setItem(3, unlockItem);
        inv.setItem(5, rerollItem);
        
        openGuis.add(inv);
        return inv;
    }

    public static void deleteGui(Inventory inv) {
        openGuis.remove(inv);
    }

    public static boolean isGui(Inventory inv) {
        return openGuis.contains(inv);
    }

    public static void rerollChunk(Chunk c) {
        unlockItems.put(chunkPos(c), randomUnlockable());
        rerollScale++;
    }

    public static void updatePlayer(Player p) {
        playerLocations.put(p.getUniqueId(), p.getLocation());
    }

    public static Location getPlayer(Player p) {
        return playerLocations.get(p.getUniqueId());
    }

    public static void removePlayer(Player p) {
        playerLocations.remove(p.getUniqueId());
    }

    public static String randomUnlockable() {
        return unlockables.get((int) (Math.random()*unlockables.size()));
    }
}
