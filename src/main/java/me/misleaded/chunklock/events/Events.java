package me.misleaded.chunklock.events;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.PortalCreateEvent.CreateReason;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import me.misleaded.chunklock.ChunkManager;
import me.misleaded.chunklock.Chunklock;

public class Events implements Listener {
    private final Chunklock plugin = Chunklock.getPlugin(Chunklock.class);
    private BukkitScheduler scheduler = Bukkit.getScheduler();


    @EventHandler
    public void onChunkLoaded(ChunkLoadEvent e) {
        if (!ChunkManager.active) return;

        ChunkManager.capChunk(e.getChunk(), false);
    }

    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent e) {
        ChunkManager.updatePlayer(e.getPlayer());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        ChunkManager.removePlayer(e.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent e) {
        ChunkManager.removePlayer(e.getPlayer());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (!ChunkManager.active) return;

        if (ChunkManager.isUnlocked(e.getTo().getChunk())) {
            ChunkManager.updatePlayer(e.getPlayer());
            return;
        }

        if (ChunkManager.isUnlocked(e.getFrom().getChunk())) {
            if (e.getPlayer().isInsideVehicle()) e.getPlayer().getVehicle().eject();
            e.setCancelled(true);
            return;
        } else {
            // System.out.println("Player moved starting from locked chunk?");
            // e.getPlayer().teleport(ChunkManager.nearestUnlockedLocation(e.getPlayer().getLocation()));
            e.getPlayer().teleport(ChunkManager.getPlayer(e.getPlayer()));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (!ChunkManager.active) return;

        if (e.hasBlock()) {
            if (ChunkManager.isBorder(e.getClickedBlock().getChunk())) {
                if (e.getPlayer().isSneaking()) {
                    Inventory inv = ChunkManager.createGui(e.getClickedBlock().getChunk());
                    e.getPlayer().openInventory(inv);
                }
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void OnInventoryClick(InventoryClickEvent e) {
        if (!ChunkManager.active) return;

        if (ChunkManager.isGui(e.getInventory())) {
            e.setCancelled(true);
            if (!ChunkManager.isGui(e.getClickedInventory()) || e.getCurrentItem() == null) return;

            HumanEntity p = e.getWhoClicked();
            Inventory gui = e.getClickedInventory();
            ItemStack unlockItem = gui.getItem(3);
            ItemStack rerollItem = gui.getItem(5);
            ItemStack clicked = e.getCurrentItem();

            if (!(clicked.equals(unlockItem) || clicked.equals(rerollItem))) return; 

            Set<ItemStack> stacks = new HashSet<>(p.getInventory().all(clicked.getType()).values());

            if (stacks.isEmpty()) {
                scheduler.runTaskLater(plugin, () -> {
                    p.closeInventory();
                }, 1L);
                p.sendMessage("§cYou do not have: " + clicked.getType().toString());
                return;
            }

            int total = stacks.stream().mapToInt(entry -> entry.getAmount())
                              .reduce(0, (a, b) -> a + b);

            if (total < clicked.getAmount()) {
                scheduler.runTaskLater(plugin, () -> {
                    p.closeInventory();
                }, 1L);
                p.sendMessage("§cYou do not have enough: " + clicked.getType().toString());
                return;
            }

            int remaining = clicked.getAmount();
            for (ItemStack s : stacks) {
                int toRemove = Math.min(s.getAmount(), remaining);
                s.setAmount(s.getAmount() - toRemove);
                remaining -= toRemove;

                if (remaining == 0) break;
            }

            int x = Integer.parseInt(e.getView().getTitle().split(" ")[2]);
            int z = Integer.parseInt(e.getView().getTitle().split(" ")[3]);

            Chunk c = p.getWorld().getChunkAt(x, z);

            if (clicked.equals(unlockItem)) {
                ChunkManager.unlockChunk(c);
            } else if (clicked.equals(rerollItem)) {
                ChunkManager.rerollChunk(c);
            }

            scheduler.runTaskLater(plugin, () -> {
                p.closeInventory();
            }, 1L);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!ChunkManager.active) return;

        if (ChunkManager.isGui(e.getInventory())) {
            ChunkManager.deleteGui(e.getInventory());
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        if (!ChunkManager.active || ChunkManager.isUnlocked(e.getTo().getChunk())) return;

        if (ChunkManager.isUnlocked(e.getFrom().getChunk())) {
            if (e.getCause().equals(TeleportCause.ENDER_PEARL) || e.getCause().equals(TeleportCause.CHORUS_FRUIT)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void OnPlayerPortal(PlayerPortalEvent e) {
        if (!ChunkManager.active) return;

        e.setCreationRadius(0);
    }

    @EventHandler
    public void OnPortalCreate(PortalCreateEvent e) {
        if (!ChunkManager.active) return;

        if (e.getReason().equals(CreateReason.NETHER_PAIR)) {
            for (BlockState b : e.getBlocks()) {
                if (!ChunkManager.isUnlocked(b.getChunk())) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void OnEntityMount(EntityMountEvent e) {
        if (!ChunkManager.active) return;

        if (!ChunkManager.isUnlocked(e.getMount().getLocation().getChunk())) {
            e.setCancelled(true);
        }
    }

    // TEMP
    @EventHandler
    public void OnEntityExplode(EntityExplodeEvent e) {
        if (!ChunkManager.active) return;

        HashSet<Block> toRemove = new HashSet<Block>();

        for (Block b : e.blockList()) {
            if (!ChunkManager.isUnlocked(b.getChunk())) {
                toRemove.add(b);
            }
        }

        for (Block b : toRemove) {
            e.blockList().remove(b);
        }
    }

    // TEMP
    @EventHandler
    public void OnBlockPistonExtend(BlockPistonExtendEvent e) {
        if (!ChunkManager.active) return;

        for (Block b : e.getBlocks()) {
            if (!ChunkManager.isUnlocked(b.getChunk())) {
                e.setCancelled(true);
            }
        }
    }

    // TEMP
    @EventHandler
    public void OnBlockPistonRetract(BlockPistonRetractEvent e) {
        if (!ChunkManager.active) return;

        for (Block b : e.getBlocks()) {
            if (!ChunkManager.isUnlocked(b.getChunk())) {
                e.setCancelled(true);
            }
        }
    }

    // @EventHandler
    // public void OnEntitySpawn(EntitySpawnEvent e) {
    //     if (!ChunkManager.active) return;

    //     if (!ChunkManager.isUnlocked(e.getLocation().getChunk())) {
    //         e.setCancelled(true);
    //     }
    // }

}