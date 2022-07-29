package me.misleaded.chunklock.events;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.PortalCreateEvent.CreateReason;
import org.spigotmc.event.entity.EntityMountEvent;

import me.misleaded.chunklock.ChunkManager;

public class Events implements Listener {

    @EventHandler
    public void onChunkLoaded(ChunkLoadEvent e) {
        if (!ChunkManager.active) return;

        ChunkManager.capChunk(e.getChunk(), false);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (ChunkManager.isUnlocked(e.getTo().getChunk())) return;

        if (ChunkManager.isUnlocked(e.getFrom().getChunk())) {
            if (e.getPlayer().isInsideVehicle()) e.getPlayer().getVehicle().eject();
            e.setCancelled(true);
            return;
        } else {
            System.out.println("Player moved starting from locked chunk?");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.hasBlock()) {
            if (!ChunkManager.isUnlocked(e.getClickedBlock().getChunk())) {
                e.getPlayer().sendMessage(e.getClickedBlock().getChunk().toString());
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        if (ChunkManager.isUnlocked(e.getTo().getChunk())) return;

        if (ChunkManager.isUnlocked(e.getFrom().getChunk())) {
            if (e.getCause().equals(TeleportCause.ENDER_PEARL) || e.getCause().equals(TeleportCause.CHORUS_FRUIT)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void OnPlayerPortal(PlayerPortalEvent e) {
        e.setCreationRadius(0);
    }

    @EventHandler
    public void OnPortalCreate(PortalCreateEvent e) {
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
        if (!ChunkManager.isUnlocked(e.getMount().getLocation().getChunk())) {
            e.setCancelled(true);
        }
    }

    // TEMP
    @EventHandler
    public void OnEntityExplode(EntityExplodeEvent e) {
        for (Block b : e.blockList()) {
            if (!ChunkManager.isUnlocked(b.getChunk())) {
                e.setCancelled(true);
            }
        }
    }

    // TEMP
    @EventHandler
    public void OnBlockPistonExtend(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) {
            if (!ChunkManager.isUnlocked(b.getChunk())) {
                e.setCancelled(true);
            }
        }
    }

    // TEMP
    @EventHandler
    public void OnBlockPistonRetract(BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) {
            if (!ChunkManager.isUnlocked(b.getChunk())) {
                e.setCancelled(true);
            }
        }
    }

}