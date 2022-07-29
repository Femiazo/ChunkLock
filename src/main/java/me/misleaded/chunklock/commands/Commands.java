package me.misleaded.chunklock.commands;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.misleaded.chunklock.ChunkManager;

public class Commands implements CommandExecutor {

    // This method is called, when somebody uses our command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Only players can use that command!");
			return true;
		}
		
		Player p = (Player) sender;
    
        if (command.getName().equalsIgnoreCase("start")) {
            
            int x, z;

            if (args.length == 0) {
                x = 8;
                z = 8;
            } else if (args.length == 2) {
                x = Integer.parseInt(args[0]);
                z = Integer.parseInt(args[1]);
            } else {
                p.sendMessage("§4Arguments: X and Z coordinates (Optional)");
                return true;
            }

            World overworld = Bukkit.getWorlds().get(0);

            int y = overworld.getHighestBlockYAt(x, z)+1;
            Location spawnLoc = new Location(overworld, x, y, z);

            overworld.setSpawnLocation(spawnLoc);
            Bukkit.setSpawnRadius(6);

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.teleport(spawnLoc);
            }

            ChunkManager.active = true;

            ChunkManager.unlockChunk(overworld.getChunkAt(spawnLoc));
            ChunkManager.capLoadedChunks();
        }

        if (command.getName().equalsIgnoreCase("unlock")) {

            int x, z;
            int w = 0;
            if (args.length == 2) {
                x = Integer.parseInt(args[0]);
                z = Integer.parseInt(args[1]);
            } else if (args.length == 3) {
                x = Integer.parseInt(args[0]);
                z = Integer.parseInt(args[1]);
                w = Integer.parseInt(args[2]);
            } else {
                p.sendMessage("§4Arguments: X and Z coordinates, world index (Optional)");
                return true;
            }

            World world = Bukkit.getWorlds().get(w); 
            Chunk c = world.getChunkAt(new Location(world, x*16, 0, z*16));
            
            if (ChunkManager.isUnlocked(c)) {
                p.sendMessage("§4Chunk is already unlocked");
                return true;
            }

            ChunkManager.unlockChunk(c);
        }

        return true;
    }
}

