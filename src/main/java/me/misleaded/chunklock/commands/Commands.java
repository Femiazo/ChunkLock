package me.misleaded.chunklock.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import me.misleaded.chunklock.ChunkManager;

public class Commands implements TabExecutor {

    // This method is called, when somebody uses our command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Only players can use that command!");
			return true;
		}

        if (!sender.isOp()) return true;
		
		Player p = (Player) sender;
    
        if (command.getName().equalsIgnoreCase("start")) {
            
            int x = 8;
            int z = 8;

            if (args.length == 1 || args.length > 2) {
                p.sendMessage("§4Arguments: X and Z coordinates (Optional)");
                return true;
            }

            if (args.length == 2) {
                int[] startLoc = parseBlock(p, args[0], args[1]);
                if (startLoc == null) {
                    p.sendMessage("§4Arguments: X and Z coordinates (Optional)");
                    return true;
                }
                x = startLoc[0];
                z = startLoc[1];
            }

            if (ChunkManager.active == true) {
                p.sendMessage("§4Chunklock has already been started.");
                return true;
            }

            World overworld = Bukkit.getWorlds().get(0);
            World nether = Bukkit.getWorlds().get(1);
            World end = Bukkit.getWorlds().get(2);

            int y = overworld.getHighestBlockYAt(x, z)+1;
            Location spawnLoc = new Location(overworld, x, y, z);
            Location netherLoc = new Location(nether, x/8, 0, z/8);

            overworld.setSpawnLocation(spawnLoc);
            Bukkit.setSpawnRadius(6);

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.teleport(spawnLoc);
            }

            ChunkManager.active = true;

            ChunkManager.unlockChunk(overworld.getChunkAt(spawnLoc));
            ChunkManager.unlockChunk(nether.getChunkAt(netherLoc));
            ChunkManager.unlockChunk(end.getChunkAt(6, 0));
            ChunkManager.capLoadedChunks();

            p.sendMessage("§aGame Start!");
        }

        if (command.getName().equalsIgnoreCase("unlock")) {

            if (args.length < 2 || args.length > 3) {
                p.sendMessage("§4Arguments: X and Z coordinates, world (Optional)");
                return true;
            }

            World w = (args.length == 3) ? Bukkit.getWorld(args[2]) : p.getWorld();
            Chunk c = parseChunk(p, args[0], args[1], w);
            if (c == null) {
                p.sendMessage("§4Arguments: X and Z coordinates, world (Optional)");
                return true;
            }
            
            if (ChunkManager.isUnlocked(c)) {
                p.sendMessage("§4Chunk is already unlocked");
                return true;
            }

            ChunkManager.unlockChunk(c);
            p.sendMessage("§aChunk at §6(" + c.getX() + ", " + c.getZ() + ")§a has been unlocked!");
        }

        return true;
    }

    private int[] parseBlock(Player p, String argX, String argZ) {
        Location l = p.getLocation();
        int x = l.getBlockX();
        int z = l.getBlockZ();

        if (!argX.equals("~")) {
            try {
                x = Integer.parseInt(argX);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (!argZ.equals("~")) {
            try {
                z = Integer.parseInt(argZ);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return new int[] { x, z };
    }

    private Chunk parseChunk(Player p, String argX, String argZ, World world) {
        if (world == null) return null;

        Chunk c = p.getLocation().getChunk();
        int x = c.getX();
        int z = c.getZ();

        if (!argX.equals("~")) {
            try {
                x = Integer.parseInt(argX);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (!argZ.equals("~")) {
            try {
                z = Integer.parseInt(argZ);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return world.getChunkAt(x, z);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Only players can use that command!");
			return new ArrayList<>();
		}

        if (!sender.isOp()) return new ArrayList<>();
		
		Player p = (Player) sender;
    
        if (command.getName().equalsIgnoreCase("start")) {
            return chunkCompletion(p, args, false);
        } 

        if (command.getName().equalsIgnoreCase("unlock")) {
            return chunkCompletion(p, args, true);
        }

        return new ArrayList<>();
    }

    private List<String> chunkCompletion(Player p, String[] args, boolean world) {
        Block target = p.getTargetBlockExact(4);
        String x, z;
        if (target != null) {
            Chunk c = target.getChunk();
            x = "" + c.getX();
            z = "" + c.getZ();
        } else {
            x = "~";
            z = "~";
        }

        if (args.length == 1 && args[0].isEmpty()) {
            return List.of(x, x + " " + z);
        } else if (args.length == 1 && !args[0].isEmpty()) {
            return List.of(args[0] + " " + z);
        } else if (args.length == 2 && args[1].isEmpty()) {
            return List.of(z);
        } else if (args.length == 2 && !args[1].isEmpty()) {
            return new ArrayList<>();
        }

        if (world && args.length == 3) {
            return Bukkit.getWorlds().stream()
                         .map(w -> w.getName())
                         .filter(s -> s.startsWith(args[2]))
                         .toList();
        }

        return new ArrayList<>();
    }
}

