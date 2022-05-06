package optic_fusion1.worldschematics;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.DataException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import optic_fusion1.worldschematics.util.Utils;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import worldschematics.WorldSchematics;
import worldschematics.util.DebugLogger;

// Handles loading schematics into memory, as well as spawning them into the world when the ChunkListener loads a chunk
// Also determines if the schematic meets the conditions needed to be placed
public class SchematicManager implements Listener {

    private static WorldSchematics plugin;
    private static HashMap<String, SpawnSchematic> SCHEMATICS = new HashMap<>();
    private static List<World> LOADED_WORLDS = new ArrayList<>();

    public SchematicManager(WorldSchematics plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("Initializing Schematic Manager");
        for (World world : plugin.getServer().getWorlds()) {
            plugin.getLogger().log(Level.INFO, "Loading world: {0}", world.getName());
            try {
                loadWorld(world);
            } catch (DataException | ParseException | IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void loadWorld(World world) throws DataException, ParseException, IOException {
        File worldFolder = new File(plugin.getDataFolder(), "Schematics/" + world.getName());
        if (!worldFolder.exists()) {
            plugin.getLogger().log(Level.INFO, "Folder for {0} doesn''t exist, creating doler", world.getName());
            worldFolder.mkdirs();
        } else {
            DebugLogger.log("Folder for " + world.getName() + " already exists");
        }
        loadSchematics(world);

        // Add to list of loaded worlds
        LOADED_WORLDS.add(world);
        DebugLogger.log("World " + world.getName() + " loaded");
    }

    private void loadSchematics(World world) throws DataException, IOException, ParseException {
        DebugLogger.log("Loading Schematics from world " + world.getName() + " into memory");
        File worldPath = new File(plugin.getDataFolder(), "Schematics/" + world.getName());
        File[] files = worldPath.listFiles();

        for (File file : files) {
            String fileExt = FilenameUtils.getExtension(file.getAbsolutePath());
            if (fileExt.equals("schematic") || fileExt.equals("schem")) {
                String schematicFileName = FilenameUtils.removeExtension(file.getName());

                // Check if the config file exists, and if not copy default config
                File configFile = new File(worldPath, schematicFileName + ".yml");
                if (!configFile.exists()) {
                    DebugLogger.log("Schematic doesn't have config file, creating config file");
                    Utils.copy(plugin.getResource("ExampleSchematic.yml"), configFile);
                }

                SpawnSchematic spawnSchematic = new SpawnSchematic(schematicFileName, world);
                // Make sure we set the world so we know which world this should be set to spawn in
                SCHEMATICS.put(spawnSchematic.name(), spawnSchematic);
                DebugLogger.log("+ Loaded Schematic " + spawnSchematic.name());
            }
        }
    }

    private void clearSchematics() {
        SCHEMATICS.clear();
    }

    public void reloadSchematics() throws IOException, DataException, ParseException {
        clearSchematics();

        for (World world : LOADED_WORLDS) {
            loadSchematics(world);
        }
    }

    private void spawnInChunk(Chunk chunk, World world, SpawnSchematic schematic) throws com.sk89q.worldedit.world.DataException, IOException, WorldEditException, ParseException {
        // Config data for the schematic
        FileConfiguration data = schematic.schematicConfig();

        // Used to randomly place a schematic within a chunk
        Random randX = new Random();
        Random randY = new Random();
        Random randZ = new Random();

        // Positions of the chunk. No need for Y
        int chunkX = chunk.getX() * 16;
        int chunkZ = chunk.getZ() * 16;

        int basementDepth = data.getInt("heightAdjustment", 0);
        int minY = data.getInt("minY", 60);
        int maxY = data.getInt("maxY", 120);

        // Position where we will paste the schematic
        int pastePosX = chunkX + randX.nextInt(16);
        int pastePosY;
        int pastePosZ = chunkZ + randZ.nextInt(16);

        // Determine positioning
        String position = data.getString("place", "ground");

        // Determine the Y value of where to place the schematic
        if (position.equals("anywhere")) {
            pastePosY = minY + randY.nextInt(maxY);
        } else if (position.equals("air")) {
            pastePosY = world.getHighestBlockYAt(pastePosX, pastePosZ) + 1 + randY.nextInt(maxY);
        } else if (position.equals("underground")) {
            pastePosY = world.getHighestBlockYAt(pastePosX, pastePosZ) - randY.nextInt(minY);
        } else {
            // Paste on ground as default
            pastePosY = world.getHighestBlockYAt(pastePosX, pastePosZ);
        }

        // Take basementDepth into account NOTE: This config option is deprecated now
        pastePosY += basementDepth;

        // Take into account offset
        pastePosX = pastePosX + schematic.getConfigOffsetX();
        pastePosY = pastePosY + schematic.getConfigOffsetY();
        pastePosZ = pastePosZ + schematic.getConfigOffsetZ();

        // TODO: Check if the file is read from disk each time.
        if (plugin.isSpawnSchematicsOn() && schematic.isEnabled()) {
            schematic.spawn(world, pastePosX, pastePosY, pastePosZ);
        }
    }

    public static void spawn(String schematicName, Location location) throws WorldEditException, com.sk89q.worldedit.world.DataException, ParseException, IOException {
        SpawnSchematic schematic = SCHEMATICS.get(schematicName);

        // TODO: Check if the file is read from disk each time
        if (schematic == null) {
            DebugLogger.log("Attempted to spawn schematic, unable to find schematic: " + schematicName, DebugLogger.DebugType.MISC);
            return;
        }

        if (plugin.isSpawnSchematicsOn()) {
            schematic.spawn(location);
        }
    }

    public static void spawn(String schematicName, Location location, int rotation) throws WorldEditException, com.sk89q.worldedit.world.DataException, ParseException, IOException {
        spawn(schematicName, location, rotation, false);
    }

    public static void spawn(String schematicName, Location location, boolean skipChecks) throws WorldEditException, com.sk89q.worldedit.world.DataException, ParseException, IOException {
        spawn(schematicName, location, -1, skipChecks);
    }

    public static void spawn(String schematicName, Location location, int rotation, boolean skipChecks) throws WorldEditException, com.sk89q.worldedit.world.DataException, ParseException, IOException {
        // Defensive copy to prevent original from modification
        SpawnSchematic schematicCopy = SCHEMATICS.get(schematicName);

        // TODO: Check if the file is read from disk each time
        if (schematicCopy == null) {
            DebugLogger.log("Attempted to spawn schematic, unable to find schematic: " + schematicName, DebugLogger.DebugType.MISC);
            return;
        }

        if (plugin.isSpawnSchematicsOn()) {
            schematicCopy.spawn(location, rotation, skipChecks);
        }
    }

    // Can't get a list of worlds on startup, so lets listen for them instead as the server starts and loads them
    @EventHandler
    private void on(WorldLoadEvent event) throws DataException, IOException, ParseException {
        loadWorld(event.getWorld());
    }

    // Remove world if its unloaded
    @EventHandler
    public void on(WorldUnloadEvent event) {
        LOADED_WORLDS.remove(event.getWorld());
    }

    // Whenever a new chunk is created
    @EventHandler
    public void onChunkPopulate(ChunkPopulateEvent event) throws IOException, WorldEditException, ParseException, com.sk89q.worldedit.world.DataException {
        DebugLogger.log("new chunk created, looking to spawn schematic", DebugLogger.DebugType.WORLDGENERATION);

        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();

        // If world is not loaded, load world
        if (!LOADED_WORLDS.contains(world)) {
            loadWorld(world);
        }

        if (chunk.load()) {
            if (SCHEMATICS.isEmpty()) {
                DebugLogger.log("No schematics available for this world", DebugLogger.DebugType.WORLDGENERATION);
                return;
            }

            for (SpawnSchematic schematic : SCHEMATICS.values()) {
                // Check and make sure the schematic would be spawned in this world
                if (!schematic.world().getName().equals(world.getName())) {
                    continue;
                }

                // Check the chances of the schematic spawning here
                Random spawnChance = new Random();

                // Get change of spawn from schematics config
                double chanceOfSpawn = schematic.schematicConfig().getDouble("chance", 10);
                DebugLogger.log("Chance of schematic spawning: " + chanceOfSpawn, DebugLogger.DebugType.WORLDGENERATION);
                if (0 + (10000 - 0) * spawnChance.nextDouble() <= chanceOfSpawn) {
                    spawnInChunk(chunk, world, schematic);
                }
            }
        }
    }

}
