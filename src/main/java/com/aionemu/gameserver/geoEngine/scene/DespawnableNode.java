/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.geoEngine.scene;

import java.util.BitSet;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.geoEngine.collision.Collidable;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.collision.IgnoreProperties;
import com.aionemu.gameserver.lifecycle.GameEventServices;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.services.SiegeService;

public class DespawnableNode extends Node {

	public DespawnableType type = DespawnableType.NONE;
	public int id;
	public byte levelBitMask;
	private final BitSet instances = new BitSet();

	public void setActive(int instanceId, boolean active) {
		synchronized (instances) {
			instances.set(instanceId, active);
		}
	}

	public boolean isActive(int instanceId) {
		synchronized (instances) {
			return instances.get(instanceId);
		}
	}

	public void copyFrom(Node node) throws CloneNotSupportedException {
		name = node.name;
		collisionFlags = node.collisionFlags;
		for (Spatial spatial : node.getChildren()) {
			if (spatial instanceof Geometry) {
				attachChild(new Geometry(spatial.getName(), ((Geometry) spatial).getMesh()));
			} else if (spatial instanceof Node) {
				attachChild(((Node) spatial).clone());
			} else {
				throw new CloneNotSupportedException();
			}
		}
	}

	@Override
	public int collideWith(Collidable other, CollisionResults results) {
		if (type == DespawnableType.EVENT) {
			if (activeEventThemeId() != id) {
				return 0;
			}
		} else if (type == DespawnableType.SHIELD) {
			IgnoreProperties ignoreProperties = results.getIgnoreProperties();
			if (ignoreProperties == IgnoreProperties.ANY_RACE) {
				return 0;
			}
			SiegeLocation loc = getSiegeLocation();
			if (loc != null) {
				if (!loc.isUnderShield()) {
					return 0;
				}
				if (ignoreProperties != null && ignoreProperties.getRace() != null) {
					if (loc.getRace() != SiegeRace.BALAUR
							&& ignoreProperties.getRace().getRaceId() == loc.getRace().getRaceId()) {
						return 0;
					}
					if (loc.getRace() == SiegeRace.BALAUR
							&& ignoreProperties.getRace() == IgnoreProperties.BALAUR.getRace()) {
						return 0;
					}
				}
			}
		} else if (type != DespawnableType.EVENT && type != DespawnableType.HOUSE && !isActive(results.getInstanceId())) {
			return 0;
		} else if (results.getIgnoreProperties() != null && results.getIgnoreProperties().getStaticId() > 0
				&& results.getIgnoreProperties().getStaticId() == id) {
			return 0;
		}
		return super.collideWith(other, results);
	}

	private int activeEventThemeId() {
		if (DataManager.EVENT_DATA == null) {
			return 0;
		}
		return GameEventServices.eventService().getEventType().getId();
	}

	private SiegeLocation getSiegeLocation() {
		try {
			return SiegeService.getInstance().getSiegeLocation(id);
		} catch (NullPointerException e) {
			return null;
		}
	}

	@Override
	public Node clone() throws CloneNotSupportedException {
		DespawnableNode node = new DespawnableNode();
		node.copyFrom(this);
		node.type = type;
		node.id = id;
		node.levelBitMask = levelBitMask;
		node.instances.or(instances);
		return node;
	}

	public void setType(DespawnableType type) {
		this.type = type;
	}

	public void setId(int id) {
		this.id = id;
	}

	public enum DespawnableType {
		NONE(0),
		EVENT(1),
		PLACEABLE(2),
		HOUSE(3),
		HOUSE_DOOR(4),
		TOWN_OBJECT(5),
		DOOR_STATE1(6),
		DOOR_STATE2(7),
		SHIELD(8);

		private final byte id;

		DespawnableType(int id) {
			this.id = (byte) id;
		}

		public static DespawnableType getById(byte id) {
			for (DespawnableType type : values()) {
				if (type.id == id) {
					return type;
				}
			}
			throw new IllegalArgumentException("Invalid despawnable type " + id);
		}

		public byte getId() {
			return id;
		}
	}
}
