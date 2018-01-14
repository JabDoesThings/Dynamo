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

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class DynamoDebug1 extends Dynamo {

	private double offsetRX = 0;
	private double offsetRY = 0;
	private double offsetRZ = 0;

	public DynamoDebug1(Location locationAxis) {
		super(locationAxis);
	}

	@Override
	public void onInitialize() {
		Material material = Material.REDSTONE_BLOCK;
		for (int z = -10; z <= 10; z++) {
			for (int x = -10; x <= 10; x++) {
				addBlock(new Vector(x, 0, z), material);
			}
		}
		setRotation(new EulerAngle(0, 0, 0));
	}

	@Override
	public void onUpdate() {
		double radians = Math.toRadians(1);
		offsetRX += radians;
		offsetRY += radians;
		offsetRZ += radians;
		setRotation(new EulerAngle(offsetRX, offsetRY, offsetRZ));
	}

	@Override
	public void onStopped() {

	}
}