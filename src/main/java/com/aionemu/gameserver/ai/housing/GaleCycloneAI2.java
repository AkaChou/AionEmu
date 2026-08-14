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
	// 是否已失效：死亡或消失后为 true，阻止继续对玩家施放旋风技能。 / Whether defunct: true after death/despawn, blocks further cyclone casts.
    private boolean blocked;
	// 正在观察的玩家及其移动观察器，用于在玩家移动时触发旋风技能。 / Observed players and their move observers, triggering the cyclone skill on movement.
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
	
	/**
	 * 清理所有观察器：置失效标记，并从各玩家观察控制器中移除。
	 * Clears all observers: sets the defunct flag and detaches every observer from its player.
	 */
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
