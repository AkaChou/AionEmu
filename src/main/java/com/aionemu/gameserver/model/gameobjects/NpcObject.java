package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.HousingNpc;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.spawnengine.SpawnEngine;

/**
 * NPC 对象。
 * Npc Object game object.
 */

public class NpcObject extends HouseObject<HousingNpc> {

	Npc npc = null;

	public NpcObject(House owner, int objId, int templateId) {
		super(owner, objId, templateId);
	}

	/** 使用时 / On use. */
	@Override
	public void onUse(Player player) {
	}

	/** 生成。 / Spawn. */
	public synchronized void spawn() {
		super.spawn();
		if (npc == null) {
			HousingNpc template = (HousingNpc) getObjectTemplate();
			SpawnTemplate spawn = SpawnEngine.addNewSingleTimeSpawn(getOwnerHouse().getWorldId(), template.getNpcId(),
					getX(), getY(), getZ(), getHeading());
			npc = ((Npc) SpawnEngine.spawnObject(spawn, getOwnerHouse().getInstanceId()));
		}
	}

	/** 消失时 / On despawn. */
	@Override
	public synchronized void onDespawn() {
		super.onDespawn();
		if (npc != null) {
			npc.getController().onDelete();
			npc = null;
		}
	}

	/** 是否立即过期 / Whether expire now */
	@Override
	public synchronized boolean canExpireNow() {
		if (npc == null) {
			return true;
		}
		return npc.getTarget() == null;
	}

	/** 返回 NPC 对象 ID / Returns the npc object id */
	public int getNpcObjectId() {
		return npc == null ? 0 : npc.getObjectId();
	}
}
