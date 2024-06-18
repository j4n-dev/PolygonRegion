/**
 * Copyright (c) 2024 J4N
 * This code is licensed under GNU GPLv3 license.
 * For more information, please refer to the LICENSE file.
 */
package coffee.j4n.polygonregion.commands;

import coffee.j4n.polygonregion.PolygonRegion;
import coffee.j4n.polygonregion.util.statics.ItemStacks;
import coffee.j4n.polygonregion.util.statics.Prefixes;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents the command executor for the main command of the plugin.<br />
 * It provides the functionality to give the player a region stick, confirm a region, show a region, hide a region, fence a region and unfence a region.<br />
 * - The region stick is a stick that allows the player to set points for a region.<br />
 * - The confirm command creates a region with the points that have been set.<br />
 * - The show command shows the points of a region.<br />
 * - The hide command hides the points of a region.<br />
 * - The fence command places fences around a region.<br />
 * - The unfence command removes the fences around a region.<br />
 * - The class also provides a help message for the main command.<br />
 */
public class CmdRegion implements CommandExecutor {

    private final PolygonRegion plugin;

    public CmdRegion(PolygonRegion plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        Player player = (Player) commandSender;

        // Help message
        if (args.length == 0) {
            player.sendMessage("\n" + Prefixes.PLUGIN + "§7Hilfe zu §6PolygonRegion§7:");
            player.sendMessage("§7- §8/§bpregion §7oder §8/§bpr §8| §7Hauptbefehl des Plugins und zeigt diese Hilfe an.");
            player.sendMessage("§7- §8/§bpregion stick §8| §7Gibt dir den " + ItemStacks.REGION_STICK_NAME + "§7.");
            player.sendMessage("§7- §8/§bpregion confirm §8[§3Name§8] | §7Bestätigt die Region und legt den Namen fest.");
            player.sendMessage("§7- §8/§bpregion show §8[§3Name§8] | §7Zeigt die Markierungen der Region an.");
            player.sendMessage("§7- §8/§bpregion hide §8| §7Versteckt alle Regionsmarkierungen.");
            player.sendMessage("§7- §8/§bpregion fence §8[§3Name§8] | §7Zäunt die Region ein.");
            return false;
        }

        // Give  region stick, regardless of the number of arguments
        if (args[0].equalsIgnoreCase("stick")) {
            player.getInventory().addItem(ItemStacks.getRegionStick());
            player.sendMessage(Prefixes.INFO + "§7Du hast den " + ItemStacks.REGION_STICK_NAME + " §7erhalten.");
        }

        if (args.length == 1) {
            // Confirm region (name missing)
            if (args[0].equalsIgnoreCase("confirm")) {
                player.sendMessage(Prefixes.ERROR + "Fehlende Argumente! Es wurde §nkein Name§7 angegeben.");
                player.sendMessage(Prefixes.ADDITION + "Korrekte verwendung: §8/§bpregion confirm §8[§3§nName§8] \n");

                plugin.getPoints().isRegionValid(player);

                return false;
            }


            // Show region markers (name missing)
            if (args[0].equalsIgnoreCase("show")) {
                player.sendMessage(Prefixes.ERROR + "Fehlende Argumente! Es wurde §nkein Name§7 angegeben.");
                player.sendMessage(Prefixes.ADDITION + "Korrekte verwendung: §8/§bpregion show §8[§3§nName§8]");
            }


            // Hide region markers
            if (args[0].equalsIgnoreCase("hide")) {
                if (!plugin.getPoints().playerExist(player)) {
                    player.sendMessage(Prefixes.ERROR + "§7Dir werden aktuell §nkeine Markierungen§7 angezeigt, die du verstecken könntest.");
                } else {
                    plugin.getPoints().removeAllMarkers(player);
                    player.sendMessage(Prefixes.INFO + "§7Die Markierungen wurden §aerfolgreich §7versteckt.");
                }
            }


            // Fence region (name missing)
            if (args[0].equalsIgnoreCase("fence")) {
                player.sendMessage(Prefixes.ERROR + "Fehlende Argumente! Es wurde §nkein Name§7 angegeben.");
                player.sendMessage(Prefixes.ADDITION + "Korrekte verwendung: §8/§bpregion fence §8[§3§nName§8]");
            }
        }

        if (args.length == 2) {
            // Confirm region
            if (args[0].equalsIgnoreCase("confirm")) {

                if (!plugin.getPoints().isRegionValid(player)) {
                    return false;
                }

                plugin.getPoints().createWgRegion(player, args[1]);
            }


            // Show region markers
            if (args[0].equalsIgnoreCase("show")) {
                plugin.getPoints().loadPointsFromRegion(player, args[1]);
            }


            // Fence region
            if (args[0].equalsIgnoreCase("fence")) {
                plugin.getPoints().placeWallAroundRegion(args[1], player.getWorld(), player, Material.PURPLE_CONCRETE);
            }
        }
        return false;
    }
}
