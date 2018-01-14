/*
 * This file is part of Dynamo.
 *
 *     Dynamo is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Dynamo is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Dynamo.  If not, see <http://www.gnu.org/licenses/>.
 */
package jab.spigot.dynamo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import com.comphenix.packetwrapper.WrapperPlayServerEntityTeleport;

import static java.lang.Math.*;

import net.minecraft.server.v1_12_R1.DataWatcher;
import net.minecraft.server.v1_12_R1.DataWatcherRegistry;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_12_R1.Vector3f;

public abstract class Dynamo {

    private static final Vector headOffset = new Vector(-0.5, -1.23, 0.5);
    private volatile Map<ArmorStand, Location> mapLocationsToSend = new HashMap<>();
    private volatile Map<ArmorStand, Vector3f> mapAnglesToSend = new HashMap<>();

    private Map<ArmorStand, Double> lastLocationX = new HashMap<>();
    private Map<ArmorStand, Double> lastLocationY = new HashMap<>();
    private Map<ArmorStand, Double> lastLocationZ = new HashMap<>();
    private Map<ArmorStand, Double> lastRotationX = new HashMap<>();
    private Map<ArmorStand, Double> lastRotationY = new HashMap<>();
    private Map<ArmorStand, Double> lastRotationZ = new HashMap<>();

    private World world;

    private Location locationAxis;
    private EulerAngle rotationAxis;

    private Dynamo parent;

    private List<Dynamo> listChildren;

    private volatile List<ArmorStand> listMatrix;
    private volatile Map<ArmorStand, Vector> mapMatrixPositions;

    private Location locationUpdated;
    private Location locationOffset;

    public Dynamo(Location locationAxis) {
        locationAxis = locationAxis.clone();
        locationAxis.setDirection(new Vector(0, 0, 0));
        locationAxis.setYaw(180);
        locationAxis.setPitch(0);
        setAxis(locationAxis);
        setWorld(locationAxis.getWorld());

        listChildren = new ArrayList<>();
        listMatrix = new ArrayList<>();
        mapMatrixPositions = new HashMap<>();
    }

    void initialize() {
        rotationAxis = new EulerAngle(0, 0, 0);
        onInitialize();
    }

    public ArmorStand addBlock(Vector vectorBlock, Material material) {
        ArmorStand stand = (ArmorStand) world.spawnEntity(getAxis().clone().add(vectorBlock), EntityType.ARMOR_STAND);
        setMatrix(stand, material, vectorBlock);
        return stand;
    }

    private void setMatrix(ArmorStand stand, Material material, Vector vectorBlock) {
        stand.setHelmet(new ItemStack(material));
        stand.setVisible(false);
        stand.setAI(false);
        stand.setGravity(false);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setVelocity(new Vector(0, 0, 0));
        stand.setSilent(true);
        synchronized (mapLocationsToSend) {
            lastLocationX.put(stand, Double.NaN);
            lastLocationY.put(stand, Double.NaN);
            lastLocationZ.put(stand, Double.NaN);
            lastRotationX.put(stand, Double.NaN);
            lastRotationY.put(stand, Double.NaN);
            lastRotationZ.put(stand, Double.NaN);
            listMatrix.add(stand);
        }

        mapMatrixPositions.put(stand, vectorBlock);
    }

    void renderSynchronized() {
        synchronized (mapLocationsToSend) {
            for (ArmorStand armorStand : listMatrix) {
                Location l = mapLocationsToSend.get(armorStand);
                if (l != null) {
                    l = l.clone();
                    armorStand.teleport(l);
                }
                Vector3f vectorHead = mapAnglesToSend.get(armorStand);
                if (vectorHead != null) {
                    armorStand.setHeadPose(new EulerAngle(toRadians(vectorHead.getX()), toRadians(vectorHead.getY()),
                            toRadians(vectorHead.getZ())));
                }
            }
        }
    }

    void update() {
        this.onUpdate();
        this.updateChildren();
    }

    void updateChildren() {
        for (Dynamo child : listChildren) {
            child.update();
        }
    }

    private List<ArmorStand> listToUpdate = new ArrayList<>();

