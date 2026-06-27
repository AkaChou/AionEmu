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
package com.aionemu.gameserver.services.beritraservice;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Map;

import com.aionemu.gameserver.model.beritra.BeritraLocation;
import com.aionemu.gameserver.services.BeritraService;

/**
 * @author Rinzler (Encom)
 */

public class BeritraStartRunnable implements Runnable {
	private final int id;

	public BeritraStartRunnable(int id) {
		this.id = id;
	}

	@Override
	public void run() {
		// Beritra Invasion Portal.
		GameLocationBootstrapServices.beritraService().adventPortalSP(id);
		// Ereshkigal Invasion Portal.
		GameLocationBootstrapServices.beritraService().adventPortalEreshSP(id);
		// The Beritra Legion's Invasion Corridor has appeared.
		GameLocationBootstrapServices.beritraService().invasionCorridorMsg(id);
		// The Ereshkigal Legion's Invasion Corridor has been created.
		GameLocationBootstrapServices.beritraService().ereshkigalCorridorMsg(id);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				// Beritra Invasion Lazer.
				GameLocationBootstrapServices.beritraService().adventDirectingSP(id);
				// Ereshkigal Invasion Lazer.
				GameLocationBootstrapServices.beritraService().adventDirectingEreshSP(id);
				// The Devil Unit has infiltrated through the Invasion Corridor.
				GameLocationBootstrapServices.beritraService().devilUnitThroughMsg(id);
				// The Ereshkigal Legion's Magic weapon has infiltrated through the Invasion
				// Corridor.
				GameLocationBootstrapServices.beritraService().ereshkigalLegionThroughMsg(id);
			}
		}, 180000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				// Beritra Invasion Black Sky.
				GameLocationBootstrapServices.beritraService().adventControlSP(id);
				// Ereshkigal Invasion Black Sky.
				GameLocationBootstrapServices.beritraService().adventControlEreshSP(id);
			}
		}, 300000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				Map<Integer, BeritraLocation> locations = GameLocationBootstrapServices.beritraService().getBeritraLocations();
				for (final BeritraLocation loc : locations.values()) {
					if (loc.getId() == id) {
						// Beritra Invasion Light Blue.
						GameLocationBootstrapServices.beritraService().adventEffectSP(id);
						// Ereshkigal Invasion Light Blue.
						GameLocationBootstrapServices.beritraService().adventEffectEreshSP(id);
						// Beritra Invasion Start 4.7
						GameLocationBootstrapServices.beritraService().beritraInvasionMsg(id);
						// Ereshkigal Invasion Start 4.9.1
						GameLocationBootstrapServices.beritraService().ereshkigalInvasionMsg(id);
						// Dredgion Defense.
						GameLocationBootstrapServices.beritraService().dredgionDefenseMsg(id);
						GameLocationBootstrapServices.beritraService().startBeritraInvasion(loc.getId());
					}
				}
			}
		}, 600000);
	}
}