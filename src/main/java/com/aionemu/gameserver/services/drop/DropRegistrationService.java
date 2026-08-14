package com.aionemu.gameserver.services.drop;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameEventServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.configs.main.DropConfig;
import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.GlobalDropData;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.Drop;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.drop.DropModifiers;
import com.aionemu.gameserver.model.drop.NpcDrop;
import com.aionemu.gameserver.model.gameobjects.DropNpc;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemId;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.team2.common.legacy.LootGroupRules;
import com.aionemu.gameserver.model.templates.event.EventDrop;
import com.aionemu.gameserver.model.templates.event.EventTemplate;
import com.aionemu.gameserver.model.templates.globaldrops.GlobalDropItem;
import com.aionemu.gameserver.model.templates.globaldrops.GlobalRule;
import com.aionemu.gameserver.model.templates.housing.HouseType;
import com.aionemu.gameserver.model.templates.npc.NpcRating;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.pet.PetFunctionType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LOOT_STATUS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LOOT_STATUS.Status;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MINIONS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PET;
import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.services.toypet.MinionService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.DropRewardEnum;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 掉落注册服务，在 NPC 死亡后生成并登记掉落物。
 * Drop registration service that builds and registers loot after NPC death.
 */
@Slf4j
public class DropRegistrationService {
	private static volatile ObjectProvider<DropRegistrationService> instanceProvider;

	private ConcurrentMap<Integer, Set<DropItem>> currentDropMap = new ConcurrentHashMap<Integer, Set<DropItem>>();
	private ConcurrentMap<Integer, DropNpc> dropRegistrationMap = new ConcurrentHashMap<Integer, DropNpc>();
	private volatile Set<Integer> noReductionMaps = Set.of();

	/**
	 * 使用玩家等级注册 NPC 掉落。
	 * Registers NPC drops using the player's level.
	 *
	 * @param npc 死亡的 NPC / dead NPC
	 * @param player 主要拾取玩家 / primary looter
	 * @param groupMembers 队伍成员 / group members
	 */
	public void registerDrop(Npc npc, Player player, Collection<Player> groupMembers) {
		registerDrop(npc, player, player.getLevel(), groupMembers);
	}

	/**
	 * 构造并初始化掉落注册服务。
	 * Constructs and initializes the drop registration service.
	 */
	public DropRegistrationService() {
		init();
		reload();
	}

	public void reload() {
		Set<Integer> maps = new HashSet<>();
		String configuredMaps = DropConfig.DISABLE_DROP_REDUCTION_IN_ZONES;
		if (configuredMaps != null) {
			for (String mapId : configuredMaps.split(",")) {
				if (!mapId.isBlank() && !mapId.trim().equals("0")) {
					maps.add(Integer.parseInt(mapId.trim()));
				}
			}
		}
		noReductionMaps = Set.copyOf(maps);
	}

	/**
	 * 初始化服务（掉落数据按需由 NpcTemplate 加载）。
	 * Initializes the service (drops are loaded on demand by NpcTemplate).
	 */
	public final void init() {
		// 掉落按需由 NpcTemplate#getNpcDrop() 加载。 / Drops are loaded on demand by NpcTemplate#getNpcDrop().
	}

