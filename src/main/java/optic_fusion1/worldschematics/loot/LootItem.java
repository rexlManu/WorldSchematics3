package optic_fusion1.worldschematics.loot;

import dev.lone.itemsadder.api.CustomStack;
import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.adapters.AbstractItemStack;
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter;
import io.lumine.xikage.mythicmobs.items.MythicItem;
import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import worldschematics.WorldSchematics;
import worldschematics.util.DebugLogger;

public class LootItem {

  private String configItemName;
  private String lootTable;
  private File dataFolder = WorldSchematics.instance().getDataFolder();
  private File lootFile;
  private ConfigurationSection lootConfig;

  // ItemStack represents an item in inventory
  private ItemStack item;
  private ItemMeta itemMeta;
  private ItemType itemType;
  private String customPluginItemName;
  private double chance;
  private int minAmount;
  private int maxAmount;
  private int amount;
  private boolean unbreakable;
  private boolean hideAttributes;
  private String configMaterial;
  private String texturePlayer;
  private List<String> lore;

  LootItem(String configItemName, ConfigurationSection lootTable) {
    this.configItemName = configItemName;
    lootConfig = lootTable.getConfigurationSection(configItemName);
    itemType = itemType.valueOf(lootConfig.getString("type", "ITEM").toUpperCase());
    chance = lootConfig.getDouble("chance", 0);
    minAmount = lootConfig.getInt("minAmount", 1);
    maxAmount = lootConfig.getInt("maxAmount", 1);
    customPluginItemName = lootConfig.getString("name", "none");
    configMaterial = lootConfig.getString("material");
    lore = lootConfig.getStringList("lore");
    texturePlayer = lootConfig.getString("playerTexture", "");
    unbreakable = lootConfig.getBoolean("unbreakable", false);
    hideAttributes = lootConfig.getBoolean("hideAttributes", false);
    amount = 1;

    // If this is the case then we need to create the item ourselves from scratch
    if (itemType == ItemType.ITEM) {
      // Create the item
      if (configMaterial != null) {
        try {
          item = new ItemStack(Material.valueOf(configMaterial.toUpperCase()), amount);
        } catch (IllegalArgumentException e) {
          DebugLogger.log("Unknown item material type: " + configMaterial,
              DebugLogger.DebugType.WARNING);
          item = new ItemStack(Material.AIR);
        }
      } else {
        item = new ItemStack(Material.AIR, amount);
      }

      // Get Item MetaData so we can modify it
      itemMeta = item.getItemMeta();

      // Set name, if any
      if (lootConfig.getString("display") != null) {
        itemMeta.setDisplayName(lootConfig.getString("display"));
      }

      // Set lore, if any
      if (lore != null && !lore.isEmpty()) {
        itemMeta.setLore(lore);
      }

      // Set enchantments, if any
      if (lootConfig.getList("enchantments") != null) {
        List<String> enchantmentList = lootConfig.getStringList("enchantments");
        for (String configEnchantment : enchantmentList) {
          // Parse the enchantment and the power level
          String[] splitEnchantment = configEnchantment.split(";");
          int enchantmentLevel = Integer.parseInt(splitEnchantment[1]);

          // Adds the enchantment to the item

          Enchantment enchantment = Enchantment.getByKey(
              NamespacedKey.fromString(splitEnchantment[0]));
          if (enchantment == null) {
            DebugLogger.log("Unknown enchantment: " + splitEnchantment[0],
                DebugLogger.DebugType.WARNING);
            continue;
          }
          itemMeta.addEnchant(enchantment, enchantmentLevel, true);
        }
      }

      // Set unbreakable
      if (unbreakable) {
        itemMeta.setUnbreakable(true);
      }

      // Set flags
      if (hideAttributes) {
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
      }

      // Set skull data if it is a skull
      if (!texturePlayer.equals("") && itemMeta instanceof SkullMeta) {
        ((SkullMeta) itemMeta).setOwner(texturePlayer);
      }
      item.setItemMeta(itemMeta);
      randomize();

      // Set the amount;
      item.setAmount(amount);
      itemMeta = item.getItemMeta();
    }
    if (itemType == ItemType.MYTHICMOBS_ITEM) {
      // Check if MythicMobs plugin is installed first
      if (!WorldSchematics.instance().getMythicMobsInstalled()) {
        WorldSchematics.instance().getLogger()
            .info("Tried to place MythicMobs item in chest, but MythicMobs is not installed!");
        return;
      }
      MythicItem mythicItem = MythicMobs.inst().getItemManager().getItem(customPluginItemName)
          .get();
      int itemAmount = ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
      AbstractItemStack abstractItemStack = mythicItem.generateItemStack(itemAmount);
      item = BukkitAdapter.adapt(abstractItemStack);
    }
    if (itemType == ItemType.ITEMSADDER_ITEM) {
      // Check if ItemsAdder plugin is installed first
      if (!WorldSchematics.instance().isItemsAdderInstalled()) {
        WorldSchematics.instance().getLogger()
            .info("Tried to place ItemsAdder item in chest, but ItemsAdder is not installed!");
        return;
      }

      CustomStack customStack = CustomStack.getInstance(this.customPluginItemName);
      if (customStack == null) {
        WorldSchematics.instance().getLogger()
            .info("ItemAdder item " + this.customPluginItemName + " does not exist!");
        return;
      }
      this.item = customStack.getItemStack();
      this.item.setAmount(ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1));
    }
  }

  public ItemType itemType() {
    return itemType;
  }

  protected void randomize() {
    if (minAmount == maxAmount) {
      item.setAmount(minAmount);
      return;
    }
    amount = ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
    item.setAmount(amount);
  }

  public ItemStack itemStack() {
    return item;
  }

  public ItemMeta itemMeta() {
    return itemMeta;
  }

  public double chance() {
    return chance;
  }

  public enum ItemType {
    ITEM, MYTHICMOBS_ITEM, ITEMSADDER_ITEM;
  }

}
