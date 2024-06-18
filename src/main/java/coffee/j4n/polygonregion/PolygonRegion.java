/**
 * Copyright (c) 2024 J4N
 * This code is licensed under GNU GPLv3 license.
 * For more information, please refer to the LICENSE file.
 */
package coffee.j4n.polygonregion;

import coffee.j4n.polygonregion.commands.CmdRegion;
import coffee.j4n.polygonregion.listeners.PlayerInteract;
import org.bukkit.plugin.java.JavaPlugin;

public class PolygonRegion extends JavaPlugin {

    private RegionMarker regionMarker;

    @Override
    public void onEnable() {
        getLogger().info("Enabling PolygonRegion...");

        regionMarker =  new RegionMarker(this);

        // register listener
        getServer().getPluginManager().registerEvents(new PlayerInteract(this), this);

        // register command
        getCommand("pregion").setExecutor(new CmdRegion(this));

        getLogger().info("PolygonRegion enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling PolygonRegion");

        getLogger().info("PolygonRegion disabled!");
    }

    public RegionMarker getPoints() {
        return this.regionMarker;
    }
}