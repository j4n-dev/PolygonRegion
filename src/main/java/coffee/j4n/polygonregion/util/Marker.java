/**
 * Copyright (c) 2024 J4N
 * This code is licensed under GNU GPLv3 license.
 * For more information, please refer to the LICENSE file.
 */
package coffee.j4n.polygonregion.util;

import org.bukkit.Material;

/**
 * This class represents a marker in the world.
 *
 * It contains the x, y, z coordinates and the original material of the block at this position.
 * - The original material is used to restore the block to its original state after the marker has been removed.
 *
 * It also provides a method to calculate the distance to another marker.
 * - The distance is calculated using the Euclidean distance formula.
 *
 * The class also provides a method to convert the marker to a string or a fancy, colored string for chat messages.
 */
public class Marker {

    /**
     * The x coordinate of the marker.
     */
    public final double x;

    /**
     * The y coordinate of the marker.
     */
    public final double y;

    /**
     * The z coordinate of the marker.
     */
    public final double z;

    /**
     * The original material of the block at this position.
     */
    private final Material originalMaterial;

    /**
     * This constructor creates a new marker with the given x, y, z coordinates and the original material of the block at this position.
     *
     * @param x The x coordinate of the marker.
     * @param y The y coordinate of the marker.
     * @param z The z coordinate of the marker.
     * @param originalMaterial The original material of the block at this position.
     */
    public Marker(double x, double y, double z, Material originalMaterial) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.originalMaterial = originalMaterial;
    }

    /**
     * This method returns the original material of the block at this position.
     *
     * @return The original material of the block at this position.
     */
    public Material getOriginalMaterial() {
        return originalMaterial;
    }

    /**
     * This method calculates the distance between this marker and another marker.
     *
     * @param markerToGetDistanceFrom The marker to calculate the distance to.
     * @return The distance between this marker and the other marker.
     */
    public double getDistanceTo(Marker markerToGetDistanceFrom) {
        double dx = this.x - markerToGetDistanceFrom.x;
        double dy = this.y - markerToGetDistanceFrom.y;
        double dz = this.z - markerToGetDistanceFrom.z;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * This method returns a fancy, colored string representation of the marker for chat messages.
     *
     * @return The fancy, colored string representation of the marker.
     */
    public String toFancyString(boolean withMaterial) {
        return "§8[§7X: §b" + x + " §8| " + "§7Y: §b" + y + " §8| " + "§7Z: §b" + z + "§8] " + (withMaterial ? "§7Material: §b" + originalMaterial : "");
    }


    @Override
    public String toString() {
        return "Point{" + "x=" + x + ", y=" + y + ", z=" + z + ", originalMaterial=" + originalMaterial + '}';
    }
}

