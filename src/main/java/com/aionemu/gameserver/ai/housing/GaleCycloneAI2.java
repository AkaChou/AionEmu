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

/**
 * 房屋相关 NPC AI：Gale Cyclone（@AIName "galecyclone"），继承 NpcAI2。
 * Housing-related NPC AI: Gale Cyclone (@AIName "galecyclone"), extends NpcAI2.
 *
 * @author Encom
 */
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
