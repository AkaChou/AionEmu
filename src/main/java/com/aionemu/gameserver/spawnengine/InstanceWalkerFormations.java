package com.aionemu.gameserver.spawnengine;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单个实例内的巡逻编队与候选 NPC 缓存。
 * Walker formations and candidate NPC cache within one instance.
 *
 * @author Rolandas
 */
@Slf4j
public class InstanceWalkerFormations {

	/**
	 * 路径 ID → 待编队的集群 NPC 候选列表。
	 * Route id to clustered NPC candidates awaiting formation.
	 */
	private Map<String, List<ClusteredNpc>> groupedSpawnObjects;

	/**
	 * 路径 ID → 已组建的巡逻队。
	 * Route id to formed walker groups.
	 */
	private Map<String, WalkerGroup> walkFormations;

	/**
	 * 创建空的实例巡逻编队容器。
	 * Creates an empty instance walker formation holder.
	 */
	public InstanceWalkerFormations() {
		groupedSpawnObjects = new HashMap<String, List<ClusteredNpc>>();
		walkFormations = new HashMap<String, WalkerGroup>();
	}

	/**
	 * 按路径 ID 获取已组建的巡逻队。
	 * Returns the formed walker group for the route id.
	 *
	 * @param walkerId 巡逻路径 ID / walker route id
	 * @return 巡逻队，可能为 null / walker group or null
	 */
	public WalkerGroup getSpawnWalkerGroup(String walkerId) {
		return walkFormations.get(walkerId);
	}

	/**
	 * 缓存一个待编队的集群巡逻 NPC 候选。
	 * Caches a clustered walker NPC candidate for later formation.
	 *
	 * @param npcWalker 集群巡逻 NPC / clustered npc
	 * @return 是否加入成功 / whether added
	 */
	protected synchronized boolean cacheWalkerCandidate(ClusteredNpc npcWalker) {
		String walkerId = npcWalker.getWalkTemplate().getRouteId();
		List<ClusteredNpc> candidateList = groupedSpawnObjects.get(walkerId);
		if (candidateList == null) {
			candidateList = new ArrayList<ClusteredNpc>();
			groupedSpawnObjects.put(walkerId, candidateList);
		}
		return candidateList.add(npcWalker);
	}

	/**
	 * 组织并刷出所有已处理的巡逻编队；仅应在实例 NPC 全部生成时调用。
	 * Organizes and spawns all processed walker groups; call only when spawning all instance NPCs.
	 */
	protected void organizeAndSpawn() {
		for (List<ClusteredNpc> candidates : groupedSpawnObjects.values()) {
			Map<Integer, List<ClusteredNpc>> byPosition = groupByPositionHash(candidates);
			int maxSize = 0;
			List<ClusteredNpc> npcs = null;
			for (List<ClusteredNpc> group : byPosition.values()) {
				if (group.size() > maxSize) {
					npcs = group;
					maxSize = npcs.size();
				}
			}
			if (maxSize == 1) {
				for (ClusteredNpc snpc : candidates) {
					snpc.spawn(snpc.getNpc().getSpawn().getZ());
				}
			} else {
				WalkerGroup wg = new WalkerGroup(npcs);
				if (candidates.get(0).getWalkTemplate().getPool() != candidates.size()) {
					log.warn(I18n.get("log.3696eb38139f", candidates.get(0).getWalkTemplate().getRouteId()));
				}
				wg.form();
				wg.spawn();
				walkFormations.put(candidates.get(0).getWalkTemplate().getRouteId(), wg);
				// 生成其余坐标不同的单位 / spawn the rest which didn't have the same coordinates
				for (ClusteredNpc snpc : candidates) {
					if (npcs.contains(snpc)) {
						continue;
					}
					snpc.spawn(snpc.getNpc().getZ());
				}
			}
		}
	}

	/**
	 * 按坐标哈希将候选 NPC 分组。
	 * Groups candidate NPCs by position hash.
	 *
	 * @param candidates 候选列表 / candidate list
	 * @return 位置哈希 → 成员列表 / position hash to members
	 */
	private Map<Integer, List<ClusteredNpc>> groupByPositionHash(List<ClusteredNpc> candidates) {
		Map<Integer, List<ClusteredNpc>> grouped = new HashMap<Integer, List<ClusteredNpc>>();
		for (ClusteredNpc candidate : candidates) {
			Integer positionHash = candidate.getPositionHash();
			List<ClusteredNpc> group = grouped.get(positionHash);
			if (group == null) {
				group = new ArrayList<ClusteredNpc>();
				grouped.put(positionHash, group);
			}
			group.add(candidate);
		}
		return grouped;
	}

	/**
	 * 实例销毁时清空候选与编队缓存。
	 * Clears candidates and formation cache when the instance is destroyed.
	 */
	protected synchronized void onInstanceDestroy() {
		groupedSpawnObjects.clear();
		walkFormations.clear();
	}
}