	/**
	 * 在 NPC 死亡后注册全部掉落（NPC 表、任务、活动与全局掉落）。
	 * Registers full drops after NPC death (NPC table, quest, event, and global drops).
	 *
	 * @param npc 死亡的 NPC / dead NPC
	 * @param player 主要拾取玩家 / primary looter
	 * @param highestLevel 队伍最高等级 / highest group level
	 * @param groupMembers 队伍成员 / group members
	 */
	public void registerDrop(Npc npc, Player player, int highestLevel, Collection<Player> groupMembers) {

		if (player == null) {
			return;
		}
		int npcObjId = npc.getObjectId();

		// 获取该 NPC 全部可能掉落 / Getting all possible drops for this Npc
		NpcDrop npcDrop = npc.getNpcDrop();
		List<Player> dropPlayers = new ArrayList<>();
		Player teamLooter = initDropNpc(player, npcObjId, dropPlayers, groupMembers);
		Player genesis = teamLooter == null ? player : teamLooter;
		int winnerObj = teamLooter == null ? 0 : teamLooter.getObjectId();

		Set<DropItem> droppedItems = new HashSet<>();
		int index = 1;
		DropModifiers dropModifiers = createDropModifiers(npc, genesis, highestLevel);
		float dropRate = dropModifiers.calculateDropChance(1f, true);
		if (npcDrop != null) {
			index = npcDrop.dropCalculator(droppedItems, index, dropModifiers, groupMembers);
		}
		currentDropMap.put(npcObjId, droppedItems);
		index = QuestService.getQuestDrop(droppedItems, index, npc, groupMembers, genesis);
		if (EventsConfig.ENABLE_EVENT_SERVICE) {
			List<EventTemplate> activeEvents = GameEventServices.eventService().getActiveEvents();
			for (EventTemplate eventTemplate : activeEvents) {
				if (eventTemplate.EventDrop() == null) {
					continue;
				}
				List<EventDrop> eventDrops = eventTemplate.EventDrop().getEventDrops();
				for (EventDrop eventDrop : eventDrops) {
					int diff = npc.getLevel() - eventDrop.getItemTemplate().getLevel();
					int minDiff = eventDrop.getMinDiff();
					int maxDiff = eventDrop.getMaxDiff();
					if (minDiff != 0) {
						if (diff < eventDrop.getMinDiff()) {
							continue;
						}
					}
					if (maxDiff != 0) {
						if (diff > eventDrop.getMaxDiff()) {
							continue;
						}
					}
					float percent = eventDrop.getChance();
					percent *= dropRate;
					if (Rnd.get() * 100 > percent) {
						continue;
					}
					droppedItems.add(regDropItem(index++, winnerObj, npcObjId, eventDrop.getItemId(), eventDrop.getCount()));
				}
			}
		}
		if (DropConfig.ENABLE_GLOBAL_DROPS) {
			index = registerGlobalDrops(npc, player, winnerObj, droppedItems, index, dropModifiers);
		}
		if (npc.getPosition().isInstanceMap()) {
			npc.getPosition().getWorldMapInstance().getInstanceHandler().onDropRegistered(npc);
		}
		npc.getAi2().onGeneralEvent(AIEventType.DROP_REGISTERED);
		for (Player p : dropPlayers) {
			PacketSendUtility.sendPacket(p, new SM_LOOT_STATUS(npcObjId, Status.LOOT_ENABLE));
		}
		if (player.getPet() != null && player.getPet().getPetTemplate().getPetFunction(PetFunctionType.LOOT) != null && player.getPet().getCommonData().isLooting()) {
			PacketSendUtility.sendPacket(player, new SM_PET(true, npcObjId));
			Set<DropItem> drops = getCurrentDropMap().get(npcObjId);
			if (drops == null || drops.size() == 0) {
				npc.getController().onDelete();
			} else {
				DropItem[] dropItems = drops.toArray(new DropItem[0]);
				for (int i = 0; i < dropItems.length; i++) {
					GameCoreGameplayServices.dropService().requestDropItem(player, npcObjId, dropItems[i].getIndex(), true);
				}
			}
			PacketSendUtility.sendPacket(player, new SM_PET(false, npcObjId));
			if (drops == null || drops.size() == 0) {
				return;
			}
		}
		if (player.getMinion() != null && player.getMinion().getCommonData().isLooting()
				&& !MinionService.rejectIfMinionFunctionExpired(player)) {
			PacketSendUtility.sendPacket(player, new SM_MINIONS(8, 1, npcObjId, true));
			Set<DropItem> drops = getCurrentDropMap().get(npcObjId);
			if (drops == null || drops.size() == 0) {
				npc.getController().onDelete();
			} else {
				DropItem[] dropItems = drops.toArray(new DropItem[drops.size()]);
				for (int i = 0; i < dropItems.length; i++) {
					GameCoreGameplayServices.dropService().requestDropItem(player, npcObjId, dropItems[i].getIndex(), true);
				}
			}
			PacketSendUtility.sendPacket(player, new SM_MINIONS(8, 1, npcObjId, false));
			if (drops == null || drops.size() == 0) {
				return;
			}
		}
		GameCoreGameplayServices.dropService().scheduleFreeForAll(npcObjId);
	}

	/**
	 * 根据 NPC 与玩家状态构建掉落修正参数。
	 * Builds drop modifiers from NPC and player state.
	 *
	 * @param npc 目标 NPC / target NPC
	 * @param player 参考玩家 / reference player
	 * @param highestLevel 队伍最高等级 / highest group level
	 * @return 掉落修正参数 / drop modifiers
	 */
	public DropModifiers createDropModifiers(Npc npc, Player player, int highestLevel) {
		DropModifiers modifiers = new DropModifiers();
		boolean isChest = npc.getAi2().getName().equals("chest");
		modifiers.setDropNpcChest(isChest);
		modifiers.setDropRace(player.getRace());
		modifiers.setBoostDropRate(calculateBoostDropRate(player, npc));
		modifiers.setReductionDropRate(getReductionDropRate(npc.getLevel(), highestLevel, npc.getWorldId(), isChest));
		return modifiers;
	}