    void render() {
        synchronized (mapLocationsToSend) {
            listToUpdate.clear();
            for (ArmorStand block : listMatrix) {
                Location locationUpdated;
                if (!hasParent()) {
                    locationUpdated = locationAxis;
                } else {
                    locationUpdated = getParent().getUpdatedLocation().clone().add(getLocation());
                }
                Vector vectorBlock = this.mapMatrixPositions.get(block).clone();
                Location axis = locationUpdated.clone();
                EulerAngle rot = new EulerAngle(getRotation().getX(), getRotation().getY(), getRotation().getZ());
                double x = vectorBlock.getX();
                double y = vectorBlock.getY();
                double z = vectorBlock.getZ();
                double ax = axis.getX();
                double ay = axis.getY();
                double az = axis.getZ();
                double rx = rot.getX();
                double ry = rot.getY();
                double rz = rot.getZ();
                Double llx = lastLocationX.containsKey(block) ? lastLocationX.get(block) : Double.NaN;
                Double lly = lastLocationY.containsKey(block) ? lastLocationY.get(block) : Double.NaN;
                Double llz = lastLocationZ.containsKey(block) ? lastLocationZ.get(block) : Double.NaN;
                Double lrx = lastRotationX.containsKey(block) ? lastRotationX.get(block) : Double.NaN;
                Double lry = lastRotationY.containsKey(block) ? lastRotationY.get(block) : Double.NaN;
                Double lrz = lastRotationZ.containsKey(block) ? lastRotationZ.get(block) : Double.NaN;
                if (x != llx || y != lly || z != llz || rx != lrx || ry != lry || rz != lrz) {
                    double[] result = rotate(x, y, z, ax, ay, az, rx, ry, rz);
                    Vector vectorResult = new Vector(result[0], result[1], result[2]);
                    double erx = -rx;
                    double ery = -ry;
                    double erz = rz;
                    Location locationResult = vectorResult.toLocation(getWorld()).add(headOffset.clone());
                    locationResult.setDirection(new Vector(0, 0, 0));
                    locationResult.setYaw(180);
                    locationResult.setPitch(0);
                    // Asynchronus maps.
                    mapAnglesToSend.put(block,
                            new Vector3f((float) toDegrees(erx), (float) toDegrees(ery), (float) toDegrees(erz)));
                    mapLocationsToSend.put(block, locationResult);
                    lastLocationX.put(block, x);
                    lastLocationY.put(block, y);
                    lastLocationZ.put(block, z);
                    lastRotationX.put(block, rx);
                    lastRotationY.put(block, ry);
                    lastRotationZ.put(block, rz);
                    listToUpdate.add(block);
                }
            }
            for (ArmorStand block : listToUpdate) {
                updatePosition(block);
            }
        }
    }

    void renderChildren() {
        for (Dynamo child : listChildren) {
            child.render();
        }
    }

