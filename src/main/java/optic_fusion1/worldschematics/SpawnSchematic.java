package optic_fusion1.worldschematics;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.json.JsonException;
import optic_fusion1.worldschematics.schematicblock.SchematicContainer;
import optic_fusion1.worldschematics.schematicblock.SchematicMarker;
import optic_fusion1.worldschematics.schematicblock.SchematicSpawner;
import optic_fusion1.worldschematics.util.Utils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import worldschematics.WorldSchematics;
import worldschematics.util.DebugLogger;

public class SpawnSchematic {

    private static WorldSchematics WORLD_SCHEMATICS = WorldSchematics.instance();
    private Random spawnChance;
    private boolean noSpawn;
    private String name;
    private World world;
    private Random random;

    // Config options
    private String place;
    private boolean pasteAir;
    private boolean pasteEntities;
    private boolean restrictBiomes;
    private List<String> biomesList = new ArrayList<>();
    private List<String> blockBlacklist = new ArrayList<>();
    private List<String> regionFlagList = new ArrayList<>();
    private boolean whitelistMode;
    private boolean biomeBlacklistMode;
    private boolean randomRotate;
    private double chanceOfSpawn;
    private int basementDepth;
    private int minY;
    private int maxY;
    private int timesSpawned;
    private int maxSpawns;
    private Location location;
    private Chunk chunk;
    private int chunkX;
    private int chunkZ;
    private FileConfiguration data;
    private FileConfiguration blockDataConfig;
    private File configFile;
    private File blockDataConfigFile;

    // Used to randomly place a schematic within a chunk
    private Random randX = new Random();
    private Random randY = new Random();
    private Random randZ = new Random();

    // Dimensions and stuff of the schematic
    private int width;
    private int length;
    private int height;
    private BlockVector3 origin;
    private Vector3 offset;

    private int offsetX;
    private int offsetY;
    private int offsetZ;

    private boolean enabled;

    private File worldPath;
    private File schematicFile;

    private double version;

    private File dataFolder = WORLD_SCHEMATICS.getDataFolder();

    // Special blocks
    private static final List<SchematicSpawner> SPAWNERS = new ArrayList<>();
    private static final List<SchematicContainer> CONTAINERS = new ArrayList<>();
    private static final List<SchematicMarker> MARKERS = new ArrayList<>();

    public SpawnSchematic(String name, World world) throws IOException, JsonException, ParseException {
        DebugLogger.log("==Schematic Properties==", DebugLogger.DebugType.SCHEMATICINFO);

        // Load the schematic
        this.name = name;
        this.world = world;

        DebugLogger.log("Name: " + this.name, DebugLogger.DebugType.SCHEMATICINFO);

        worldPath = new File(dataFolder, "Schematics/" + world.getName());
        schematicFile = new File(worldPath, name + ".schematic");

        // Test if the schematic is a sponge schematic instead
        if (!schematicFile.exists()) {
            DebugLogger.log("Schematic is Sponge Schematic", DebugLogger.DebugType.SCHEMATICINFO);
            schematicFile = new File(worldPath, name + ".schem");
        }
        DebugLogger.log("Schematic path: " + schematicFile.getCanonicalPath(), DebugLogger.DebugType.SCHEMATICINFO);

        // Load schematic into temporary clipboard so we can read some of its properties
        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);

        try ( ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            Clipboard tempClipboard = reader.read();
            BlockVector3 dimensions = tempClipboard.getDimensions();
            width = dimensions.getX();
            height = dimensions.getY();
            length = dimensions.getZ();
            origin = tempClipboard.getOrigin();
        }
        blockDataConfigFile = new File(worldPath, name + "-blockdata.yml");
        configFile = new File(worldPath, name + ".yml");

        if (!blockDataConfigFile.exists()) {
            DebugLogger.log("Schematic config file doesn't exist", DebugLogger.DebugType.SCHEMATICINFO);
        }

        // Load info from config file
        loadConfig();

        // Load special blocks in the schematic
        loadSchematicBlocks();

