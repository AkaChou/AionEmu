/*
 * This file is part of Encom.
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
package com.aionemu.gameserver.ai.housing;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.controllers.observer.GaleCycloneObserver;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/****/
/** Author (Encom)
/****/

@AIName("galecyclone")
public class GaleCycloneAI2 extends NpcAI2
{
    private boolean blocked;
	private Map<Integer, GaleCycloneObserver> observed = new ConcurrentHashMap<Integer, GaleCycloneObserver>();
    
	@Override
	protected void handleCreatureSee(Creature creature) {
		if (blocked) {
			return;
		} if (creature instanceof Player) {
			final Player player = (Player) creature;
			final GaleCycloneObserver observer = new GaleCycloneObserver(player, getOwner()) {
				@Override
				public void onMove() {
					if (!blocked) {
						GameEngineServices.skillEngine().getSkill(getOwner(), 20528, 50, player).useNoAnimationSkill();
					}
				}
			};
			player.getObserveController().addObserver(observer);
			observed.put(player.getObjectId(), observer);
		}
	}
	
	@Override
	protected void handleCreatureNotSee(Creature creature) {
		if (blocked) {
			return;
		} if (creature instanceof Player) {
			Player player = (Player) creature;
			Integer obj = player.getObjectId();
			GaleCycloneObserver observer = observed.remove(obj);
			if (observer != null) {
				player.getObserveController().removeObserver(observer);
			}
		}
	}
	
	@Override
	protected void handleDied() {
		clear();
		super.handleDied();
	}
	
	@Override
	protected void handleDespawned() {
		clear();
		super.handleDespawned();
	}
	
	private void clear() {
		blocked = true;
		for (Iterator<Map.Entry<Integer, GaleCycloneObserver>> iterator = observed.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<Integer, GaleCycloneObserver> entry = iterator.next();
			Player player = getKnownList().getKnownPlayers().get(entry.getKey());
			GaleCycloneObserver observer = entry.getValue();
			iterator.remove();
			if (player != null) {
				player.getObserveController().removeObserver(observer);
			}
		}
	}
}
