package optic_fusion1.worldschematics.schematicblock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import optic_fusion1.worldschematics.loot.LootTable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import worldschematics.util.DebugLogger;

/**
 * Represents a container, such as a chest, within a schematic.
 */
public class SchematicContainer extends AbstractSchematicBlock {

    private List<String> lootTables = new ArrayList<>();
    private ContainerType containerType;

    public SchematicContainer(Location location, ConfigurationSection configSection, String name, ContainerType containerType) {
        super(location, configSection, name);
        lootTables = configSection.getStringList("lootTables");
        this.containerType = containerType;
    }

    public void createInWorld(Location worldLocation) throws IOException {
        DebugLogger.log("container detected in config, will try to fill with loot. Location is a " + worldLocation.getBlock().getType().toString() + " at " + worldLocation.getBlockX() + " " + worldLocation.getBlockY() + " " + worldLocation.getBlockZ(), DebugLogger.DebugType.LOOTTABLE);

        createContainer(worldLocation);
    }

    private void createContainer(Location worldLocation) throws IOException {
        // First check if the block is a chest
        if (worldLocation.getBlock().getType() != Material.CHEST && worldLocation.getBlock().getType() != Material.TRAPPED_CHEST) {
            DebugLogger.log("Block is not a chest, this may happen when a world is first being generated", DebugLogger.DebugType.LOOT);
            return;
        }
        Random random = new Random(System.currentTimeMillis());
        Chest chest = (Chest) worldLocation.getBlock().getState();
        boolean overwriteContents = false;

        // Check options for the chest and see if we should overwrite the contents or not
        if (configSection().getBoolean("properties.deleteContents", false)) {
            overwriteContents = true;
        }

        // We need to make sure the loot table list is not empty first
        if (lootTables == null || lootTables.isEmpty()) {
            return;
        }
        String randomLootTable = lootTables.get(random.nextInt(lootTables.size()));
        LootTable lootTable = new LootTable(chest, randomLootTable, overwriteContents);
        lootTable.fillChest();
    }

    public enum ContainerType {
        CHEST
    }

}
