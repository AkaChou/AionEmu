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
package com.aionemu.gameserver.services.rvrservice;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Map;

import com.aionemu.gameserver.model.rvr.RvrLocation;
import com.aionemu.gameserver.services.RvrService;

/**
 * @author Rinzler (Encom)
 */

public class RvrStartRunnable implements Runnable {
	private final int id;

	public RvrStartRunnable(int id) {
		this.id = id;
	}

	@Override
	public void run() {
		// Invasion Portal.
		GameLocationBootstrapServices.rvrService().adventPortalSP(id);
		// An Elyos warship will invade in 10 minutes.
		GameLocationBootstrapServices.rvrService().DF6G1Spawn01Msg(id);
		// An Asmodian warship will invade in 10 minutes.
		GameLocationBootstrapServices.rvrService().LF6G1Spawn01Msg(id);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				// Invasion Lazer.
				GameLocationBootstrapServices.rvrService().adventDirectingSP(id);
			}
		}, 180000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				// Invasion Black Sky.
				GameLocationBootstrapServices.rvrService().adventControlSP(id);
				// An Elyos Warship will invade in 5 minutes.
				GameLocationBootstrapServices.rvrService().DF6G1Spawn02Msg(id);
				// An Asmodian Warship will invade in 5 minutes.
				GameLocationBootstrapServices.rvrService().LF6G1Spawn02Msg(id);
				// Intrusion was detected.
				GameLocationBootstrapServices.rvrService().F6RaidStart5Minute(id);
			}
		}, 300000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				// An Elyos warship will invade in 3 minutes.
				GameLocationBootstrapServices.rvrService().DF6G1Spawn03Msg(id);
				// An Asmodian warship will invade in 3 minutes.
				GameLocationBootstrapServices.rvrService().LF6G1Spawn03Msg(id);
			}
		}, 480000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				// An Elyos warship will invade in 1 minute.
				GameLocationBootstrapServices.rvrService().DF6G1Spawn04Msg(id);
				// An Asmodian warship will invade in 1 minute.
				GameLocationBootstrapServices.rvrService().LF6G1Spawn04Msg(id);
			}
		}, 540000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				Map<Integer, RvrLocation> locations = GameLocationBootstrapServices.rvrService().getRvrLocations();
				for (final RvrLocation loc : locations.values()) {
					if (loc.getId() == id) {
						// Invasion Light Blue.
						GameLocationBootstrapServices.rvrService().adventEffectSP(id);
						// Elyos Warship Invasion.
						GameLocationBootstrapServices.rvrService().DF6G1Spawn05Msg(id);
						// Asmodian Warship Invasion.
						GameLocationBootstrapServices.rvrService().LF6G1Spawn05Msg(id);
						// Ancient's Weapon Invasion.
						GameLocationBootstrapServices.rvrService().F6RaidStart(id);
						// Brigade General's Urgent Order.
						GameLocationBootstrapServices.rvrService().startRvr(loc.getId());
						// The Asmodian Troopers are retreating after the defeat of their officers.
						GameLocationBootstrapServices.rvrService().LF6EventG2Start02Msg(id);
						// The Aetos are retreating after the defeat of their officers.
						GameLocationBootstrapServices.rvrService().DF6EventG2Start02Msg(id);
					}
				}
			}
		}, 600000);
	}
}