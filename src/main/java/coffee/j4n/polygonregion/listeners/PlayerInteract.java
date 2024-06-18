/**
 * Copyright (c) 2024 J4N
 * This code is licensed under GNU GPLv3 license.
 * For more information, please refer to the LICENSE file.
 */
package coffee.j4n.polygonregion.listeners;

import coffee.j4n.polygonregion.PolygonRegion;
import coffee.j4n.polygonregion.util.Marker;
import coffee.j4n.polygonregion.util.statics.ItemStacks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * This class represents the listener for player interactions.<br />
 *
 * It provides the functionality to set points for a region with a region stick.<br />
 * - The player can set points by right-clicking on a block with a region stick.<br />
 * - The player can remove points by left-clicking on a block with a region stick.<br />
 * - The player can remove all points by sneaking and left-clicking on a block with a region stick.<br />
 * - The listener also cancels the event to prevent the player from breaking blocks with the region stick.
 */
public class PlayerInteract implements Listener {

    private final PolygonRegion pl;

    public PlayerInteract(PolygonRegion pl) {
        this.pl = pl;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        Material heldItemType = heldItem.getType();

        if (heldItemType != Material.STICK || !heldItem.getItemMeta().getDisplayName().equals(ItemStacks.REGION_STICK_NAME)) {
            return;
        }

        event.setCancelled(true);
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null) {
            return;
        }

        Location blockLocation = clickedBlock.getLocation().add(0.5, 0.5, 0.5);
        Marker newMarker = new Marker(blockLocation.getX(), blockLocation.getY(), blockLocation.getZ(), clickedBlock.getType());

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Add marker
            pl.getPoints().addRegionMarker(player, newMarker, clickedBlock);

        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Remove marker
            if (player.isSneaking()) {
                // Remove all markers
                pl.getPoints().removeAllMarkers(player);
                return;
            }

            pl.getPoints().removePoint(player, newMarker, clickedBlock);
        }
    }
}
