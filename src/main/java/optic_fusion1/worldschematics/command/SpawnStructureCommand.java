package optic_fusion1.worldschematics.command;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.DataException;
import java.io.IOException;
import java.text.ParseException;
import optic_fusion1.worldschematics.SchematicManager;
import optic_fusion1.worldschematics.SpawnSchematic;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnStructureCommand implements CommandExecutor {

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
      @NotNull String label, @NotNull String[] args) {

    if (args.length == 0) {
      this.printHelp(sender, args);
      return true;
    }

    if (!(sender instanceof Player)) {
      sender.sendMessage(ChatColor.RED + "You must be a player to use this command");
      return true;
    }

    String structureName = args[0];
    if (!SchematicManager.getSchematics().containsKey(structureName)) {
      sender.sendMessage(ChatColor.RED + "No structure with the name " + structureName + " exists");
      return true;
    }

    SpawnSchematic schematic = SchematicManager.getSchematics().get(structureName);

    sender.sendMessage(
        ChatColor.GREEN + "Spawning structure " + structureName + " at your location");
    try {
      schematic.spawn(((Player) sender).getLocation());
    } catch (WorldEditException | IOException | DataException | ParseException e) {
      sender.sendMessage(ChatColor.RED + "Failed to spawn structure " + structureName);
      throw new RuntimeException(e);
    }

    return true;
  }

  private void printHelp(CommandSender sender, String[] args) {
    sender.sendMessage(ChatColor.GRAY + "Usage: /spawnstructure <name>");
  }
}
