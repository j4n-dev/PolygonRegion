/**
 * Copyright (c) 2024 J4N
 * This code is licensed under GNU GPLv3 license.
 * For more information, please refer to the LICENSE file.
 */
package coffee.j4n.polygonregion;

import coffee.j4n.polygonregion.util.Marker;
import coffee.j4n.polygonregion.util.statics.ItemStacks;
import coffee.j4n.polygonregion.util.statics.Prefixes;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This class is responsible for managing the region markers. <br />
 * - It allows players to add and remove points to create a polygonal region.<br />
 * - It also provides methods to create and remove fences around a region.<br />
 */
public class RegionMarker {

    private final Map<Player, List<Marker>> playerRegionMarkers = new HashMap<>();
    private final PolygonRegion pl;

    public RegionMarker(PolygonRegion pl) {
        this.pl = pl;
    }

    //<editor-fold desc="Add region markers">

    /**
     * Add a region marker to a player at the clicked block.<br />
     * <p>
     * - The marker is inserted at the correct position based on proximity to the line segments between the existing markers.<br />
     * - The clicked block is set to a sea lantern and the player is notified about the added point.<br />
     * - If at least 3 points are present, the region markers are updated.<br />
     *
     * @param player         The player
     * @param pointToBeAdded The marker to be added
     * @param clickedBlock   The block that was clicked
     */
    public void addRegionMarker(Player player, Marker pointToBeAdded, Block clickedBlock) {
        List<Marker> points = playerRegionMarkers.computeIfAbsent(player, k -> new ArrayList<>());

        if (points.contains(pointToBeAdded)) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);
            player.sendMessage(Prefixes.PLUGIN + "Dieser Punkt ist bereits gesetzt.");
            return;
        }

        // Find the correct position to insert the new point based on proximity
        int insertIndex = findInsertIndex(points, pointToBeAdded);
        points.add(insertIndex, pointToBeAdded);
        playerRegionMarkers.put(player, points);

        clickedBlock.setType(Material.SEA_LANTERN);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        player.getWorld().playEffect(clickedBlock.getLocation().add(0, 1, 0), Effect.BONE_MEAL_USE, 1);
        player.sendMessage(Prefixes.PLUGIN + "§3" + points.size() + ". §7Punkt (Marker §3" + (insertIndex + 1) + "§7) §ahinzugefügt§7:");
        player.sendMessage(Prefixes.ADDITION + pointToBeAdded.toFancyString(true));

        // Update region markers if at least 3 points are present
        if (points.size() >= 3) {
            updateRegionMarkers(player);
        }
    }

    /**
     * Find the index to insert a new marker based on proximity to the line segments between the existing markers.
     *
     * @param points           the list of existing markers
     * @param comparisonMarker the new marker to be inserted
     * @return the index to insert the new marker
     */
    private int findInsertIndex(@NotNull List<Marker> points, Marker comparisonMarker) {

        // First marker = first index
        if (points.isEmpty()) {
            return 0;
        }

        double minDistance = Double.MAX_VALUE;
        // Default: insert every new marker at the end
        int insertIndex = points.size();

        for (int i = 0; i < points.size(); i++) {
            Marker marker1 = points.get(i);
            Marker marker2 = points.get((i + 1) % points.size());

            // Markers should not be equal, so skip if they are
            if (marker1.equals(marker2)) {
                continue;
            }

            double distance = distancePointToSegment(marker1, marker2, comparisonMarker);

            if (distance < minDistance) {
                minDistance = distance;
                insertIndex = i + 1;
            }
        }

        return insertIndex;
    }

    /**
     * Calculate the distance between a point and a line segment
     *
     * @param marker1          the first marker of the line segment
     * @param marker2          the second marker of the line segment
     * @param comparisonMarker the marker to calculate the distance to
     * @return the distance between the marker and the line segment
     */
    private double distancePointToSegment(@NotNull Marker marker1, @NotNull Marker marker2, Marker comparisonMarker) {
        // Delta between the x coordinates and z coordinates of the two markers
        double xCoordDeltaM1M2 = marker2.x - marker1.x;
        double zCoordDeltaM1M2 = marker2.z - marker1.z;

        if ((xCoordDeltaM1M2 == 0) && (zCoordDeltaM1M2 == 0)) {
            throw new IllegalArgumentException("marker1 and marker2 cannot be the same");
        }

        // Calculate the difference in x and z coordinates between comparisonMarker and marker1
        double xCoordDeltaCMM1 = comparisonMarker.x - marker1.x;
        double zCoordDeltaCMM1 = comparisonMarker.z - marker1.z;

        // Calculate the dot product of the coordinate differences and the coordinate deltas
        double dotProduct = xCoordDeltaCMM1 * xCoordDeltaM1M2 + zCoordDeltaCMM1 * zCoordDeltaM1M2;
        // Calculate the squared length of the vector described by the coordinate deltas
        double lengthSquared = xCoordDeltaM1M2 * xCoordDeltaM1M2 + zCoordDeltaM1M2 * zCoordDeltaM1M2;

        // Compute the factor that determines the closest point on the line segment to the comparisonMarker
        double closestPointFactor = dotProduct / lengthSquared;

        final Marker closestPoint;

        // Determine the closest point based on the closestPointFactor
        if (closestPointFactor < 0) {
            // Closest point is marker1
            closestPoint = marker1;
        } else if (closestPointFactor > 1) {
            // Closest point is marker2
            closestPoint = marker2;
        } else {
            // Closest point is on the line segment between marker1 and marker2
            double closestPointX = marker1.x + closestPointFactor * xCoordDeltaM1M2;
            double closestPointZ = marker1.z + closestPointFactor * zCoordDeltaM1M2;
            closestPoint = new Marker(closestPointX, marker1.y, closestPointZ, marker1.getOriginalMaterial());
        }

        return comparisonMarker.getDistanceTo(closestPoint);
    }
    //</editor-fold>


    //<editor-fold desc="Remove region markers">

    /**
     * Remove a region marker from a player at the clicked block.<br />
     * <p>
     * - The marker is removed based on the coordinates of the clicked block.<br />
     * - The clicked block is set to its original material and the player is notified about the removed point.<br />
     * - If at least 3 points are present, the region markers are updated, otherwise they are cleared.<br />
     *
     * @param player           The player
     * @param pointToBeRemoved The marker to be removed
     * @param clickedBlock     The block that was clicked
     */
    public void removePoint(Player player, Marker pointToBeRemoved, Block clickedBlock) {
        if (playerRegionMarkers.get(player) == null || playerRegionMarkers.get(player).isEmpty()) {
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_DEATH, 1, 1);
            player.getWorld().playEffect(clickedBlock.getLocation().add(0, 1, 0), Effect.SMOKE, 1);
            player.sendMessage(Prefixes.PLUGIN + "Es sind §nkeine Marker§7 gesetzt, die du entfernen könntest.");
            return;
        }

        // find marker by coordinates
        Marker coordPoint = playerRegionMarkers.get(player).stream().filter(p ->
                p.x == pointToBeRemoved.x &&
                        p.y == pointToBeRemoved.y &&
                        p.z == pointToBeRemoved.z).findFirst().orElse(null);



        if (coordPoint == null || !playerRegionMarkers.get(player).contains(coordPoint)) {
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_DEATH, 1, 1);
            player.getWorld().playEffect(clickedBlock.getLocation().add(0, 1, 0), Effect.SMOKE, 1);
            player.sendMessage(Prefixes.PLUGIN + "Das ist §nkein Marker§7.");
            return;
        }

        int markerIndex = playerRegionMarkers.get(player).indexOf(coordPoint);
        playerRegionMarkers.get(player).remove(coordPoint);

        clickedBlock.setType(coordPoint.getOriginalMaterial());

        player.getWorld().playEffect(clickedBlock.getLocation().add(0, 1, 0), Effect.COPPER_WAX_ON, 1);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1, 1);

        player.sendMessage(Prefixes.PLUGIN + "§3" + (playerRegionMarkers.get(player).size() + 1) + ". §7Punkt (Marker §3" + (markerIndex + 1) + "§7) §centfernt§7:");
        player.sendMessage(Prefixes.ADDITION + pointToBeRemoved.toFancyString(false));

        if (playerRegionMarkers.get(player).size() >= 3) {
            updateRegionMarkers(player);
        } else {
            // clear region markers (we cant create a polygon with less than 3 points)
            clearRegionMarkers(player);
        }
    }

    /**
     * Remove all region markers from a player.<br />
     * - All markers are removed from the world and the player is notified about the removed points.<br />
     * - The region markers are cleared and removed from the playerRegionMarkers map.<br />
     * - The player is notified about the successful removal of all points.<br />
     *
     * @param player The player
     */
    public void removeAllMarkers(Player player) {

        for (Marker currentMarker : playerRegionMarkers.get(player)) {
            // Subtract 0.5 from the x and z coordinates to get the block at the correct position
            // --> We add 0.5 to the coordinates when creating location of the markers in the PlayerInteract event
            Block block = player.getWorld().getBlockAt((int) (currentMarker.x - 0.5), (int) currentMarker.y, (int) (currentMarker.z - 0.5));
            block.setType(currentMarker.getOriginalMaterial());
        }

        clearRegionMarkers(player);

        playerRegionMarkers.remove(player);

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);

        player.sendMessage(Prefixes.PLUGIN + "§c§nAlle§7 Punkte wurden §centfernt§7.");
    }
    //</editor-fold>


    //<editor-fold desc="Display entities (Markers)">

    /**
     * Updates the region markers for a player by creating display entities between the markers.
     *
     * @param player The player
     */
    private void updateRegionMarkers(Player player) {
        List<Marker> playerMarkers = playerRegionMarkers.get(player);

        clearRegionMarkers(player);

        for (int markerIndex = 0; markerIndex < playerMarkers.size() - 1; markerIndex++) {
            createDisplayEntitiesBetweenPointsBresenham(player, playerMarkers.get(markerIndex), playerMarkers.get(markerIndex + 1));
        }

        createDisplayEntitiesBetweenPointsBresenham(player, playerMarkers.getLast(), playerMarkers.getFirst());
    }

    /**
     * removes all entities with the tag "pr_polygon_marker" from the world of a player
     *
     * @param player the player
     */
    public void clearRegionMarkers(@NotNull Player player) {
        player.getWorld().getEntities().stream().filter(entity ->
                entity.getScoreboardTags().contains("pr_polygon_marker") ||
                        entity.getScoreboardTags().contains("pr_polygon_connector") ||
                        entity.getScoreboardTags().contains("pr_polygon_marker_text")).forEach(Entity::remove);
    }


    /**
     * Creates the lines (display entities) between two markers
     * - The lines are created by spawning entities with the tag "pr_polygon_connector" and the material RED_CONCRETE
     * - The "support" for the lines is created by spawning entities with the tag "pr_polygon_marker" and the material YELLOW_CONCRETE
     * - Every "support" displays the number of the marker as an armor stand
     *
     * @param player  The player
     * @param markerA The first marker
     * @param markerB The second marker
     */
    private void createDisplayEntitiesBetweenPoints(@NotNull Player player, @NotNull Marker markerA, @NotNull Marker markerB) {
        // Copy the markers to prevent changes to the original markers
        //Marker markerA = marker1;
        //Marker markerB = marker2;

        // Create locations for the markers, 5 blocks above the ground
        Location locationMarkerA = new Location(player.getWorld(), markerA.x - 0.5, markerA.y + 5.0, markerA.z - 0.5);
        Location locationMarkerB = new Location(player.getWorld(), markerB.x - 0.5, markerB.y + 5.0, markerB.z - 0.5);

        // Create indexes for the markers
        int index1 = playerRegionMarkers.get(player).indexOf(markerA) + 1;
        int index2 = playerRegionMarkers.get(player).indexOf(markerB) + 1;

        //org.bukkit.entity.Marker marker1 = player.getWorld().spawn(location1, org.bukkit.entity.Marker.class, entity -> {
        //    entity.addScoreboardTag("pr_polygon_marker");
        //});
        //org.bukkit.entity.Marker marker2 = player.getWorld().spawn(location2, org.bukkit.entity.Marker.class, entity -> {
        //    entity.addScoreboardTag("pr_polygon_marker");
        //});

        // Create text entities for the marker indexes as armor stands above the markers
        Location textLocation1 = locationMarkerA.clone().add(0.5, 1, 0.5);
        player.getWorld().spawn(textLocation1, ArmorStand.class, entity -> {
            entity.setCustomName(String.valueOf(index1));
            entity.setCustomNameVisible(true);
            entity.setInvisible(true);
            entity.setMarker(true);
            entity.addScoreboardTag("pr_polygon_marker_text");
        });

        Location textLocation2 = locationMarkerB.clone().add(0.5, 1, 0.5);
        player.getWorld().spawn(textLocation2, ArmorStand.class, entity -> {
            entity.setCustomName(String.valueOf(index2));
            entity.setCustomNameVisible(true);
            entity.setInvisible(true);
            entity.setMarker(true);
            entity.addScoreboardTag("pr_polygon_marker_text");
        });

        // Create "support" entities for the lines
        createVerticalSupport(player, markerA, locationMarkerA);
        createVerticalSupport(player, markerB, locationMarkerB);

        // Calculate the distance between the two markers
        double xDeltaLocMBMA = locationMarkerB.getX() - locationMarkerA.getX();
        double yDeltaLocMBMA = locationMarkerB.getY() - locationMarkerA.getY();
        double zDeltaLocMBMA = locationMarkerB.getZ() - locationMarkerA.getZ();
        double totalDistance = Math.sqrt(xDeltaLocMBMA * xDeltaLocMBMA + yDeltaLocMBMA * yDeltaLocMBMA + zDeltaLocMBMA * zDeltaLocMBMA);

        // Number of segments (Red concrete blocks) to create (1 block per block)
        int numSegments = (int) (totalDistance);

        double stepX = xDeltaLocMBMA / numSegments;
        double stepY = yDeltaLocMBMA / numSegments;
        double stepZ = zDeltaLocMBMA / numSegments;

        for (int i = 0; i < numSegments; i++) {
            double segmentX = locationMarkerA.getX() + stepX * i + 0.5;
            double segmentY = locationMarkerA.getY() + stepY * i + 0.5;
            double segmentZ = locationMarkerA.getZ() + stepZ * i + 0.5;

            spawnRegionBoundaryConnector(player, segmentX, segmentY, segmentZ);
        }
    }

    /**
     * Creates the vertical support for the lines between two markers using the Bresenham's Algorithm
     *
     * @param player  The player
     * @param markerA The first marker
     * @param markerB The second marker
     */
    private void createDisplayEntitiesBetweenPointsBresenham(@NotNull Player player, @NotNull Marker markerA, @NotNull Marker markerB) {
        // Create locations for the markers, 5 blocks above the ground
        Location locationMarkerA = new Location(player.getWorld(), markerA.x - 0.5, markerA.y + 5.0, markerA.z - 0.5);
        Location locationMarkerB = new Location(player.getWorld(), markerB.x - 0.5, markerB.y + 5.0, markerB.z - 0.5);

        // Create indexes for the markers
        int index1 = playerRegionMarkers.get(player).indexOf(markerA) + 1;
        int index2 = playerRegionMarkers.get(player).indexOf(markerB) + 1;

        // Create text entities for the marker indexes as armor stands above the markers
        Location textLocation1 = locationMarkerA.clone().add(0.5, 1, 0.5);
        player.getWorld().spawn(textLocation1, ArmorStand.class, entity -> {
            entity.setCustomName(String.valueOf(index1));
            entity.setCustomNameVisible(true);
            entity.setInvisible(true);
            entity.setMarker(true);
            entity.addScoreboardTag("pr_polygon_marker_text");
        });

        Location textLocation2 = locationMarkerB.clone().add(0.5, 1, 0.5);
        player.getWorld().spawn(textLocation2, ArmorStand.class, entity -> {
            entity.setCustomName(String.valueOf(index2));
            entity.setCustomNameVisible(true);
            entity.setInvisible(true);
            entity.setMarker(true);
            entity.addScoreboardTag("pr_polygon_marker_text");
        });

        // Create "support" entities for the lines
        createVerticalSupport(player, markerA, locationMarkerA);
        createVerticalSupport(player, markerB, locationMarkerB);

        // Bresenham's Algorithm to draw the line
        int xMarker1 = locationMarkerA.getBlockX();
        int yMarker1 = locationMarkerA.getBlockY();
        int zMarker1 = locationMarkerA.getBlockZ();

        int xMarker2 = locationMarkerB.getBlockX();
        int yMarker2 = locationMarkerB.getBlockY();
        int zMarker2 = locationMarkerB.getBlockZ();

        // Calculate the differences in each coordinate
        int xDeltaM2M1 = Math.abs(xMarker2 - xMarker1);
        int yDeltaM2M1 = Math.abs(yMarker2 - yMarker1);
        int zDeltaM2M1 = Math.abs(zMarker2 - zMarker1);

        // Determine the direction of movement in each axis
        int stepX = xMarker1 < xMarker2 ? 1 : -1;
        int stepY = yMarker1 < yMarker2 ? 1 : -1;
        int stepZ = zMarker1 < zMarker2 ? 1 : -1;

        // Identify the dominant direction
        if (xDeltaM2M1 >= yDeltaM2M1 && xDeltaM2M1 >= zDeltaM2M1) {
            // Initialize error terms for YZ and XZ planes
            int errorYZ = 2 * yDeltaM2M1 - xDeltaM2M1;
            int errorXZ = 2 * zDeltaM2M1 - xDeltaM2M1;

            while (xMarker1 != xMarker2) {
                xMarker1 += stepX;

                if (errorYZ >= 0) {
                    yMarker1 += stepY;
                    errorYZ -= 2 * xDeltaM2M1;
                }

                if (errorXZ >= 0) {
                    zMarker1 += stepZ;
                    errorXZ -= 2 * xDeltaM2M1;
                }

                errorYZ += 2 * yDeltaM2M1;
                errorXZ += 2 * zDeltaM2M1;

                // Spawn the connector entity at the current position
                spawnRegionBoundaryConnector(player, xMarker1, yMarker1, zMarker1);
            }
        } else if (yDeltaM2M1 >= xDeltaM2M1 && yDeltaM2M1 >= zDeltaM2M1) {
            int errorXY = 2 * xDeltaM2M1 - yDeltaM2M1;
            int errorYZ = 2 * zDeltaM2M1 - yDeltaM2M1;

            while (yMarker1 != yMarker2) {
                yMarker1 += stepY;

                if (errorXY >= 0) {
                    xMarker1 += stepX;
                    errorXY -= 2 * yDeltaM2M1;
                }

                if (errorYZ >= 0) {
                    zMarker1 += stepZ;
                    errorYZ -= 2 * yDeltaM2M1;
                }

                errorXY += 2 * xDeltaM2M1;
                errorYZ += 2 * zDeltaM2M1;

                spawnRegionBoundaryConnector(player, xMarker1, yMarker1, zMarker1);
            }
        } else {
            int errorXY = 2 * yDeltaM2M1 - zDeltaM2M1;
            int errorXZ = 2 * xDeltaM2M1 - zDeltaM2M1;

            while (zMarker1 != zMarker2) {
                zMarker1 += stepZ;

                if (errorXY >= 0) {
                    yMarker1 += stepY;
                    errorXY -= 2 * zDeltaM2M1;
                }

                if (errorXZ >= 0) {
                    xMarker1 += stepX;
                    errorXZ -= 2 * zDeltaM2M1;
                }

                errorXY += 2 * yDeltaM2M1;
                errorXZ += 2 * xDeltaM2M1;

                spawnRegionBoundaryConnector(player, xMarker1, yMarker1, zMarker1);
            }
        }
    }

    /**
     * Helper method to spawn a region boundary connector entity at a given location
     * - The entity is spawned with the tag "pr_polygon_connector" and the material RED_CONCRETE
     * - The entity is scaled to 1/3 of its original size
     * - The entity is spawned at the given location
     *
     * @param player The player
     * @param x      The x coordinate
     * @param y      The y coordinate
     * @param z      The z coordinate
     */
    private void spawnRegionBoundaryConnector(@NotNull Player player, double x, double y, double z) {
        Location segmentLocation = new Location(player.getWorld(), x + 0.5, y + 1.125, z + 0.5);
        player.getWorld().spawn(segmentLocation, ItemDisplay.class, entity -> {
            entity.setItemStack(new ItemStack(Material.RED_CONCRETE));

            entity.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),                 // Translation
                    new Quaternionf(),                              // Left rotation
                    new Vector3f(0.333f, 0.333f, 0.333f),  // Scale
                    new Quaternionf()                               // Right rotation
            ));

            entity.addScoreboardTag("pr_polygon_connector");
        });
    }


    /**
     * Creates the vertical support for the lines between two markers
     * - The support is created by spawning entities with the tag "pr_polygon_connector" and the material YELLOW_CONCRETE and BLACK_CONCRETE
     * - The support is created in 3 segments to prevent entity clipping
     *
     * @param player   The player
     * @param point    The marker
     * @param location The location of the marker
     */
    private void createVerticalSupport(Player player, @NotNull Marker point, Location location) {
        for (int y = (int) point.y + 1; y <= point.y + 5; y++) {
            Location segmentLocation = new Location(player.getWorld(), point.x, y + 0.125, point.z);
            for (int i = 0; i < 3; i++) {

                // skip last iteration to prevent entity clipping
                if (y == point.y + 5 && i == 1) {
                    continue;
                }

                // i = odd -> BLACK_CONCRETE, i = even -> YELLOW_CONCRETE
                final Material materialToUse = ((i % 2) == 0) ? Material.YELLOW_CONCRETE : Material.BLACK_CONCRETE;

                Location displayLocation = segmentLocation.clone().add(0, i * (1.0 / 3.0), 0);

                ItemDisplay itemDisplay = player.getWorld().spawn(displayLocation, ItemDisplay.class, entity -> {
                    entity.setItemStack(new ItemStack(materialToUse));
                    entity.setTransformation(new Transformation(
                            new Vector3f(0, 0, 0),                  // Translation
                            new Quaternionf(),                               // Left rotation
                            new Vector3f(0.333f, 0.333f, 0.333f),   // Scale
                            new Quaternionf()                                // Right rotation
                    ));

                    entity.addScoreboardTag("pr_polygon_connector");
                });
            }
        }
    }
    // </editor-fold>


    // <editor-fold desc="WorldGuard regions">

    /**
     * Create a WorldGuard region from the markers of a player
     * - The region is created with the name provided by the player
     * - The region is created with the flags set to deny block break, chest access, block place, vehicle destruction, fire spread, mob damage and TNT
     * - The region is added to the WorldGuard region manager
     *
     * @param player       The player
     * @param wgRegionName The name of the WorldGuard region
     */
    public void createWgRegion(Player player, String wgRegionName) {
        List<Marker> playerMarkers = playerRegionMarkers.get(player);

        LocalPlayer wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

        RegionContainer wgRegionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager wgRegionInWgWorld = wgRegionContainer.get(wgPlayer.getWorld());

        if (wgRegionInWgWorld == null) {
            player.sendMessage(Prefixes.ERROR + "Fehler beim Zugriff auf das Region-Management.");
            player.sendMessage(Prefixes.ADDITION + "Versuche es gerne noch einmal, oder kontaktiere ein Serverteammitglied.");
            return;
        }

        List<BlockVector2> wgPoints = new ArrayList<>();

        for (Marker playerMarker : playerMarkers) {
            wgPoints.add(BlockVector2.at(playerMarker.x, playerMarker.z));
        }

        // Regex to find invalid characters
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9]");
        Matcher matcher = pattern.matcher(wgRegionName);

        StringBuilder invalidChars = new StringBuilder();

        while (matcher.find()) {
            invalidChars.append(matcher.group()).append(" ");
        }

        if (invalidChars.length() > 0) {
            player.sendMessage(Prefixes.ERROR + "Der Regionsname enthält ungültige Zeichen: " + invalidChars.toString());
            return;
        }

        ProtectedPolygonalRegion wgRegion = new ProtectedPolygonalRegion(wgRegionName, wgPoints, -64, 320);

        // set flags
        wgRegion.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
        wgRegion.setFlag(Flags.CHEST_ACCESS, StateFlag.State.DENY);
        wgRegion.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
        wgRegion.setFlag(Flags.DESTROY_VEHICLE, StateFlag.State.DENY);
        wgRegion.setFlag(Flags.FIRE_SPREAD, StateFlag.State.DENY);
        wgRegion.setFlag(Flags.MOB_DAMAGE, StateFlag.State.DENY);
        wgRegion.setFlag(Flags.TNT, StateFlag.State.DENY);

        wgRegionInWgWorld.addRegion(wgRegion);

        player.sendMessage(Prefixes.PLUGIN + "Die Region \"§2" + wgRegionName + "§7\" wurde §aerfolgreich §7erstellt.");
        player.sendMessage(Prefixes.ADDITION + "Die Region ist nun §ageschützt §7und kann von anderen Spielern §nnicht§7 verändert werden.\n");
        player.sendMessage(Prefixes.ADDITION + "Sie besitzt standardmäßig folgende WorldGuard §nFlags§7:");

        // Display the flags of the region
        wgRegion.getFlags().keySet().stream().map(flag -> Prefixes.ADDITION + "§8- §7" + flag.getName() + "§8: §3" + wgRegion.getFlag(flag)).forEach(player::sendMessage);

        player.sendMessage("\n" + Prefixes.INFO + "Die Region kann optional über WorldGuard verwaltet werden.");
    }


    /**
     * Load the points of a WorldGuard region into the region markers of a player
     * - The points are loaded from the WorldGuard region manager
     * - The points are displayed to the player
     *
     * @param player       The player
     * @param wgRegionName The name of the WorldGuard region
     */
    public void loadPointsFromRegion(Player player, String wgRegionName) {
        LocalPlayer wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

        RegionContainer wgRegionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager wgRegionInWgWorld = wgRegionContainer.get(wgPlayer.getWorld());

        if (wgRegionInWgWorld == null) {
            player.sendMessage(Prefixes.ERROR + "Fehler beim Zugriff auf das Region-Management.");
            player.sendMessage(Prefixes.ADDITION + "Versuche es gerne noch einmal, oder kontaktiere ein Serverteammitglied.");
            return;
        }

        ProtectedPolygonalRegion wgPolygonalRegion = (ProtectedPolygonalRegion) wgRegionInWgWorld.getRegion(wgRegionName);

        if (wgPolygonalRegion == null) {
            player.sendMessage(Prefixes.ERROR + "Die Region \"§2§n" + wgRegionName + "§7\" wurde §nnicht gefunden§7.");
            return;
        }

        player.sendMessage(Prefixes.INFO + "Lade Punkte der Region \"§2" + wgRegionName + "§7\"...");

        List<Marker> points = new ArrayList<>();
        for (BlockVector2 wgPoint : wgPolygonalRegion.getPoints()) {
            Location location = new Location(player.getWorld(), wgPoint.x(), player.getWorld().getHighestBlockYAt(wgPoint.x(), wgPoint.z()), wgPoint.z());
            Block block = location.getBlock();

            points.add(new Marker(location.getX() - 0.5, location.getY() - 0.5, location.getZ() - 0.5, block.getType()));
        }

        playerRegionMarkers.put(player, points);
        player.sendMessage(Prefixes.INFO + "§3" + points.size() + " Markierungen §7wurden gefunden!");
        updateRegionMarkers(player);
        player.sendMessage(Prefixes.INFO + "Die Markierungen der Region \"§2" + wgRegionName + "§7\" werden nun angezeigt.");
    }
    //</editor-fold>


    //<editor-fold desc="Region walls">

    /**
     * Place a wall around a polygonal WorldGuard region
     * - The wall is created by spawning falling fences around the region
     * - The wall is created by connecting the points of the region with straight lines
     * - The wall is created with a random height between 5 and 20 blocks
     *
     * @param wgRegionName
     * @param world
     * @param player
     * @param wallMaterial
     */
    public void placeWallAroundRegion(String wgRegionName, World world, Player player, Material wallMaterial) {
        LocalPlayer wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

        RegionContainer wgRegionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager wgRegionInWgWorld = wgRegionContainer.get(wgPlayer.getWorld());

        if (wgRegionInWgWorld == null) {
            player.sendMessage(Prefixes.ERROR + "Fehler beim Zugriff auf das Region-Management.");
            player.sendMessage(Prefixes.ADDITION + "Versuche es gerne noch einmal, oder kontaktiere ein Serverteammitglied.");
            return;
        }

        ProtectedPolygonalRegion wgPolygonalRegion = (ProtectedPolygonalRegion) wgRegionInWgWorld.getRegion(wgRegionName);

        if (wgPolygonalRegion == null) {
            player.sendMessage(Prefixes.ERROR + "Die Region \"§2§n" + wgRegionName + "\"§7 konnte §nnicht gefunden§7 werden.");
            return;
        }

        List<BlockVector2> wgRegionPoints = wgPolygonalRegion.getPoints();
        Set<Location> wallLocations = new HashSet<>();

        // Generate the wall locations by connecting the points of the region with straight lines
        for (int i = 0; i < wgRegionPoints.size(); i++) {
            BlockVector2 point1 = wgRegionPoints.get(i);
            BlockVector2 point2 = wgRegionPoints.get((i + 1) % wgRegionPoints.size());
            addWallLocations(world, wallLocations, point1, point2, wgPolygonalRegion.getMinimumPoint().y(), wgPolygonalRegion.getMaximumPoint().y());
        }

        // Spawn the wall using the locations and falling block entities (to be able to adapt the wall to the terrain)
        for (Location location : wallLocations) {
            spawnFallingFence(location, wallMaterial);
        }
    }

    /**
     * Add the locations of a wall around a polygonal region to a set of locations using the Bresenham's line algorithm
     *
     * @param world              The world
     * @param wallBlockLocations The set of wall block locations
     * @param wgPoint1           The first point of the wall
     * @param wgPoint2           The second point of the wall
     * @param minY               The minimum y coordinate of the region
     * @param maxY               The maximum y coordinate of the region
     */
    private void addWallLocations(World world, Set<Location> wallBlockLocations, @NotNull BlockVector2 wgPoint1, @NotNull BlockVector2 wgPoint2, int minY, int maxY) {
        // Calculate the absolute delta between the x and z coordinates of wgPoint1 and wgPoint2
        int xDeltaP2P1 = Math.abs(wgPoint2.x() - wgPoint1.x());
        int zDeltaP2P1 = Math.abs(wgPoint2.z() - wgPoint1.z());

        // Initialize the current x and z coordinates to the x and z coordinates of wgPoint1
        int currentXP1 = wgPoint1.x();
        int currentZP1 = wgPoint1.z();

        // Calculate the number of steps to reach wgPoint2 from wgPoint1
        int stepsRemaining = 1 + xDeltaP2P1 + zDeltaP2P1;

        // Determine the direction of the x and z coordinates from wgPoint1 to wgPoint2
        int xDirection = (wgPoint2.x() > wgPoint1.x()) ? 1 : -1;
        int zDirection = (wgPoint2.z() > wgPoint1.z()) ? 1 : -1;

        // Initialize the error term based on the delta in the x and z coordinates
        int errorTerm = xDeltaP2P1 - zDeltaP2P1;

        // Double the deltas for use in the algorithm
        xDeltaP2P1 *= 2;
        zDeltaP2P1 *= 2;

        for (; stepsRemaining > 0; --stepsRemaining) {
            Location highestBlock = getHighestBlock(world, currentXP1, currentZP1, minY, maxY);

            wallBlockLocations.add(highestBlock);

            // Update the error term and current coordinates based on the error term
            if (errorTerm > 0) {
                currentXP1 += xDirection;
                errorTerm -= zDeltaP2P1;
            } else {
                currentZP1 += zDirection;
                errorTerm += xDeltaP2P1;
            }
        }
    }

    /**
     * Get the highest block at a given x and z coordinate in a world
     *
     * @param world The world
     * @param x     The x coordinate
     * @param z     The z coordinate
     * @param minY  The minimum y coordinate
     * @param maxY  The maximum y coordinate
     * @return The highest block at the given x and z coordinate
     */
    private @NotNull Location getHighestBlock(World world, int x, int z, int minY, int maxY) {
        for (int y = maxY; y >= minY; y--) {
            Location loc = new Location(world, x, y, z);
            if (!loc.getBlock().getType().isAir()) {
                return loc.add(0, 1, 0);
            }
        }
        return new Location(world, x, minY, z);
    }

    private void spawnFallingFence(@NotNull Location location, @NotNull Material wallMaterial) {
        int randomHeight = (int) (Math.random() * 16) + 5;
        Location spawnLocation = location.clone().add(0, randomHeight, 0);

        FallingBlock fallingWallBlock = location.getWorld().spawnFallingBlock(spawnLocation, wallMaterial.createBlockData());
        fallingWallBlock.setDropItem(false);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (fallingWallBlock.isOnGround()) {
                    fallingWallBlock.remove();
                    location.getBlock().setType(wallMaterial);
                    this.cancel();
                }
            }
        }.runTaskTimer(pl, 0L, 1L);
    }
    //</editor-fold>


    //<editor-fold desc="Region validation">

    /**
     * Returns whether a player has set points
     *
     * @param player the player
     * @return whether the player has set points
     */
    public boolean playerExist(Player player) {
        return playerRegionMarkers.containsKey(player);
    }

    /**
     * Returns whether a player has set enough points
     *
     * @param player the player
     * @return whether the player has set enough points
     */
    private boolean playerHasEnoughPoints(Player player) {
        return playerRegionMarkers.get(player).size() >= 3;
    }

    /**
     * Returns whether the region set by a player is valid
     * - If the player has not set any points, the player is notified about the missing points
     * - If the region is not valid, the player is notified about the missing points
     *
     * @param player the player
     * @return whether the region set by the player is valid
     */
    public boolean isRegionValid(Player player) {
        if (!playerExist(player)) {
            player.sendMessage(Prefixes.ADDITION + "Du hast noch §nkeine Punkte§7 gesetzt!");
            player.sendMessage(Prefixes.ADDITION + "Benutze den " + ItemStacks.REGION_STICK_NAME + "§7 --> §8/§bPRegion Stick");

            return false;

        } else if (!playerHasEnoughPoints(player)) {
            int missingPoints = getMinPoints() - getPointCount(player);

            player.sendMessage(Prefixes.ADDITION + "Du hast erst §c" + getPointCount(player) + "§8/§a§n" + getMinPoints() + "§7 gesetzt!");
            player.sendMessage(Prefixes.ADDITION + "Für eine gültige Region benötigst du noch mindestens §c§n" + missingPoints + "§7 Punkte.");
            player.sendMessage(Prefixes.ADDITION + "Benutze den " + ItemStacks.REGION_STICK_NAME + "§7 --> §8/§bPRegion Stick");

            return false;
        }

        return true;
    }

    /**
     * Returns the number of points set by a player
     *
     * @param player the player
     * @return the number of points set by the player
     */
    private int getPointCount(Player player) {
        return playerRegionMarkers.get(player).size();
    }

    /**
     * Returns the minimum number of points required to create a region
     *
     * @return the minimum number of points required to create a region
     */
    private int getMinPoints() {
        return 3;
    }
    // </editor-fold>
}