package optic_fusion1.worldschematics.schematicblock;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.DataException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import javax.json.JsonException;
import optic_fusion1.worldschematics.SchematicManager;
import optic_fusion1.worldschematics.schematicblock.SchematicSpawner.SpawnerType;
import optic_fusion1.worldschematics.util.MapUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import worldschematics.util.DebugLogger;

/**
 * Represents a Sign with [Marker] in its first line located inside a schematic, which will uses signs to determine the location of spawners and other features.
 */
public class SchematicMarker extends AbstractSchematicBlock {

    private HashMap<String, Double> schematicsList = new HashMap<>();
    private List<String> blocksList = new ArrayList<>();
    private SubType subType;
    private int markerChance = 0;
    private Random random = new Random();

    public SchematicMarker(Location schematicLocation, ConfigurationSection configSection, String name) {
        super(schematicLocation, configSection, name);

        try {
            subType = SubType.valueOf(configSection.getString("subtype", "NONE").toUpperCase());
        } catch (Exception e) {
            DebugLogger.log("Invalid Marker subtype: " + configSection.getString("subtype"));
            subType = SubType.NONE;
        }

        DebugLogger.log("Marker subtype is: " + subType, DebugLogger.DebugType.MARKER);

        // Chance of marker being replaced with something
        markerChance = configSection.getInt("markerChance", 100);

        if (subType == SubType.SCHEMATIC) {
            if (!configSection.contains("schematicList")) {
                DebugLogger.log("No schematicList is available for Marker with SCHEMATIC subtype", DebugLogger.DebugType.MISC);
            } else {
                ConfigurationSection schematicsListSection = configSection.getConfigurationSection("schematicList");
                for (String listName : schematicsListSection.getKeys(false)) {
                    DebugLogger.log("Found subschematic: " + listName, DebugLogger.DebugType.MARKER);
                    schematicsList.put(listName, schematicsListSection.getDouble(listName + ".chance", 0.0));
                }
            }
        }
        blocksList = configSection.getStringList("blockList");
    }

    public void createInWorld(Location location) throws IOException, DataException, WorldEditException, JsonException, ParseException {
        if (subType == SubType.MOB) {
            createSpawner(location, SpawnerType.MOB);
            return;
        }
        if (subType == SubType.MYTHICMOB) {
            createSpawner(location, SpawnerType.MYTHICMOB);
            return;
        }
        if (subType == SubType.MOBSPAWNER) {
            createSpawner(location, SpawnerType.MOBSPAWNER, Material.SPAWNER);
            return;
        }
        if (subType == SubType.MYTHICMOBSPAWNER) {
            createSpawner(location, SpawnerType.MYTHICMOBSPAWNER);
            return;
        }
        if (subType == SubType.SCHEMATIC) {
            location.getBlock().setType(Material.AIR);
            createSchematic(location);
            return;
        }
        if (subType == SubType.BLOCK) {
            createBlock(location);
            return;
        }
        if (subType == SubType.NONE) {
            location.getBlock().setType(Material.AIR);
        }
    }

    private void createSpawner(Location location, SpawnerType spawnerType) {
        createSpawner(location, spawnerType, Material.AIR);
    }

    private void createSpawner(Location location, SpawnerType spawnerType, Material type) {
        SchematicSpawner spawner = new SchematicSpawner(getLocation(), configSection(), getName(), spawnerType);
        spawner.createInWorld(location);
    }

    private void createSchematic(Location location) throws IOException, DataException, WorldEditException, JsonException, ParseException {
        if (schematicsList.isEmpty()) {
            DebugLogger.log("Attempted to spawn sub-schematic, but there are none to choose from in config!", DebugLogger.DebugType.MISC);
            return;
        }
        String listSchematic = MapUtils.getFromWeightedMap(schematicsList);
        boolean skipChecks = configSection().getBoolean("schematicList." + listSchematic + ".skipChecks", true);
        int rotation = configSection().getInt("schematicList." + listSchematic + ".rotation", -1);

        // Won't spawn schematic is set to NO_SCHEMATIC
        if (!listSchematic.equalsIgnoreCase("NO_SCHEMATIC")) {
            if (rotation == -1) {
                SchematicManager.spawn(listSchematic, location, skipChecks);
                return;
            }
            SchematicManager.spawn(listSchematic, location, rotation, skipChecks);
            DebugLogger.log("Rotating schematic to: " + rotation, DebugLogger.DebugType.MARKER);
        }
    }

    private void createBlock(Location location) {
        if (blocksList.isEmpty()) {
            DebugLogger.log("Attempted to replace marker with block, but there are none to choose from in config!", DebugLogger.DebugType.MISC);
            return;
        }
        String randomBlock = blocksList.get(random.nextInt(blocksList.size())).toUpperCase();
        location.getBlock().setType(Material.valueOf(randomBlock));
    }

    public enum SubType {
        MOB, MOBSPAWNER, MYTHICMOB, MYTHICMOBSPAWNER, CHEST, TRAPPEDCHEST, SCHEMATIC, BLOCK, NONE
    }

}
