package worldschematics.util;

import worldschematics.WorldSchematics;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles logging of all debug info for the plugin
 */
public class DebugLogger {

    private static boolean DEBUG = false;

    private static boolean DEBUG_LOOT_TABLES = false;
    private static boolean DEBUG_SCHEMATIC_INFO = false;
    private static boolean DEBUG_SCHEMATIC_SPAWNING = false;
    private static boolean DEBUG_MOB_SPAWNING = false;
    private static boolean DEBUG_WORLD_GENERATION = false;
    private static boolean DEBUG_MARKERS = false;

    private static boolean logToFile;

    private static Logger logger;

    private static File logFile;

    public DebugLogger() {
        logger = WorldSchematics.instance().getLogger();
        createLogFile();
    }

    public enum DebugType {
        LOOTTABLE, LOOT, MISC, SCHEMATICINFO, SCHEMATICSPAWNING, MOBSPAWNING, WORLDGENERATION, MARKER, INFO, WARNING
    }

    public static void log(String message) {
        log(message, DebugType.MISC);
    }

    // logs messages only if debug option in config is set to true
    public static void log(String message, DebugType type) {

        if (type == DebugType.WARNING) {
            logger.log(Level.INFO, "[WARNING] {0}", message);
        }

        if (DEBUG == true) {
            if (type == DebugType.MISC) {
                logger.log(Level.INFO, "[Debug] {0}", message);
            }
            if (type == DebugType.SCHEMATICSPAWNING && DEBUG_SCHEMATIC_SPAWNING) {
                logger.log(Level.INFO, "[Debug - SchematicSpawning] {0}", message);
            }
            if (type == DebugType.SCHEMATICINFO && DEBUG_SCHEMATIC_INFO) {
                logger.log(Level.INFO, "[Debug - SchematicInfo] {0}", message);
            }
            if (type == DebugType.LOOTTABLE || type == DebugType.LOOT && DEBUG_LOOT_TABLES) {
                logger.log(Level.INFO, "[Debug - loot] {0}", message);
            }
            if (type == DebugType.MOBSPAWNING && DEBUG_MOB_SPAWNING) {
            }
            logger.log(Level.INFO, "[Debug - MobsSpawning] {0}", message);
            if (type == DebugType.WORLDGENERATION && DEBUG_WORLD_GENERATION) {
                logger.log(Level.INFO, "[Debug - World Generation] {0}", message);
            }
            //writeToLogFile("[Debug - World Generation] " + message);
            if (type == DebugType.MARKER && DEBUG_MARKERS) {
                logger.log(Level.INFO, "[Debug - Markers] {0}", message);
            }
        }
    }

    public static void writeToLogFile(String message) {
        if (logToFile == true && WorldSchematics.instance().isFinishedLoading() == true && logFile.exists()) {
            try {
                FileOutputStream fos = new FileOutputStream(logFile);
                try ( BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos))) {
                    bw.write(message);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                logger.info("ERROR: Unable to write to Log file. File not does not exist");
            } catch (IOException e) {
                e.printStackTrace();
                logger.info("ERROR: Unable to write to Log file. Please make sure files exists and file is not protected");
            }

        }

    }

    private void createLogFile() {
        //create log file if it has not been created
        if (logToFile == true) {
            String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
            logFile = new File(WorldSchematics.instance().getDataFolder() + "/logs/" + "WorldSchematics2_" + timeStamp + ".txt");

            if (!logFile.exists()) {
                try {
                    //create any directories needed
                    logFile.getParentFile().mkdirs();

                    logFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isDebug() {
        return DEBUG;
    }

    public void setDebug(boolean debug) {
        logger.log(Level.INFO, "Debug set to: {0}", debug);
        DebugLogger.DEBUG = debug;
    }

    public boolean isDebugLootTables() {
        return DEBUG_LOOT_TABLES;
    }

    public void setDebugLootTables(boolean debugLootTables) {
        logger.log(Level.INFO, "LootTable Debug set to: {0}", debugLootTables);
        DebugLogger.DEBUG_LOOT_TABLES = debugLootTables;
    }

    public boolean isDebugSchematicInfo() {
        return DEBUG_SCHEMATIC_INFO;
    }

    public void setDebugSchematicInfo(boolean debugSchematicInfo) {
        logger.log(Level.INFO, "Schematic Info Debug set to: {0}", debugSchematicInfo);
        DebugLogger.DEBUG_SCHEMATIC_INFO = debugSchematicInfo;
    }

    public boolean isDebugSchematicSpawning() {
        return DEBUG_SCHEMATIC_SPAWNING;
    }

    public void setDebugSchematicSpawning(boolean debugSchematicSpawning) {
        logger.log(Level.INFO, "Schematic Spawning Debug set to: {0}", debugSchematicSpawning);
        DebugLogger.DEBUG_SCHEMATIC_SPAWNING = debugSchematicSpawning;
    }

    public boolean isDebugMobSpawning() {
        return DEBUG_MOB_SPAWNING;
    }

    public void setDebugMobSpawning(boolean debugMobSpawning) {
        logger.log(Level.INFO, "Mobspawning debug set to: {0}", debugMobSpawning);
        DebugLogger.DEBUG_MOB_SPAWNING = debugMobSpawning;
    }

    public boolean isDebugWorldGeneration() {
        return DEBUG_WORLD_GENERATION;
    }

    public void setDebugWorldGeneration(boolean debugWorldGeneration) {
        logger.log(Level.INFO, "WorldGeneration debug set to: {0}", debugWorldGeneration);
        DebugLogger.DEBUG_WORLD_GENERATION = debugWorldGeneration;
    }

    public boolean isLogToFile() {
        return logToFile;
    }

    public void setLogToFile(boolean logToFile) {
        logger.log(Level.INFO, "Log to file set to: {0}", logToFile);
        DebugLogger.logToFile = logToFile;
    }

    public boolean isDebugMarkers() {
        return DEBUG_MARKERS;
    }

    public void setDebugMarkers(boolean debugMarkers) {
        logger.log(Level.INFO, "Markers Debug set to: {0}", debugMarkers);
        DebugLogger.DEBUG_MARKERS = debugMarkers;
    }

}
