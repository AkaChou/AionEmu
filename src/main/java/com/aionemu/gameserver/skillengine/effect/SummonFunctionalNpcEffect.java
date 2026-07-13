package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.spawnengine.VisibleObjectSpawner;

/**
 * 召唤功能 NPC 效果：生成功能性 NPC（如商人等），超时后删除。
 * Summon functional NPC effect: spawns a utility NPC and deletes it after a timeout.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SummonFunctionalNpcEffect")
public class SummonFunctionalNpcEffect extends SummonEffect {
	@XmlAttribute(name = "owner")
	private SummonOwner owner;

	/**
	 * 生成功能 NPC，并按模板时限删除。
	 * Spawns the functional NPC and schedules deletion after the template lifetime.
	 */
	@Override
	public void applyEffect(Effect effect) {
		Player effected = (Player) effect.getEffected();
		final Npc functionalNpc = VisibleObjectSpawner.spawnFunctionalNpc(effected, npcId, owner);

		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (functionalNpc != null && functionalNpc.isSpawned()) {
					functionalNpc.getController().onDelete();
				}
			}
		}, (time > 0 ? time : 300) * 1000L);
	}
}