    public void updatePosition(ArmorStand armorStand) {
        List<Entity> listEntities = armorStand.getNearbyEntities(100, 100, 100);
        for (Entity entity : listEntities) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                sendArmorStand(armorStand, player);
            }
        }
    }

    private void sendArmorStand(ArmorStand armorStand, Player player) {
        WrapperPlayServerEntityTeleport wrapper = new WrapperPlayServerEntityTeleport();
        Location location = this.mapLocationsToSend.get(armorStand).clone();
        wrapper.setEntityID(armorStand.getEntityId());
        wrapper.setX(location.getX());
        wrapper.setY(location.getY());
        wrapper.setZ(location.getZ());
        wrapper.setYaw(location.getYaw());
        wrapper.setPitch(location.getPitch());
        wrapper.setOnGround(false);
        wrapper.sendPacket(player);
        Vector3f angleHead = this.mapAnglesToSend.get(armorStand);
        DataWatcher dwOriginal = ((CraftArmorStand) armorStand).getHandle().getDataWatcher();
        dwOriginal.set(DataWatcherRegistry.i.a(12), angleHead);
        PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(armorStand.getEntityId(), dwOriginal,
                true);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    public void remove() {
        synchronized (mapLocationsToSend) {
            for (Dynamo child : getChildren()) {
                child.remove();
            }
            for (ArmorStand block : listMatrix) {
                block.remove();
            }
            mapLocationsToSend.clear();
            mapAnglesToSend.clear();
            mapMatrixPositions.clear();
            listChildren.clear();
            listMatrix.clear();
        }
    }

    public List<Dynamo> getChildren() {
        return this.listChildren;
    }

    /**
     * @return Returns whether or not the Dynamo has a parent Dynamo.
     */
    public boolean hasParent() {
        return this.parent != null;
    }

    /**
     * @return Returns the parent Dynamo, if the Dynamo has one.
     */
    public Dynamo getParent() {
        return this.parent;
    }

    /**
     * Sets the parent Dynamo.
     *
     * @param parent The parent Dyanmo.
     */
    public void setParent(Dynamo parent) {
        this.parent = parent;
    }

    /**
     * @return Whether or not the Dynamo has children Dynamo.
     */
    public boolean hasChildren() {
        return this.listChildren.size() > 0;
    }

    /**
     * Adds a child Dynamo to this Dynamo.
     *
     * @param child The child Dynamo.
     */
    public void addChild(Dynamo child) {
        if (!listChildren.contains(child)) {
            listChildren.add(child);
        }
    }

    /**
     * Removes a child Dynamo.
     *
     * @param child The child Dynamo.
     */
    public void removeChild(Dynamo child) {
        if (listChildren.contains(child)) {
            listChildren.remove(child);
        }
    }

    public EulerAngle getRotation() {
        return this.rotationAxis;
    }

    public void setRotation(EulerAngle rotation) {
        this.rotationAxis = rotation;
    }

    /**
     * @return Returns the Axis <Location> of the Dynamo.
     */
    public Location getAxis() {
        if (hasParent()) {
            return getParent().getAxis();
        } else {
            return this.locationAxis;
        }
    }

    /**
     * Sets the Axis <Location> of the Dynamo.
     *
     * @param location The Axis <Location> of the Dynamo.
     */
    private void setAxis(Location location) {
        this.locationAxis = location;
    }

    public World getWorld() {
        return this.world;
    }

    private void setWorld(World world) {
        this.world = world;
    }

    public Location getUpdatedLocation() {
        return this.locationUpdated;
    }

    public Location getLocation() {
        return locationOffset;
    }

    public void setLocation(Location offset) {
        this.locationOffset = offset;
    }

    public static double[] rotate(double x, double y, double z, double ax, double ay, double az, double rx, double ry,
                                  double rz) {
        if (x == ax && y == ay && z == az) {
            return new double[]{x, y, z};
        }
        double[] result = rotateX(x, y, z, ax, ay, az, rx);
        double ex = result[0];
        double ey = result[1];
        double ez = result[2];
        result = rotateY(ex, ey, ez, ax, ay, az, ry);
        ex = result[0];
        ey = result[1];
        ez = result[2];
        result = rotateZ(ex, ey, ez, ax, ay, az, rz);
        ex = result[0];
        ey = result[1];
        ez = result[2];
        return new double[]{ex + ax, ey + ay, ez + az};
    }

    public static double[] rotateX(double x, double y, double z, double ax, double ay, double az, double radians) {
        double[] pm = new double[]{x, y, z};
        double[][] rm = new double[][]{{1, 0, 0}, {0, cos(radians), -sin(radians)},
                {0, sin(radians), cos(radians)}};
        double nrx = (rm[0][0] * pm[0]) + (rm[0][1] * pm[1]) + (rm[0][2] * pm[2]);
        double nry = (rm[1][0] * pm[0]) + (rm[1][1] * pm[1]) + (rm[1][2] * pm[2]);
        double nrz = (rm[2][0] * pm[0]) + (rm[2][1] * pm[1]) + (rm[2][2] * pm[2]);
        return new double[]{nrx, nry, nrz};
    }

    public static double[] rotateY(double x, double y, double z, double ax, double ay, double az, double radians) {
        double[] pm = new double[]{x, y, z};
        double[][] rm = new double[][]{{cos(radians), 0, sin(radians)}, {0, 1, 0},
                {-sin(radians), 0, cos(radians)}};
        double nrx = (rm[0][0] * pm[0]) + (rm[0][1] * pm[1]) + (rm[0][2] * pm[2]);
        double nry = (rm[1][0] * pm[0]) + (rm[1][1] * pm[1]) + (rm[1][2] * pm[2]);
        double nrz = (rm[2][0] * pm[0]) + (rm[2][1] * pm[1]) + (rm[2][2] * pm[2]);
        return new double[]{nrx, nry, nrz};
    }

    public static double[] rotateZ(double x, double y, double z, double ax, double ay, double az, double radians) {
        double[] pm = new double[]{x, y, z};
        double[][] rm = new double[][]{{cos(radians), -sin(radians), 0}, {sin(radians), cos(radians), 0},
                {0, 0, 1}};
        double nrx = (rm[0][0] * pm[0]) + (rm[0][1] * pm[1]) + (rm[0][2] * pm[2]);
        double nry = (rm[1][0] * pm[0]) + (rm[1][1] * pm[1]) + (rm[1][2] * pm[2]);
        double nrz = (rm[2][0] * pm[0]) + (rm[2][1] * pm[1]) + (rm[2][2] * pm[2]);
        return new double[]{nrx, nry, nrz};
    }

    public abstract void onInitialize();

    public abstract void onUpdate();

    public abstract void onStopped();
}