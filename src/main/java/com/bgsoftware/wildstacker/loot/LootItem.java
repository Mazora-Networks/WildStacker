package com.bgsoftware.wildstacker.loot;

import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.objects.StackedEntity;
import com.bgsoftware.wildstacker.utils.Random;
import com.bgsoftware.wildstacker.utils.items.GlowEnchantment;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class LootItem {

    private static WildStackerPlugin plugin = WildStackerPlugin.getPlugin();

    private final ItemStack itemStack, burnableItem;
    private final double chance;
    private final int min, max;
    private final boolean looting;
    private final String requiredPermission, spawnCauseFilter;

    private LootItem(ItemStack itemStack, @Nullable ItemStack burnableItem, int min, int max, double chance, boolean looting, String requiredPermission, String spawnCauseFilter){
        this.itemStack = itemStack;
        this.burnableItem = burnableItem;
        this.min = min;
        this.max = max;
        this.chance = chance;
        this.looting = looting;
        this.requiredPermission = requiredPermission;
        this.spawnCauseFilter = spawnCauseFilter;
    }

    public double getChance(int lootBonusLevel, double lootMultiplier) {
        return chance + (lootBonusLevel * lootMultiplier);
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    public String getSpawnCauseFilter() {
        return spawnCauseFilter;
    }

    public ItemStack getItemStack(StackedEntity stackedEntity, int amountOfItems, int lootBonusLevel){
        ItemStack itemStack = LootTable.isBurning(stackedEntity) && burnableItem != null ? burnableItem.clone() : this.itemStack.clone();

        int lootingBonus = 0;

        if (looting && lootBonusLevel > 0) {
            lootingBonus = Random.nextInt(lootBonusLevel + 1);
        }

        int itemAmount = amountOfItems < 10 ? Random.nextInt(((max + lootingBonus) * amountOfItems) - ((min + lootingBonus) * amountOfItems) + 1) + ((min + lootingBonus) * amountOfItems) :
                Random.nextInt((min + lootingBonus) * amountOfItems, (max + lootingBonus) * amountOfItems);

        if(itemAmount <= 0)
            return null;

        itemStack.setAmount(itemAmount);

        return itemStack;
    }

    @Override
    public String toString() {
        return "LootItem{item=" + itemStack + ",burnable=" + burnableItem + "}";
    }

    public static LootItem fromJson(JsonObject jsonObject){
        ItemStack itemStack = buildItemStack(jsonObject), burnableItem = null;
        double chance = jsonObject.has("chance") ? jsonObject.get("chance").getAsDouble() : 100;
        int min = jsonObject.has("min") ? jsonObject.get("min").getAsInt() : 1;
        int max = jsonObject.has("max") ? jsonObject.get("max").getAsInt() : 1;
        boolean looting = jsonObject.has("looting") && jsonObject.get("looting").getAsBoolean();
        String requiredPermission = jsonObject.has("permission") ? jsonObject.get("permission").getAsString() : "";
        String spawnCauseFilter = jsonObject.has("spawn-cause") ? jsonObject.get("spawn-cause").getAsString() : "";

        if(jsonObject.has("burnable")){
            burnableItem = buildItemStack(jsonObject.get("burnable").getAsJsonObject());
        }

        return new LootItem(itemStack, burnableItem, min, max, chance, looting, requiredPermission, spawnCauseFilter);
    }

    private static ItemStack buildItemStack(JsonObject jsonObject){
        Material type = Material.valueOf(jsonObject.get("type").getAsString());
        short data = jsonObject.has("data") ? jsonObject.get("data").getAsShort() : 0;

        ItemStack itemStack = new ItemStack(type, 1, data);

        if(jsonObject.has("skull"))
            itemStack = plugin.getNMSAdapter().getPlayerSkull(jsonObject.get("skull").getAsString());

        ItemMeta itemMeta = itemStack.getItemMeta();

        if(jsonObject.has("name"))
            itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', jsonObject.get("name").getAsString()));

        if(jsonObject.has("lore")){
            JsonArray jsonArray = jsonObject.getAsJsonArray("lore");
            List<String> lore = new ArrayList<>();
            jsonArray.forEach(jsonElement -> lore.add(ChatColor.translateAlternateColorCodes('&', jsonElement.getAsString())));
            itemMeta.setLore(lore);
        }

        if(jsonObject.has("enchants")){
            JsonObject enchants = jsonObject.getAsJsonObject("enchants");
            for(Map.Entry<String, JsonElement> entry : enchants.entrySet()){
                try {
                    itemMeta.addEnchant(Enchantment.getByName(entry.getKey()), entry.getValue().getAsInt(), true);
                }catch(Exception ignored){}
            }
        }

        if(jsonObject.has("glow") && jsonObject.get("glow").getAsBoolean()){
            itemMeta.addEnchant(GlowEnchantment.getGlowEnchant(), 1, true);
        }

        itemStack.setItemMeta(itemMeta);

        if(jsonObject.has("nbt-data")){
            JsonObject nbtData = jsonObject.getAsJsonObject("nbt-data");
            for(Map.Entry<String, JsonElement> entry : nbtData.entrySet()){
                if(entry.getValue().isJsonPrimitive())
                    itemStack = plugin.getNMSAdapter().setTag(itemStack, entry.getKey(), getValue(entry.getValue().getAsJsonPrimitive()));
            }
        }

        return itemStack;
    }

    private static Object getValue(JsonPrimitive jsonPrimitive){
        try{
            Field valueField = JsonPrimitive.class.getDeclaredField("value");
            valueField.setAccessible(true);
            Object value = valueField.get(jsonPrimitive);
            valueField.setAccessible(false);
            return value;
        }catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

}
