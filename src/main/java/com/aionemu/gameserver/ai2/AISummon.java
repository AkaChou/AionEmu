package com.aionemu.gameserver.ai2;

import com.aionemu.gameserver.controllers.SummonController;
import com.aionemu.gameserver.controllers.movement.SummonMoveController;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 召唤物默认 AI，提供召唤物相关的便捷访问方法。
 * Default summon AI providing convenience accessors for summon-related data.
 */
@AIName("summon")
public class AISummon extends AITemplate {

	/**
	 * 获取召唤物所有者。
	 * Returns the summon owner.
	 *
	 * @return 召唤物 / summon
	 */
	@Override
	public Summon getOwner() {
		return (Summon) super.getOwner();
	}

	/**
	 * 获取 NPC 模板。
	 * Returns the NPC object template.
	 *
	 * @return NPC 模板 / NPC template
	 */
	protected NpcTemplate getObjectTemplate() {
		return getOwner().getObjectTemplate();
	}

	/**
	 * 获取刷新模板。
	 * Returns the spawn template.
	 *
	 * @return 刷新模板 / spawn template
	 */
	protected SpawnTemplate getSpawnTemplate() {
		return getOwner().getSpawn();
	}

	/**
	 * 获取种族。
	 * Returns the race.
	 *
	 * @return 阵营 / race
	 */
	protected Race getRace() {
		return getOwner().getRace();
	}

	/**
	 * 获取召唤物主人（玩家）。
	 * Returns the summon's master player.
	 *
	 * @return 召唤者玩家 / master player
	 */
	protected Player getMaster() {
		return getOwner().getMaster();
	}

	/**
	 * 获取移动控制器。
	 * Returns the move controller.
	 *
	 * @return 移动控制器 / move controller
	 */
	protected SummonMoveController getMoveController() {
		return getOwner().getMoveController();
	}

	/**
	 * 获取召唤物控制器。
	 * Returns the summon controller.
	 *
	 * @return 召唤物控制器 / summon controller
	 */
	protected SummonController getController() {
		return getOwner().getController();
	}
}
