package optic_fusion1.worldschematics.schematicblock;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.mobs.MobManager;
import io.lumine.xikage.mythicmobs.skills.placeholders.parsers.PlaceholderInt;
import io.lumine.xikage.mythicmobs.spawning.spawners.MythicSpawner;
import io.lumine.xikage.mythicmobs.spawning.spawners.SpawnerManager;
import io.lumine.xikage.mythicmobs.utils.numbers.RandomInt;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import worldschematics.WorldSchematics;
import worldschematics.util.DebugLogger;

/**
 * Represents a spawner within a schematic.
 */
public class SchematicSpawner extends AbstractSchematicBlock {

    private List<String> mobsList = new ArrayList<>();
    private SpawnerType spawnerType;

    public SchematicSpawner(Location schematicLocation, ConfigurationSection configSection, String name, SpawnerType type) {
        super(schematicLocation, configSection, name);
        this.spawnerType = type;
        mobsList = configSection.getStringList("mobs");
    }

    public List<String> mobsList() {
        return mobsList;
    }

    public void setMobsList(List<String> mobsList) {
        this.mobsList = mobsList;
    }

    public void createInWorld(Location location) {
        // Check if the list has mobs in it first
        if (mobsList == null || mobsList.isEmpty()) {
            DebugLogger.log("Mobs list for the spawner is empty, leaving as is", DebugLogger.DebugType.MOBSPAWNING);
            return;
        }
        if (spawnerType == SpawnerType.MOB) {
            createMob(location);
            return;
        }
        if (spawnerType == SpawnerType.MYTHICMOB) {
            createMythicMob(location);
            return;
        }
        if (spawnerType == SpawnerType.MOBSPAWNER) {
            createMobSpawner(location);
            return;
        }
        if (spawnerType == SpawnerType.MYTHICMOBSPAWNER) {
            createMythicMobSpawner(location);
        }
    }

    private void createMob(Location location) {
        DebugLogger.log("Found Mob in config, attempting to spawn", DebugLogger.DebugType.MOBSPAWNING);
        Random random = new Random();
        String randomMob = mobsList().get(random.nextInt(mobsList().size()));
        int amount = configSection().getInt("properties.amount", 1);

        DebugLogger.log("Spawning " + amount + "of Mob " + randomMob + " at " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ(), DebugLogger.DebugType.MOBSPAWNING);
        // We need to loop the amount we want since there is no way to spawn a specific amount
        for (int i = 0; i < amount; i++) {
            location.getWorld().spawnEntity(location, EntityType.valueOf(randomMob));
        }
    }

    private void createMythicMob(Location location) {
        DebugLogger.log("Found MythicMob in config, attempting to spawn", DebugLogger.DebugType.MOBSPAWNING);
        if (!WorldSchematics.getInstance().getMythicMobsInstalled()) {
            return;
        }
        Random random = new Random();
        String randomMob = mobsList().get(random.nextInt(mobsList().size()));
        int amount = configSection().getInt("properties.amount", 1);

        // Get the instance of MythicMobs and remove the spawner block itself
        location.getBlock().setType(Material.AIR);

        DebugLogger.log("Spawning " + amount + " of MythicMob " + randomMob + " at " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ(), DebugLogger.DebugType.MOBSPAWNING);

        // We need to loop the amount we want since there is no way to spawn a specific amount
        MobManager mobManager = MythicMobs.inst().getMobManager();
        if (mobManager == null) {
            DebugLogger.log("Attempted to spawn MythicMob, but MythicMobs is not loaded yet!", DebugLogger.DebugType.MOBSPAWNING);
            return;
        }
        for (int i = 0; i < amount; i++) {
            if (configSection().contains("properties.level")) {
                // Takes into account mob level
                mobManager.spawnMob(randomMob, location, configSection().getInt("properties.mobLevel"));
                continue;
            }
            mobManager.spawnMob(randomMob, location);
        }
    }

    private void createMobSpawner(Location location) {
        DebugLogger.log("Found Mob Spawner in config, attempting to create", DebugLogger.DebugType.MOBSPAWNING);
        Block block = location.getBlock();
        block.setType(Material.AIR);
        block.setType(Material.SPAWNER);

        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        Random random = new Random();
        String randomMob = mobsList().get(random.nextInt(mobsList().size()));
        spawner.setSpawnedType(EntityType.valueOf(randomMob));

        // Set cooldown if specified
        if (configSection().contains("properties.cooldown")) {
            spawner.setDelay(configSection().getInt("properties.cooldown"));
        }
        spawner.update();
        DebugLogger.log("Creating  " + randomMob + " spawner at " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ(), DebugLogger.DebugType.MOBSPAWNING);
    }

    private void createMythicMobSpawner(Location location) {
        DebugLogger.log("Found MythicMobs spawner in config, attempting to create", DebugLogger.DebugType.MOBSPAWNING);
        if (!WorldSchematics.getInstance().getMythicMobsInstalled()) {
            WorldSchematics.getInstance().getLogger().info("tried creating MythicMobs spawner but MythicMobs is not installed");
            return;
        }
        Random random = new Random();
        String randomMob = mobsList().get(random.nextInt(mobsList().size()));
        SpawnerManager spawnerManager = MythicMobs.inst().getSpawnerManager();
        Block block = location.getBlock();
        String spawnerName = "WorldSchamatics_" + location.getWorld().getName() + "_x" + location.getBlockX() + "_y" + location.getBlockY() + "_z" + location.getBlockZ() + "_" + randomMob;
        block.setType(Material.AIR);
        spawnerManager.createSpawner(spawnerName, location, randomMob);
        DebugLogger.log("Creating MythicMobs spawner using mob " + randomMob + " at " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ(), DebugLogger.DebugType.MOBSPAWNING);
        // Set options for the spawner
        MythicSpawner spawner = spawnerManager.getSpawnerByName(spawnerName);
        if (configSection().contains("properties.checkForPlayers")) {
            spawner.setCheckForPlayers(true);
        }
        if (configSection().contains("properties.cooldown")) {
            spawner.setCooldownSeconds(configSection().getInt("properties.cooldown"));
        }
        if (configSection().contains("properties.group")) {
            spawner.setGroup(configSection().getString("properties.group"));
        }
        if (configSection().contains("properties.leashRange")) {
            spawner.setLeashRange(configSection().getInt("properties.leashrange"));
        }
        if (configSection().contains("properties.maxMobs")) {
            spawner.setMaxMobs(new PlaceholderInt(String.valueOf(configSection().getInt("properties.maxMobs"))));
        }
        if (configSection().contains("properties.mobLevel")) {
            spawner.setMobLevel(new RandomInt(configSection().getInt("properties.mobLevel")));
        }
        if (configSection().contains("properties.mobsPerSpawn")) {
            spawner.setMobsPerSpawn(configSection().getInt("properties.mobsPerSpawn"));
        }
        if (configSection().contains("properties.spawnRadius")) {
            spawner.setSpawnRadius(configSection().getInt("properties.spawnRadius"));
        }
        if (configSection().contains("properties.showFlames")) {
            spawner.setShowFlames(configSection().getBoolean("properties.showFlames"));
        }
        if (configSection().contains("properties.warmupSeconds")) {
            spawner.setWarmupSeconds(configSection().getInt("properties.warmupSeconds"));
        }
    }

    public enum SpawnerType {
        MOB, MOBSPAWNER, MYTHICMOB, MYTHICMOBSPAWNER
    }

}
