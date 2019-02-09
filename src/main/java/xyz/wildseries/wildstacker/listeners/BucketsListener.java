package xyz.wildseries.wildstacker.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import xyz.wildseries.wildstacker.WildStackerPlugin;
import xyz.wildseries.wildstacker.utils.ItemUtil;

@SuppressWarnings("unused")
public final class BucketsListener implements Listener {

    private WildStackerPlugin plugin;

    public BucketsListener(WildStackerPlugin plugin){
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent e){
        ItemStack newBucketItem;

        if(e.getBlockClicked().getType().name().contains("LAVA"))
            newBucketItem = new ItemStack(Material.LAVA_BUCKET);
        else if(e.getBlockClicked().getType().name().contains("WATER"))
            newBucketItem = new ItemStack(Material.WATER_BUCKET);
        else return;

        e.setCancelled(true);

        e.getBlockClicked().setType(Material.AIR);

        if(e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack inHand = getItem(e);
                inHand.setAmount(inHand.getAmount() - 1);
                //e.getPlayer().setItemInHand(inHand);
                ItemUtil.addItem(newBucketItem, e.getPlayer().getInventory(), e.getPlayer().getLocation());
                e.getPlayer().updateInventory();
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent e){
        Material blockType;

        if (e.getBucket().name().contains("LAVA"))
            blockType = Material.LAVA;
        else if (e.getBucket().name().contains("WATER"))
            blockType = Material.WATER;
        else return;

        e.setCancelled(true);

        Block toBeReplaced = e.getBlockClicked().getRelative(e.getBlockFace());

        if(toBeReplaced.getType() != Material.AIR)
            return;

        if(e.getBlockClicked().getWorld().getEnvironment() != World.Environment.NETHER)
            toBeReplaced.setType(blockType);

        if(e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack inHand = getItem(e);
                inHand.setAmount(inHand.getAmount() - 1);
                //setItem(e, inHand.getAmount() <= 0 ? new ItemStack(Material.AIR) : inHand);
                ItemUtil.addItem(new ItemStack(Material.BUCKET), e.getPlayer().getInventory(), e.getPlayer().getLocation());
                e.getPlayer().updateInventory();
            }, 1L);
        }
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    private ItemStack getItem(PlayerBucketEvent event){
        try {
            ItemStack inHand = (ItemStack) PlayerInventory.class.getMethod("getItemInOffHand").invoke(event.getPlayer().getInventory());
            if(inHand != null) {
                if(event instanceof PlayerBucketEmptyEvent){
                    if(inHand.getType().name().contains("WATER") || inHand.getType().name().contains("LAVA"))
                        return inHand;
                }else {
                    if(inHand.getType().name().contains("BUCKET") &&
                            !(inHand.getType().name().contains("WATER") || inHand.getType().name().contains("LAVA")))
                        return inHand;
                }
            }
        }catch(Throwable ignored){}
        return event.getPlayer().getItemInHand();
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    private void setItem(PlayerBucketEmptyEvent event, ItemStack itemStack){
        try {
            ItemStack inHand = (ItemStack) PlayerInventory.class.getMethod("getItemInOffHand").invoke(event.getPlayer().getInventory());
            if(inHand != null && (inHand.getType().name().contains("WATER") || inHand.getType().name().contains("LAVA"))) {
                PlayerInventory.class.getMethod("setItemInOffHand", ItemStack.class)
                        .invoke(event.getPlayer().getInventory(), itemStack);
                return;
            }
        }catch(Throwable ignored){}
        event.getPlayer().setItemInHand(itemStack);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e){
        if(!plugin.getSettings().bucketsStackerEnabled)
            return;

        //Current Item - in slot, Cursor - holded item
        if(e.getCurrentItem() == null || (e.getCurrentItem().getType() != Material.WATER_BUCKET && e.getCurrentItem().getType() != Material.LAVA_BUCKET))
            return;

        ItemStack cursor, clicked;

        switch (e.getClick()){
            case MIDDLE:
                if(e.getWhoClicked().getGameMode() != GameMode.CREATIVE)
                    return;

                clicked = e.getCurrentItem().clone();
                cursor = clicked.clone();
                cursor.setAmount(16);
                e.getWhoClicked().getOpenInventory().setCursor(cursor);
                break;
            case RIGHT:
            case LEFT:
                if(e.getCursor() == null || (e.getCursor().getType() != Material.WATER_BUCKET && e.getCursor().getType() != Material.LAVA_BUCKET) ||
                        !e.getCursor().isSimilar(e.getCurrentItem()))
                    return;

                e.setCancelled(true);

                if(e.getCurrentItem().getAmount() >= 16)
                    return;

                int toAdd = 16 - e.getCurrentItem().getAmount();

                if(toAdd > e.getCursor().getAmount())
                    toAdd = e.getCursor().getAmount();

                if(e.getClick() == ClickType.RIGHT)
                    toAdd = 1;

                e.getCurrentItem().setAmount(e.getCurrentItem().getAmount() + toAdd);
                cursor = e.getCursor().clone();
                cursor.setAmount(cursor.getAmount() - toAdd);
                //e#setCursor is deprecated, so we can use this manipulate instead.
                e.getWhoClicked().getOpenInventory().setCursor(cursor);
                break;
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                e.setCancelled(true);

                Inventory invToAddItem = e.getWhoClicked().getOpenInventory().getTopInventory();
                if(e.getClickedInventory().equals(e.getWhoClicked().getOpenInventory().getTopInventory()))
                    invToAddItem = e.getWhoClicked().getOpenInventory().getBottomInventory();

                if(ItemUtil.stackBucket(e.getCurrentItem(), invToAddItem) || invToAddItem.addItem(e.getCurrentItem()).isEmpty()){
                    e.setCurrentItem(new ItemStack(Material.AIR));
                }
                break;
            default:
                return;
        }

        for(HumanEntity humanEntity : e.getInventory().getViewers()) {
            if (humanEntity instanceof Player)
                ((Player) humanEntity).updateInventory();
        }
    }

}
