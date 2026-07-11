package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.Future;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Kisk;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.services.KiskService;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.spawnengine.VisibleObjectSpawner;

/**
 * 召唤 Kisk（复活之石）效果：生成可绑定的 Kisk 供队伍/个人使用。
 * Summon Kisk effect: spawns a bindable Kisk for party or personal use.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SummonKiskEffect")
public class SummonKiskEffect extends SummonEffect {
	/**
	 * 在玩家位置生成 Kisk，注册服务并处理绑定/对话框。
	 * Spawns a Kisk at the player, registers it, and opens bind dialog or auto-binds.
	 */
	@Override
	public void applyEffect(final Effect effect) {
		Creature effected = effect.getEffected();
		Player player = (Player) effected;
		float x = player.getX();
		float y = player.getY();
		float z = player.getZ();
		byte heading = player.getHeading();
		int worldId = player.getWorldId();
		int instanceId = player.getInstanceId();
		SpawnTemplate spawn = SpawnEngine.addNewSingleTimeSpawn(worldId, npcId, x, y, z, heading);
		final Kisk kisk = VisibleObjectSpawner.spawnKisk(spawn, instanceId, player);
		Integer objOwnerId = player.getObjectId();
		Future<?> task = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				kisk.getController().onDelete();
			}
		}, time * 1000);
		kisk.getController().addTask(TaskId.DESPAWN, task);
		player.getController().cancelTask(TaskId.ITEM_USE);
		GameFeatureServices.kiskService().regKisk(kisk, objOwnerId);
		if (kisk.getMaxMembers() > 1) {
			kisk.getController().onDialogRequest(player);
		} else {
			GameFeatureServices.kiskService().onBind(kisk, player);
		}
	}
}
