package com.aionemu.gameserver.model.gameobjects.siege;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.siegespawns.SiegeSpawnTemplate;

/**
 * 要塞 NPC 游戏对象。
 * Siege Npc game object.
 *
 * @author ViAl
 */
public class SiegeNpc extends Npc {

	private int siegeId;
	private SiegeRace siegeRace;

	/**
	 * 创建要塞 NPC。
	 * Creates a siege NPC.
	 *
	 * @param objId 对象 ID / object id
	 * @param controller NPC 控制器 / NPC controller
	 * @param spawnTemplate 要塞刷新点模板 / siege spawn template
	 * @param objectTemplate NPC 模板 / NPC template
	 */
	public SiegeNpc(int objId, NpcController controller, SiegeSpawnTemplate spawnTemplate, NpcTemplate objectTemplate) {
		super(objId, controller, spawnTemplate, objectTemplate);
		this.siegeId = spawnTemplate.getSiegeId();
		this.siegeRace = spawnTemplate.getSiegeRace();
	}

	/** 获取要塞种族。 / Returns the siege race. */
	public SiegeRace getSiegeRace() {
		return siegeRace;
	}

	/** 返回攻城 ID / Returns the siege id */
	public int getSiegeId() {
		return siegeId;
	}

	/** 获取刷新点。 / Returns the spawn. */
	@Override
	public SiegeSpawnTemplate getSpawn() {
		return (SiegeSpawnTemplate) super.getSpawn();
	}

	/** 判断是否对指定生物主动攻击（敌对阵营要塞 NPC 视为主动）。 / Whether aggressive to the given creature (enemy-race siege NPCs count as aggressive). */
	public boolean isAggressiveTo(Creature creature) {
		if ((creature instanceof SiegeNpc) && getSiegeRace() != ((SiegeNpc) creature).getSiegeRace()) {
			return true;
		}
		return super.isAggressiveTo(creature);
	}
}
