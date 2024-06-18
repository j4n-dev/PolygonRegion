package coffee.j4n.polygonregion.util.statics;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * This class provides static methods to create ItemStacks used in the plugin.
 */
public final class ItemStacks {

    //<editor-fold desc="String constants">
    public static final String REGION_STICK_NAME = "§6Region§6§lSTICK";
    private static final String REGION_STICK_LORE_1 = "§5§nLinksklick§7: Punkt §centfernen\n";
    private static final String REGION_STICK_LORE_2 = "§5§nShift §7+ §5§nLinksklick§7: §c§nAlle§7 Punkte §centfernen\n";
    private static final String REGION_STICK_LORE_3 = "§5§nRechtsklick§7: Punkt §ahinzufügen\n";
    //</editor-fold>

    /**
     * This method returns a region stick ItemStack.<br />
     * - The region stick is a stick that allows the player to set points for a region. <br />
     * - The region stick has a custom name and lore, containing instructions on how to use it. <br />
     *
     * @return The region stick ItemStack.
     */
    public static ItemStack getRegionStick() {
        ItemStack regionStick = new ItemStack(Material.STICK);
        ItemMeta itemMeta = regionStick.getItemMeta();

        itemMeta.setDisplayName(REGION_STICK_NAME);

        itemMeta.setLore(Arrays.asList(REGION_STICK_LORE_1, REGION_STICK_LORE_2, REGION_STICK_LORE_3));

        regionStick.setItemMeta(itemMeta);

        return regionStick;
    }
}
