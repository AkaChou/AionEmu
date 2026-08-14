package com.aionemu.gameserver.spawnengine;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.walker.WalkerTemplate;

/**
 * 初始刷怪时组建巡逻队；死亡后将 NPC 带回站位。
 * Forms walker groups on initial spawn and restores NPCs after death.
 * <p>
 * 清理与重构将在测试与错误处理后进行；需配合补丁使用。
 * Cleanup and rework will follow tests and error handling; use only with the patch.
 *
 * @author vlog
 * @based on Imaginary's imagination
 * @modified Rolandas
 */
@Slf4j
public class WalkerFormator {

	/**
	 * 处理集群巡逻 NPC：首次刷怪时缓存候选并稍后编队；重生时直接归队。
	 * Handles clustered walker NPCs: caches candidates on first spawn, or re-joins on respawn.
	 * <p>
	 * 若为实例首次刷怪，会验证并创建编队，之后需调用 {@link #organizeAndSpawn(int, int)} 加速生成；
	 * 若为重生则无需编队，仅放回第一步并恢复已保存数据。
	 * On instance first spawn, verifies and creates groups; call organizeAndSpawn after.
	 * On respawn, places the NPC at the first step and restores saved data.
	 *
	 * @param npc NPC / the NPC
	 * @param worldId 世界 ID / world id
	 * @param instanceId 实例 ID / instance id
	 * @return 若本次调用已将 NPC 刷入世界则为 true / true if the npc was brought into world by this call
	 */
	public static boolean processClusteredNpc(Npc npc, int worldId, int instanceId) {
		SpawnTemplate spawn = npc.getSpawn();
		if (spawn.getWalkerId() != null) {
			InstanceWalkerFormations formations = WalkerFormationsCache.getInstanceFormations(worldId, instanceId);
			WalkerGroup wg = formations.getSpawnWalkerGroup(spawn.getWalkerId());

			if (wg != null) {
				npc.setWalkerGroup(wg);
				wg.respawn(npc);
				return false;
			}

			WalkerTemplate template = DataManager.WALKER_DATA.getWalkerTemplate(spawn.getWalkerId());
			if (template == null) {
				log.warn(I18n.get("log.dbf9e2be46c1", spawn.getWalkerId()));
				return false;
			}
			if (template.getPool() < 2) {
				return false;
			}
			return formations.cacheWalkerCandidate(new ClusteredNpc(npc, instanceId, template));
		}
		return false;
	}

	/**
	 * 组织并刷出所有已处理的巡逻编队；仅应在实例 NPC 全部生成时调用。
	 * Organizes and spawns all processed walker groups; call only when spawning all instance NPCs.
	 *
	 * @param worldId 世界 ID / world id
	 * @param instanceId 实例 ID / instance id
	 */
	public static void organizeAndSpawn(int worldId, int instanceId) {
		InstanceWalkerFormations formations = WalkerFormationsCache.getInstanceFormations(worldId, instanceId);
		formations.organizeAndSpawn();
	}

	/**
	 * 实例销毁时清理巡逻编队缓存。
	 * Clears walker formation cache when an instance is destroyed.
	 *
	 * @param worldId 世界 ID / world id
	 * @param instanceId 实例 ID / instance id
	 */
	public static void onInstanceDestroy(int worldId, int instanceId) {
		WalkerFormationsCache.onInstanceDestroy(worldId, instanceId);
	}
}
