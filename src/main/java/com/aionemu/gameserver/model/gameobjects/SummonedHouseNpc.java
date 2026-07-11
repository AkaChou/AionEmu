package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * Summoned 房屋 NPC 游戏对象。
 * Summoned House Npc game object.
 */

public class SummonedHouseNpc extends SummonedObject<House> {

	String masterName;

	public SummonedHouseNpc(int objId, NpcController controller, SpawnTemplate spawnTemplate, NpcTemplate npcTemplate,
			House house, String masterName) {
		super(objId, controller, spawnTemplate, npcTemplate, npcTemplate.getLevel());
		setCreator(house);
		this.masterName = masterName;
	}

	/** 返回 creator id / Returns the creator id */
	@Override
	public int getCreatorId() {
		return getCreator().getAddress().getId();
	}

	/** 返回大师名称 / Returns the master name */
	@Override
	public String getMasterName() {
		return masterName;
	}

	/** 返回大师 / Returns the master*/
	@Override
	public Creature getMaster() {
		return null;
	}
}