	int registerGlobalDrops(Npc npc, Player player, int winnerObj, Set<DropItem> droppedItems, int index,
			DropModifiers dropModifiers) {
		GlobalDropData globalDrops = DataManager.GLOBAL_DROP_DATA;
		if (globalDrops == null) {
			return index;
		}
		int globalDropCount = 0;
		boolean kinahRegistered = false;
		for (GlobalRule rule : globalDrops.getAllRules()) {
			if (globalDropCount >= DropConfig.MAX_GLOBAL_DROPS_PER_NPC) {
				break;
			}
			if (rule.getGlobalRuleItems() == null || !matchesGlobalRule(rule, npc, player)) {
				continue;
			}
			List<Integer> allowedItems = new ArrayList<>();
			for (GlobalDropItem globalItem : rule.getGlobalRuleItems().getGlobalDropItems()) {
				int diff = npc.getLevel() - globalItem.getItemTemplate().getLevel();
				if (diff >= rule.getMinDiff() && diff <= rule.getMaxDiff()) {
					allowedItems.add(globalItem.getId());
				}
			}
			if (allowedItems.isEmpty()) {
				continue;
			}
			int itemId = allowedItems.size() == 1 ? allowedItems.getFirst() : Rnd.get(allowedItems);
			boolean isKinah = itemId == ItemId.KINAH.value();
			if (isKinah && kinahRegistered) {
				continue;
			}
			float chance = calculateGlobalDropChance(rule, itemId, dropModifiers);
			if (Rnd.get() * 100 > chance) {
				continue;
			}
			long count = randomRuleCount(rule, npc.getLevel());
			if (isKinah) {
				droppedItems.removeIf(item -> item.getDropTemplate().getItemId() == ItemId.KINAH.value());
				count = dropModifiers.calculateKinahAmount(count, DropConfig.KINAH_RATE);
				kinahRegistered = true;
			}
			droppedItems.add(regDropItem(index++, winnerObj, npc.getObjectId(), itemId, count));
			globalDropCount++;
		}
		return index;
	}

	private boolean matchesGlobalRule(GlobalRule rule, Npc npc, Player player) {
		if (rule.getRestrictionRace() == GlobalRule.RestrictionRace.ELYOS && player.getRace() == Race.ASMODIANS
				|| rule.getRestrictionRace() == GlobalRule.RestrictionRace.ASMODIANS && player.getRace() == Race.ELYOS) {
			return false;
		}
		if (rule.getGlobalRuleMaps() != null && rule.getGlobalRuleMaps().getGlobalDropMaps().stream()
				.noneMatch(map -> map.getMapId() == npc.getPosition().getMapId())) {
			return false;
		}
		if (rule.getGlobalRuleWorlds() != null && rule.getGlobalRuleWorlds().getGlobalDropWorlds().stream()
				.noneMatch(world -> world.getWorldDropType().equals(npc.getWorldDropType()))) {
			return false;
		}
		if (rule.getGlobalRuleRatings() != null && rule.getGlobalRuleRatings().getGlobalDropRatings().stream()
				.noneMatch(rating -> rating.getRating().equals(npc.getRating()))) {
			return false;
		}
		if (rule.getGlobalRuleRaces() != null && rule.getGlobalRuleRaces().getGlobalDropRaces().stream()
				.noneMatch(race -> race.getRace().equals(npc.getRace()))) {
			return false;
		}
		if (rule.getGlobalRuleTribes() != null && rule.getGlobalRuleTribes().getGlobalDropTribes().stream()
				.noneMatch(tribe -> tribe.getTribe().equals(npc.getTribe()))) {
			return false;
		}
		return rule.getGlobalRuleZones() == null || rule.getGlobalRuleZones().getGlobalDropZones().stream()
				.anyMatch(zone -> npc.isInsideZone(ZoneName.get(zone.getZone())));
	}

	long randomRuleCount(GlobalRule rule, int npcLevel) {
		long minCount = rule.getMinCountForNpcLevel(npcLevel);
		long maxCount = rule.getMaxCountForNpcLevel(npcLevel);
		return minCount == maxCount ? minCount : Rnd.get(Math.toIntExact(minCount), Math.toIntExact(maxCount));
	}

	float calculateGlobalDropChance(GlobalRule rule, int itemId, DropModifiers dropModifiers) {
		return itemId == ItemId.KINAH.value() ? rule.getChance()
				: dropModifiers.calculateDropChance(rule.getChance(), !rule.getNoReduction());
	}