        DebugLogger.log("========================", DebugLogger.DebugType.SCHEMATICINFO);
    }

    public SpawnSchematic(SpawnSchematic schematic) throws IOException, com.sk89q.worldedit.world.DataException, JsonException, ParseException {
        this(schematic.name, schematic.world);
    }

    public void spawn(Location location) throws WorldEditException, IOException, com.sk89q.worldedit.world.DataException, JsonException, ParseException {
        spawn(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), -1, false);
    }

    public void spawn(Location location, Boolean skipChecks) throws WorldEditException, IOException, com.sk89q.worldedit.world.DataException, JsonException, ParseException {
        spawn(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), -1, skipChecks);
    }

    public void spawn(Location location, int rotation, Boolean skipChecks) throws WorldEditException, IOException, com.sk89q.worldedit.world.DataException, JsonException, ParseException {
        spawn(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), rotation, skipChecks);
    }

    public void spawn(World world, int x, int y, int z) throws WorldEditException, IOException, com.sk89q.worldedit.world.DataException, JsonException, ParseException {
        spawn(world, x, y, z, -1, false);
    }

    public void spawn(World world, int x, int y, int z, Boolean SkipChecks) throws WorldEditException, IOException, com.sk89q.worldedit.world.DataException, JsonException, ParseException {
        spawn(world, x, y, z, -1, SkipChecks);
    }

    // Check for valid spawn location, handle rotation, etc, here
    public void spawn(World world, int x, int y, int z, int parRotation, boolean skipChecks) throws WorldEditException, IOException, com.sk89q.worldedit.world.DataException, JsonException, ParseException {
        DebugLogger.log("==Spawning Debug Info==", DebugLogger.DebugType.SCHEMATICSPAWNING);

        int rotation = 0;
        location = new Location(world, x, y, z);
        chunk = location.getChunk();
        chunkX = chunk.getX() * 16;
        chunkZ = chunk.getZ() * 16;

        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        try ( ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            Clipboard clipboard = reader.read();

            biomesList = data.getStringList("biomeList");

            DebugLogger.log("Spawning Schematic " + name + " at position: " + x + " " + y + " " + z, DebugLogger.DebugType.SCHEMATICSPAWNING);

            if (randomRotate && parRotation != -1) {
                int randomNum = 0 + (int) (Math.random() * 4);
                switch (randomNum) {
                    case 1 -> {
                        rotation = 90;
                    }
                    case 2 -> {
                        rotation = 180;
                    }
                    case 3 -> {
                        rotation = 270;
                    }
                    default -> {
                        rotation = 0;
                    }
                }
            }
            DebugLogger.log("Rotation = " + rotation, DebugLogger.DebugType.SCHEMATICSPAWNING);
            boolean biomeCheck = false;
            boolean heightCheck = false;
            boolean spawnLimitCheck = false;
            boolean blockCheck = false;

            // Check minY and maxY
            if (y < maxY || y > minY) {
                heightCheck = true;
            } else {
                DebugLogger.log("Schematic was set to be spawned outside of minY and maxY, cancelled spawning", DebugLogger.DebugType.SCHEMATICSPAWNING);
            }

            // Check spawn limits
            if (maxSpawns == 0) {
                spawnLimitCheck = true;
            } else {
                if (timesSpawned >= maxSpawns) {
                    DebugLogger.log("Schematic has reached the maximum amount of times it can be spawned, canceled spawning", DebugLogger.DebugType.SCHEMATICSPAWNING);
                } else {
                    spawnLimitCheck = true;
                }
            }

            // Check biome
            if (!restrictBiomes) {
                biomeCheck = true;
            } else {
                boolean doesBiomeMatchList = false;
                String currentBiome = world.getBiome(x, y, z).name().toUpperCase();

                DebugLogger.log("Checking biome of chunk. Biome: " + currentBiome, DebugLogger.DebugType.SCHEMATICSPAWNING);

                for (String biomeName : biomesList) {
                    biomeName = biomeName.toUpperCase();
                    DebugLogger.log("Blacklist BiomeName = " + biomeName, DebugLogger.DebugType.SCHEMATICSPAWNING);

                    if (biomeName.equals(currentBiome)) {
                        doesBiomeMatchList = true;
                    }
                }

                if (doesBiomeMatchList) {
                    if (biomeBlacklistMode) {
                        biomeCheck = false;
                        DebugLogger.log("The biome the schematic is set to spawn in is not in the blacklist, canceled spawning", DebugLogger.DebugType.SCHEMATICSPAWNING);
                    } else {
                        biomeCheck = true;
                    }
                } else {
                    if (biomeBlacklistMode) {
                        biomeCheck = true;
                    } else {
                        biomeCheck = false;
                        DebugLogger.log("The biome the schematic is set to spawn in is not in the whitelist, canceled spawning", DebugLogger.DebugType.SCHEMATICSPAWNING);
                    }
                }
            }

            // Check block schematic is spawning on
            if (blockBlacklist != null & !blockBlacklist.isEmpty()) {
                boolean doesBlockMatchList = false;
                DebugLogger.log("Whitelsit/Blacklist is not empty, checking list", DebugLogger.DebugType.SCHEMATICSPAWNING);

                for (String materialName : blockBlacklist) {
                    // Get block below the paste position
                    Location loc = new Location(world, x, y - 1, z);
                    Block block = loc.getBlock();
                    DebugLogger.log("Checking if block below schematic is " + materialName, DebugLogger.DebugType.SCHEMATICSPAWNING);
                    DebugLogger.log("Block below schematic location is " + block.getType().toString(), DebugLogger.DebugType.SCHEMATICSPAWNING);
                    if (block.getType().toString().equals(materialName)) {
                        DebugLogger.log("Blocks below schematic mathc block on list", DebugLogger.DebugType.SCHEMATICSPAWNING);
                        doesBlockMatchList = true;
                    }
                }
                //
                if (doesBlockMatchList) {
                    if (whitelistMode) {
                        blockCheck = true;
                    } else {
                        blockCheck = false;
                        DebugLogger.log("Blocks below schematic are on blacklist, canceled spawning", DebugLogger.DebugType.SCHEMATICSPAWNING);
                    }
                } else {
                    if (whitelistMode) {
                        blockCheck = false;
                        DebugLogger.log("Blocks below schematic are on whitelist, canceled spawning", DebugLogger.DebugType.SCHEMATICSPAWNING);
                    } else {
                        blockCheck = true;
                    }
                }
            } else {
                // No blocks in blacklist/whitelist
                blockCheck = true;
            }
            if (biomeCheck && heightCheck && spawnLimitCheck && blockCheck || skipChecks) {
                if (WorldSchematics.instance().isShowLocation()) {
                    WorldSchematics.instance().getLogger().info("Schematic passed all checks. Spawned schematic at: " + x + " " + y + " " + z);
                }

                // Increase spawn count
                if (maxSpawns > 0) {
                    data.set("timesSpawned", timesSpawned++);
                    data.save(configFile);
                }

                com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
                try ( EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1)) {
                    AffineTransform transform = new AffineTransform();
                    transform = transform.rotateZ(rotation);
                    ClipboardHolder holder = new ClipboardHolder(clipboard);

                    // Apply rotation
                    holder.setTransform(holder.getTransform().combine(transform));

                    // Perform paste
                    Operation operation = holder
                        .createPaste(editSession)
                        .to(BlockVector3.at(x, y, z))
                        .copyEntities(this.pasteEntities)
                        .ignoreAirBlocks(false)
                        .build();
                    Operations.complete(operation);
                }
            }
        }
        DebugLogger.log("==End Spawning Debug Info==", DebugLogger.DebugType.SCHEMATICSPAWNING);
    }

    private void loadConfig() throws IOException, JsonException, ParseException {
        // Load config file for the schematic
        if (!configFile.exists()) {
            DebugLogger.log("Schematic doesn't have config file, creating config file", DebugLogger.DebugType.SCHEMATICINFO);
            Utils.copy(WorldSchematics.instance().getResource("ExampleSchematic.yml"), configFile);
        }

        // load config file for the schematic blockdata
        if (!blockDataConfigFile.exists()) {
            DebugLogger.log("Schematic doesnt have config file, creating config file", DebugLogger.DebugType.SCHEMATICINFO);
            Utils.copy(WorldSchematics.instance().getResource("ExampleSchematic-blockdata.yml"), blockDataConfigFile);
            //populate the config file with SPAWNERS in schematic, if there are any
            blockDataConfig = YamlConfiguration.loadConfiguration(blockDataConfigFile);
        }
        data = YamlConfiguration.loadConfiguration(configFile);
        blockDataConfig = YamlConfiguration.loadConfiguration(blockDataConfigFile);

        populateEntityConfig();

        DebugLogger.log("Name: " + name, DebugLogger.DebugType.SCHEMATICINFO);

        pasteAir = data.getBoolean("pasteAir", false);
        pasteEntities = data.getBoolean("pasteEntities", false);
        place = data.getString("place", "ground");
        restrictBiomes = data.getBoolean("restrictBiomes", false);
        basementDepth = data.getInt("heightAdjustment", 0);
        minY = data.getInt("minY", 60);
        maxY = data.getInt("maxY", 70);
        timesSpawned = data.getInt("timesSpawned", 0);
        maxSpawns = data.getInt("maxSpawns", 0);
        spawnChance = new Random();
        blockBlacklist = data.getStringList("blacklist");
        biomesList = data.getStringList("biomeList");
        biomeBlacklistMode = data.getBoolean("biomeBlacklistMode", false);
        whitelistMode = data.getBoolean("whitelistMode", false);
        randomRotate = data.getBoolean("randomRotate", true);
        chanceOfSpawn = data.getDouble("chance", 100);
        basementDepth = data.getInt("heightAdjustment", 0);
        minY = data.getInt("minY", 60);
        maxY = data.getInt("maxY", 70);
        timesSpawned = data.getInt("timesSpawned", 0);
        maxSpawns = data.getInt("maxSpawns", 0);
        version = data.getDouble("configVersion", 2);
        offsetX = data.getInt("offset.x", 0);
        offsetY = data.getInt("offset.y", 0);
        offsetZ = data.getInt("offset.z", 0);
        regionFlagList = data.getStringList("regionSettings.regionFlags");
        enabled = data.getBoolean("enabled", true);
    }

    private void populateEntityConfig() throws IOException {
        Clipboard clipboard;

        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        try ( ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            clipboard = reader.read();

            //gets the first block in the schematic
            int SpecialBlockCount = 0;
            int MarkerCount = 0;

            if (clipboard != null) {
                DebugLogger.log("Schematic Dimensions: X=" + clipboard.getDimensions().getX() + " Y=" + clipboard.getDimensions().getY() + " Z=" + clipboard.getDimensions().getZ(), DebugLogger.DebugType.MOBSPAWNING);

                //iterate over every block in the schematic
                for (int x = 0; x < clipboard.getDimensions().getX(); x++) {
                    for (int y = 0; y < clipboard.getDimensions().getY(); y++) {
                        for (int z = 0; z < clipboard.getDimensions().getZ(); z++) {
                            BlockVector3 bVector = BlockVector3.at(x, y, z);

                            BlockState CurrentBlock = clipboard.getBlock(bVector);

                            BlockType bType = CurrentBlock.getBlockType();
                            BaseBlock bBlock = CurrentBlock.toBaseBlock();

                            DebugLogger.log("Block at " + x + " " + y + " " + z + " is: " + bType.getId(), DebugLogger.DebugType.MOBSPAWNING);

                            //if block is a spawner
                            if (bType.getId().equals(BlockTypes.SPAWNER)) {

                                SpecialBlockCount++;
                                String BlockName = "BlockNBT" + SpecialBlockCount;

                                DebugLogger.log("Block at " + x + " " + y + " " + z + " is a spawner", DebugLogger.DebugType.MOBSPAWNING);

                                if (blockDataConfig.contains("Blocks." + BlockName) == false) {
                                    blockDataConfig.createSection("Blocks." + BlockName);
                                }

                                if (blockDataConfig.contains("Blocks." + BlockName + ".type") == false) {
                                    blockDataConfig.createSection("Blocks." + BlockName + ".type");
                                    blockDataConfig.set("Blocks." + BlockName + ".type", "spawner");
                                }

                                blockDataConfig.createSection("Blocks." + BlockName + ".x");
                                blockDataConfig.createSection("Blocks." + BlockName + ".y");
                                blockDataConfig.createSection("Blocks." + BlockName + ".z");

                                if (blockDataConfig.contains("Blocks." + BlockName + ".mobs") == false) {
                                    blockDataConfig.createSection("Blocks." + BlockName + ".mobs");
                                }

                                if (blockDataConfig.contains("Blocks." + BlockName + ".properties") == false) {
                                    blockDataConfig.createSection("Blocks." + BlockName + ".properties");
                                }

                                blockDataConfig.set("Blocks." + BlockName + ".x", x);
                                blockDataConfig.set("Blocks." + BlockName + ".y", y);
                                blockDataConfig.set("Blocks." + BlockName + ".z", z);

                                blockDataConfig.save(blockDataConfigFile);
                            }

                            //if block is a chest of any type
                            if (bType.getId().equals(BlockTypes.CHEST) || bType.equals(BlockTypes.TRAPPED_CHEST)) {

                                SpecialBlockCount++;
                                String BlockName = "BlockNBT" + SpecialBlockCount;

                                DebugLogger.log("Block at " + x + " " + y + " " + z + " is a container", DebugLogger.DebugType.LOOTTABLE);

                                if (blockDataConfig.contains("Blocks." + BlockName) == false) {
                                    blockDataConfig.createSection("Blocks." + BlockName);
                                }

                                if (blockDataConfig.contains("Blocks." + BlockName + ".type") == false) {
                                    blockDataConfig.createSection("Blocks." + BlockName + ".type");
                                    blockDataConfig.set("Blocks." + BlockName + ".type", "container");
                                }

                                blockDataConfig.createSection("Blocks." + BlockName + ".x");
                                blockDataConfig.createSection("Blocks." + BlockName + ".y");
                                blockDataConfig.createSection("Blocks." + BlockName + ".z");

                                if (blockDataConfig.contains("Blocks." + BlockName + ".loottables") == false) {
                                    blockDataConfig.createSection("Blocks." + BlockName + ".loottables");
                                }

                                if (blockDataConfig.contains("Blocks." + BlockName + ".properties") == false) {
                                    blockDataConfig.createSection("Blocks." + BlockName + ".properties");
                                }

                                blockDataConfig.set("Blocks." + BlockName + ".x", x);
                                blockDataConfig.set("Blocks." + BlockName + ".y", y);
                                blockDataConfig.set("Blocks." + BlockName + ".z", z);

                                blockDataConfig.save(blockDataConfigFile);
                            }

                            //if block is a marker sign (wall sign or standing sign)
                            if (bType.getId().equals(BlockTypes.SIGN) || bType.equals(BlockTypes.WALL_SIGN)) {

                                DebugLogger.log("Block at " + x + " " + y + " " + z + " is a sign", DebugLogger.DebugType.MARKER);
                                String[] signText = new String[3];
                                signText[0] = bBlock.getNbtData().getString("Text1");
                                signText[1] = bBlock.getNbtData().getString("Text2");

                                DebugLogger.log("Line1 of sign says: " + signText[0], DebugLogger.DebugType.MARKER);
                                DebugLogger.log("Line2 of sign says: " + signText[1], DebugLogger.DebugType.MARKER);

                                //{"text":""} is a blank sign, we dont do anything with those
                                if (!signText[0].equals("{\"text\":\"\"}") && !signText[1].equals("{\"text\":\"\"}")) {
                                    signText[0] = parseSignText(signText[0]);
                                    signText[1] = parseSignText(signText[1]);
                                    DebugLogger.log("Line1 of sign after parsing: " + signText[0], DebugLogger.DebugType.MARKER);
                                    DebugLogger.log("Line2 of sign after parsing: " + signText[1], DebugLogger.DebugType.MARKER);
                                }

                                if (signText[0].toLowerCase().equalsIgnoreCase("[Marker]") && !signText.equals("{\"text\":\"\"}") && !signText[1].contains("/") && !signText[1].contains("\\")) {
                                    MarkerCount++;

                                    String BlockName = "Marker" + MarkerCount + "_" + signText[1];

                                    if (blockDataConfig.contains("Blocks." + BlockName) == false) {
                                        blockDataConfig.createSection("Blocks." + BlockName);
                                    }

                                    if (blockDataConfig.contains("Blocks." + BlockName + ".type") == false) {
                                        blockDataConfig.createSection("Blocks." + BlockName + ".type");
                                        blockDataConfig.set("Blocks." + BlockName + ".type", "marker");
                                    }

                                    if (blockDataConfig.contains("Blocks." + BlockName + ".subtype") == false) {
                                        blockDataConfig.createSection("Blocks." + BlockName + ".subtype");
                                        blockDataConfig.set("Blocks." + BlockName + ".subtype", "none");
                                    }

                                    blockDataConfig.createSection("Blocks." + BlockName + ".x");
                                    blockDataConfig.createSection("Blocks." + BlockName + ".y");
                                    blockDataConfig.createSection("Blocks." + BlockName + ".z");

                                    if (blockDataConfig.contains("Blocks." + BlockName + ".properties") == false) {
                                        blockDataConfig.createSection("Blocks." + BlockName + ".properties");
                                    }

                                    if (blockDataConfig.contains("Blocks." + BlockName + ".schematics") == false) {
                                        blockDataConfig.createSection("Blocks." + BlockName + ".schematics");
                                    }

                                    if (blockDataConfig.contains("Blocks." + BlockName + ".mobs") == false) {
                                        blockDataConfig.createSection("Blocks." + BlockName + ".mobs");
                                    }

                                    blockDataConfig.set("Blocks." + BlockName + ".x", x);
                                    blockDataConfig.set("Blocks." + BlockName + ".y", y);
                                    blockDataConfig.set("Blocks." + BlockName + ".z", z);

                                    blockDataConfig.save(blockDataConfigFile);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void loadSchematicBlocks() {
        DebugLogger.log("Schematic special blocks", DebugLogger.DebugType.SCHEMATICINFO);
        ConfigurationSection blocksSection = blockDataConfig.getConfigurationSection("blocks");
        if (blocksSection == null) {
            DebugLogger.log("-blockdata file is empty, schematic contains no special blocks", DebugLogger.DebugType.SCHEMATICINFO);
            return;
        }
        for (String block : blocksSection.getKeys(false)) {
            String type = blocksSection.getString(block + ".type");
            DebugLogger.log("Found block " + block + ", type is " + type, DebugLogger.DebugType.SCHEMATICINFO);

            Location locationInSchematic = new Location(world, blocksSection.getDouble(block + ".x"), blocksSection.getDouble(block + ".y"), blocksSection.getDouble(block + ".z"));
            ConfigurationSection section = blocksSection.getConfigurationSection(block);

            switch (type) {
                case "spawner" ->
                    SPAWNERS.add(new SchematicSpawner(locationInSchematic, section, block, SchematicSpawner.SpawnerType.MOBSPAWNER));
                case "mob" ->
                    SPAWNERS.add(new SchematicSpawner(locationInSchematic, section, block, SchematicSpawner.SpawnerType.MOB));
                case "mythicspawner" ->
                    SPAWNERS.add(new SchematicSpawner(locationInSchematic, section, block, SchematicSpawner.SpawnerType.MYTHICMOBSPAWNER));
                case "mythicmob" ->
                    SPAWNERS.add(new SchematicSpawner(locationInSchematic, section, block, SchematicSpawner.SpawnerType.MYTHICMOB));
                case "container" ->
                    CONTAINERS.add(new SchematicContainer(locationInSchematic, section, block, SchematicContainer.ContainerType.CHEST));
                case "marker" ->
                    MARKERS.add(new SchematicMarker(locationInSchematic, section, block));
                default -> {
                }
            }
        }
    }

    private SchematicSpawner getSpawner(String name) {
        for (SchematicSpawner sp : SPAWNERS) {
            if (sp.getName().equals(name)) {
                return sp;
            }
        }

        return null;
    }

    private SchematicContainer getContainer(String name) {
        for (SchematicContainer ct : CONTAINERS) {
            if (ct.getName().equals(name)) {
                return ct;
            }
        }

        return null;
    }

    private SchematicMarker getMarker(String name) {
        for (SchematicMarker mk : MARKERS) {
            if (mk.getName().equals(name)) {
                return mk;
            }
        }

        return null;
    }

    //hacky way of getting text from sign data since it seems like worldedits method to parse NBT strings does not work
    private String parseSignText(String nbtString) {
        int firstQuoteIndex = nbtString.indexOf("{\"text\":\"") + 9;
        String newString = "NONE";

        DebugLogger.log("firstQuoteIndex = " + firstQuoteIndex, DebugLogger.DebugType.MARKER);

        if (firstQuoteIndex > 0 && firstQuoteIndex != -8) {
            try {
                newString = nbtString.substring(firstQuoteIndex);
            } catch (StringIndexOutOfBoundsException e) {
                DebugLogger.log("some error which makes no sense happened. If markers dont work, try re-exporting your schematic", DebugLogger.DebugType.MARKER);
            }

        }

        DebugLogger.log("newString = " + newString, DebugLogger.DebugType.MARKER);
        int lastQuoteIndex = newString.indexOf("\"}]");
        DebugLogger.log("lastQuoteIndex = " + lastQuoteIndex, DebugLogger.DebugType.MARKER);
        if (lastQuoteIndex > 0) {
            newString = newString.substring(0, lastQuoteIndex).replace(' ', '_');
        }
        DebugLogger.log("newString = " + newString, DebugLogger.DebugType.MARKER);
        return newString;

    }

    public World world() {
        return world;
    }

    public String name() {
        return name;
    }

    public FileConfiguration schematicConfig() {
        return data;
    }

    public FileConfiguration schematicDataConfig() {
        return blockDataConfig;
    }

    public int getConfigOffsetZ() {
        return offsetZ;
    }

    public void setConfigOffsetZ(int offsetZ) {
        this.offsetZ = offsetZ;
    }

    public int getConfigOffsetY() {
        return offsetY;
    }

    public void setConfigOffsetY(int offsetY) {
        this.offsetY = offsetY;
    }

    public int getConfigOffsetX() {
        return offsetX;
    }

    public void setConfigOffsetX(int offsetX) {
        this.offsetX = offsetX;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
