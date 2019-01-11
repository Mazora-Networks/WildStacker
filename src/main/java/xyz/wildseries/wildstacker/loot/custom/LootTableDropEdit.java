package xyz.wildseries.wildstacker.loot.custom;

import de.Linus122.DropEdit.Main;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.wildseries.wildstacker.api.objects.StackedEntity;
import xyz.wildseries.wildstacker.loot.LootTable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LootTableDropEdit extends LootTableCustom {

    @Override
    public List<ItemStack> getDrops(LootTable lootTable, StackedEntity stackedEntity, int lootBonusLevel) {
        List<ItemStack> deathLoot = JavaPlugin.getPlugin(Main.class).getDrops(stackedEntity.getType());

        if(deathLoot == null)
            deathLoot = new ArrayList<>();

        deathLoot = deathLoot.stream().filter(itemStack -> itemStack != null && itemStack.getType() != Material.AIR)
                .collect(Collectors.toList());

        for(ItemStack itemStack : deathLoot)
            itemStack.setAmount(itemStack.getAmount() * stackedEntity.getStackAmount());

        return deathLoot;
    }

}
