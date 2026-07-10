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
package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

public class GeoDataConfig {

	/**
	 * Geodata enable
	 */
	@Property(key = "gameserver.geodata.enable", defaultValue = "false")
	public static boolean GEO_ENABLE;

	/**
	 * Enable canSee checks using geodata.
	 */
	@Property(key = "gameserver.geodata.cansee.enable", defaultValue = "true")
	public static boolean CANSEE_ENABLE;

	/**
	 * Enable Fear skill using geodata.
	 */
	@Property(key = "gameserver.geodata.fear.enable", defaultValue = "true")
	public static boolean FEAR_ENABLE;

	/**
	 * Enable Geo checks during npc movement (prevent flying mobs)
	 */
	@Property(key = "gameserver.geo.npc.move", defaultValue = "false")
	public static boolean GEO_NPC_MOVE;

	/**
	 * Enable geo materials using skills
	 */
	@Property(key = "gameserver.geo.materials.enable", defaultValue = "false")
	public static boolean GEO_MATERIALS_ENABLE;

	/**
	 * Enable geo shields
	 */
	@Property(key = "gameserver.geo.shields.enable", defaultValue = "false")
	public static boolean GEO_SHIELDS_ENABLE;

	@Property(key = "gameserver.geo.nav.pathfinding.enable", defaultValue = "false")
	public static boolean GEO_NAV_ENABLE;

	@Property(key = "gameserver.geo.nav.cache.size", defaultValue = "50")
	public static int GEO_NAV_CACHE_SIZE;

	@Property(key = "gameserver.geo.nav.pull.enable", defaultValue = "true")
	public static boolean GEO_NAV_PULL_ENABLE;

	@Property(key = "gameserver.geo.nav.max.nodes", defaultValue = "800")
	public static int GEO_NAV_MAX_NODES;

	@Property(key = "gameserver.geo.nav.target.threshold", defaultValue = "5")
	public static float GEO_NAV_TARGET_THRESHOLD;

	@Property(key = "gameserver.geo.nav.path.weight", defaultValue = "0.2")
	public static float GEO_NAV_PATH_WEIGHT;

	@Property(key = "gameserver.geo.nav.target.weight", defaultValue = "20")
	public static float GEO_NAV_TARGET_WEIGHT;

	@Property(key = "gameserver.geo.nav.ground.search.distance", defaultValue = "5")
	public static float GEO_NAV_GROUND_SEARCH_DISTANCE;

	@Property(key = "gameserver.geo.nav.box.extent.xy", defaultValue = "0.8")
	public static float GEO_NAV_BOX_EXTENT_XY;

	@Property(key = "gameserver.geo.nav.box.offset.z.min", defaultValue = "-1")
	public static float GEO_NAV_BOX_OFFSET_Z_MIN;

	@Property(key = "gameserver.geo.nav.box.offset.z.max", defaultValue = "4")
	public static float GEO_NAV_BOX_OFFSET_Z_MAX;

	@Property(key = "gameserver.geo.nav.box.center.z", defaultValue = "0.2")
	public static float GEO_NAV_BOX_CENTER_Z;

	@Property(key = "gameserver.geo.nav.smooth.path", defaultValue = "true")
	public static boolean GEO_NAV_SMOOTH_PATH;

	@Property(key = "gameserver.geo.nav.corridor.length", defaultValue = "800")
	public static int GEO_NAV_CORRIDOR_LENGTH;

}
