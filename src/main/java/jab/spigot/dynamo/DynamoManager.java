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

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class DynamoManager {

    public volatile boolean updating = true;

    private JavaPlugin plugin;

    public Map<String, Dynamo> mapDynamos;

    private volatile boolean paused = false;

    public DynamoManager(JavaPlugin plugin) {
        this.plugin = plugin;
        mapDynamos = new HashMap<>();
    }

    public void start() {
        try {
            updating = true;
            paused = false;
            runnableUpdatesAsync.runTaskAsynchronously(plugin);
            runnableUpdatesSync.runTaskTimer(plugin, 0L, 200L);
        } catch (IllegalStateException e) {

        }
    }

    public void stop() {
        updating = false;
    }

    public void clear() {
        synchronized (mapDynamos) {
            for (Dynamo dynamo : mapDynamos.values()) {
                dynamo.remove();
            }
            mapDynamos.clear();
        }
    }

    public void createDebugDynamo(Player player) {
        Dynamo dynamo = new DynamoDebug1(player.getLocation());
        dynamo.initialize();
        mapDynamos.put("debug", dynamo);
    }

    public void update() {
        if (!paused) {
            synchronized (mapDynamos) {
                for (Dynamo dynamoNext : mapDynamos.values()) {
                    dynamoNext.update();
                    dynamoNext.render();
                }
            }
        }
    }

    private BukkitRunnable runnableUpdatesSync = new BukkitRunnable() {
        public void run() {
            if (!updating) {
                cancel();
                return;
            }
            synchronized (mapDynamos) {
                for (Dynamo dynamoNext : mapDynamos.values()) {
                    dynamoNext.renderSynchronized();
                }
            }
        }
    };

    private BukkitRunnable runnableUpdatesAsync = new BukkitRunnable() {
        public void run() {
            while (updating) {
                update();
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public void pause() {
        paused = true;
        (new BukkitRunnable() {
            public void run() {
                synchronized (mapDynamos) {
                    for (Dynamo dynamoNext : mapDynamos.values()) {
                        dynamoNext.renderSynchronized();
                    }
                }
            }
        }).runTaskLater(plugin, 20L);
    }

    public void resume() {
        paused = false;
    }

    public void addDynamo(String name, Dynamo dynamo) {
        dynamo.initialize();
        synchronized (mapDynamos) {
            mapDynamos.put(name, dynamo);
        }
    }
}
