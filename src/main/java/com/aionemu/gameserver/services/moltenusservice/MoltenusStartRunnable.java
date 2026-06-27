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
package com.aionemu.gameserver.services.moltenusservice;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Map;

import com.aionemu.gameserver.model.moltenus.MoltenusLocation;
import com.aionemu.gameserver.services.MoltenusService;

/**
 * @author Rinzler (Encom)
 */

public class MoltenusStartRunnable implements Runnable {
	private final int id;

	public MoltenusStartRunnable(int id) {
		this.id = id;
	}

	@Override
	public void run() {
		// Enraged Sulfur Guardian will appear in 10 minutes.
		GameLocationBootstrapServices.moltenusService().sulfurFortressMsg(id);
		// Enraged Western Guardian will appear in 10 minutes.
		GameLocationBootstrapServices.moltenusService().westernFortressMsg(id);
		// Enraged Eastern Guardian will appear in 10 minutes.
		GameLocationBootstrapServices.moltenusService().easternFortressMsg(id);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				Map<Integer, MoltenusLocation> locations = GameLocationBootstrapServices.moltenusService().getMoltenusLocations();
				for (final MoltenusLocation loc : locations.values()) {
					if (loc.getId() == id) {
						GameLocationBootstrapServices.moltenusService().startMoltenus(loc.getId());
					}
				}
			}
		}, 600000);
	}
}