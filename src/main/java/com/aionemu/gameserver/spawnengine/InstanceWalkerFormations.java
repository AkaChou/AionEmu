package com.aionemu.gameserver.spawnengine;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.templates.walker.WalkerTemplate;
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
	 * 同一编队刷怪点允许的最大 X/Y 偏差；采用现有队形成员间距。
	 * Maximum X/Y drift allowed within one formation; uses the existing formation member spacing.
	 */
	private static final float POSITION_GROUP_DISTANCE = WalkerGroupShift.DISTANCE;

	/**
	 * 路径 ID → 待编队的集群 NPC 候选列表。
	 * Route id to clustered NPC candidates awaiting formation.
	 */
	private final Map<String, List<ClusteredNpc>> groupedSpawnObjects;

	/**
	 * 路径 ID → 已组建的巡逻队。
	 * Route id to formed walker groups.
	 */
	private final Map<String, WalkerGroup> walkFormations;

	/**
	 * 创建空的实例巡逻编队容器。
	 * Creates an empty instance walker formation holder.
	 */
	public InstanceWalkerFormations() {
		groupedSpawnObjects = new HashMap<>();
		walkFormations = new HashMap<>();
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
			candidateList = new ArrayList<>();
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
			List<List<ClusteredNpc>> byPosition = groupCandidates(candidates);
			int maxSize = 0;
			List<ClusteredNpc> npcs = null;
			for (List<ClusteredNpc> group : byPosition) {
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
				WalkerTemplate template = candidates.get(0).getWalkTemplate();
				int formationSize = template.getFormationSize();
				List<ClusteredNpc> formationMembers = (formationSize >= 2 && npcs.size() > formationSize)
						? new ArrayList<>(npcs.subList(0, formationSize))
						: npcs;
				WalkerGroup wg = new WalkerGroup(formationMembers);
				if (template.getPool() != candidates.size()) {
					log.warn(I18n.get("log.3696eb38139f", template.getRouteId()));
				}
				wg.form();
				wg.spawn();
				walkFormations.put(template.getRouteId(), wg);
				// 生成未进入编队的其余单位 / spawn the remaining units outside the formation
				for (ClusteredNpc snpc : candidates) {
					if (formationMembers.contains(snpc)) {
						continue;
					}
					snpc.spawn(snpc.getNpc().getZ());
				}
			}
		}
	}

	/**
	 * 按路径队形容量和 X/Y 近邻关系将候选 NPC 分组。
	 * Groups candidate NPCs by route formation capacity and X/Y proximity.
	 * <p>
	 * 候选数恰好等于队形容量时视为一个完整编队；否则回退到近邻分组以处理复用路径或多余独立单位。
	 * When candidate count matches formation capacity, treat as one complete formation; otherwise group by proximity to handle reused routes or extra independent units.
	 *
	 * @param candidates 候选列表 / candidate list
	 * @return 候选坐标组 / candidate position groups
	 */
	static List<List<ClusteredNpc>> groupCandidates(List<ClusteredNpc> candidates) {
		if (candidates.isEmpty()) {
			return new ArrayList<>();
		}
		int formationSize = candidates.get(0).getWalkTemplate().getFormationSize();
		if (formationSize >= 2 && candidates.size() == formationSize) {
			List<List<ClusteredNpc>> completeFormation = new ArrayList<>();
			completeFormation.add(new ArrayList<>(candidates));
			return completeFormation;
		}

		List<List<ClusteredNpc>> grouped = new ArrayList<>();
		for (ClusteredNpc candidate : candidates) {
			List<ClusteredNpc> group = findPositionGroup(grouped, candidate);
			if (group == null) {
				group = new ArrayList<>();
				grouped.add(group);
			}
			group.add(candidate);
		}
		return grouped;
	}

	/**
	 * 查找候选 NPC 所属的近邻组；每个组只与首个坐标比较，避免远距离点位串联合并。
	 * Finds the proximity group for a candidate; compares only with the group anchor to prevent chain merging.
	 *
	 * @param grouped 已建立的近邻组 / established proximity groups
	 * @param candidate 待分组候选 / candidate to group
	 * @return 匹配的组，未找到时为 null / matching group or null
	 */
	private static List<ClusteredNpc> findPositionGroup(List<List<ClusteredNpc>> grouped, ClusteredNpc candidate) {
		for (List<ClusteredNpc> group : grouped) {
			ClusteredNpc anchor = group.get(0);
			float deltaX = candidate.getX() - anchor.getX();
			float deltaY = candidate.getY() - anchor.getY();
			if (deltaX * deltaX + deltaY * deltaY <= POSITION_GROUP_DISTANCE * POSITION_GROUP_DISTANCE) {
				return group;
			}
		}
		return null;
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