	private Player initDropNpc(Player player, int npcObjId, List<Player> allowedLooters, Collection<Player> groupMembers) {
		Player looter = null;
		DropNpc dropNpc = new DropNpc(npcObjId);
		var lootingTeam = player.getCurrentTeam();
		if (lootingTeam != null) {
			LootGroupRules lootGroupRules = lootingTeam.getLootGroupRules();
			switch (lootGroupRules.getLootRule()) {
				case ROUNDROBIN:
					if (groupMembers.size() > lootGroupRules.getNrRoundRobin()) {
						lootGroupRules.setNrRoundRobin(lootGroupRules.getNrRoundRobin() + 1);
					} else {
						lootGroupRules.setNrRoundRobin(1);
					}
					int i = 0;
					for (Player member : groupMembers) {
						if (++i == lootGroupRules.getNrRoundRobin()) {
							allowedLooters.add(member);
							looter = member;
							break;
						}
					}
					break;
				case FREEFORALL:
					allowedLooters.addAll(groupMembers);
					break;
				case LEADER:
					Player leader = player.isInGroup2() ? player.getPlayerGroup2().getLeaderObject()
							: player.getPlayerAlliance2().getLeaderObject();
					allowedLooters.add(leader);
					looter = leader;
					break;
			}
			dropNpc.setInRangePlayers(groupMembers);
			dropNpc.setLootingTeam(lootingTeam);
		} else {
			allowedLooters.add(player);
		}
		allowedLooters.forEach(dropNpc::setAllowedLooter);
		dropRegistrationMap.put(npcObjId, dropNpc);
		return looter;
	}

	Float getReductionDropRate(int npcLevel, int highestLevel, int mapId, boolean isChest) {
		if (DropConfig.DISABLE_DROP_REDUCTION || noReductionMaps.contains(mapId) || isChest && npcLevel == 1) {
			return null;
		}
		int dropChance = DropRewardEnum.dropRewardFrom(npcLevel - highestLevel);
		return dropChance == 100 ? null : dropChance / 100f;
	}

	private float calculateBoostDropRate(Player player, Npc npc) {
		int boostDropRate = npc.getGameStats().getStat(StatEnum.BOOST_DROP_RATE, 100).getCurrent();
		boostDropRate = player.getGameStats().getStat(StatEnum.BOOST_DROP_RATE, boostDropRate).getCurrent();
		boostDropRate = player.getGameStats().getStat(StatEnum.DR_BOOST, boostDropRate).getCurrent();
		if (player.getCommonData().getCurrentReposteEnergy() > 0) {
			boostDropRate += 5;
		}
		if (player.getCommonData().getCurrentSalvationPercent() > 0) {
			boostDropRate += 5;
		}
		if (player.getActiveHouse() != null && player.getActiveHouse().getHouseType() == HouseType.PALACE) {
			boostDropRate += 5;
		}
		return player.getRates().getDropRate() * boostDropRate / 100f;
	}

	/**
	 * 创建并填充一条掉落物品记录。
	 * Creates and fills a single drop item entry.
	 *
	 * @param index 掉落索引 / drop index
	 * @param playerObjId 归属玩家对象 ID / owner player object id
	 * @param objId NPC 对象 ID / NPC object id
	 * @param itemId 物品 ID / item id
	 * @param count 数量 / count
	 * @return 掉落物品记录 / drop item
	 */
	public DropItem regDropItem(int index, int playerObjId, int objId, int itemId, long count) {
		DropItem item = new DropItem(new Drop(itemId, 1, 1, 100, false, false));
		item.setPlayerObjId(playerObjId);
		item.setNpcObj(objId);
		item.setCount(count);
		item.setIndex(index);
		return item;
	}

	private float getRatingModifier(Npc npc) {
		float ratingModifier = 1f;
		if (npc.getRating() != null) {
			if (npc.getRating().equals(NpcRating.NORMAL)) {
				ratingModifier = 1f;
			} else if (npc.getRating().equals(NpcRating.ELITE)) {
				ratingModifier = 1.5f;
			}
		}
		return ratingModifier;
	}

	/**
	 * 获取 NPC 掉落登记映射。
	 * Returns the NPC drop registration map.
	 *
	 * @return 掉落登记映射 / drop registration map
	 */
	public Map<Integer, DropNpc> getDropRegistrationMap() {
		return dropRegistrationMap;
	}

	/**
	 * 获取当前掉落物品映射。
	 * Returns the current drop-item map.
	 *
	 * @return 掉落物品映射 / current drop map
	 */
	public Map<Integer, Set<DropItem>> getCurrentDropMap() {
		return currentDropMap;
	}

	/**
	 * 获取单例实例。
	 * Returns the singleton instance.
	 *
	 * service instance
	 */
	public static DropRegistrationService getInstance() {
		ObjectProvider<DropRegistrationService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<DropRegistrationService> instanceProvider) {
		DropRegistrationService.instanceProvider = instanceProvider;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final DropRegistrationService instance = new DropRegistrationService();
	}
}
