package com.aionemu.gameserver.controllers;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.ai.RetailAreaEngine;
import com.aionemu.gameserver.ai.RetailSensoryAreaEngine;
import com.aionemu.gameserver.configs.main.*;
import com.aionemu.gameserver.controllers.attack.AttackUtil;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.PlayerInitialData;
import com.aionemu.gameserver.lifecycle.*;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.gameobjects.*;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.BindPointPosition;
import com.aionemu.gameserver.model.gameobjects.player.MinionCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.stats.container.PlayerGameStats;
import com.aionemu.gameserver.model.summons.SummonMode;
import com.aionemu.gameserver.model.summons.UnsummonType;
import com.aionemu.gameserver.model.team2.group.PlayerFilters.ExcludePlayerFilter;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.model.templates.flypath.FlyPathEntry;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.panels.SkillPanel;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.model.templates.robot.RobotInfo;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.stats.PlayerStatsTemplate;
import com.aionemu.gameserver.model.templates.zone.ZoneClassName;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.services.*;
import com.aionemu.gameserver.services.abyss.AbyssService;
import com.aionemu.gameserver.services.events.bg.DeathmatchBg;
import com.aionemu.gameserver.services.events.bg.SoloSurvivorBg;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.summons.SummonsService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.services.toypet.PetSpawnService;
import com.aionemu.gameserver.skillengine.model.*;
import com.aionemu.gameserver.skillengine.model.Skill.SkillMethod;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldType;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.Future;


/**
 * 玩家控制器，管理视野、区域、战斗、技能、死亡与姿态等玩家行为。
 * Player controller managing sight, zones, combat, skills, death and stance behaviors.
 *
 * @author -Nemesiss-, ATracer, xavier, Sarynth, RotO, xTz, KID
 * @modified Sippolo, yayaya
 */
@Slf4j
public class PlayerController extends CreatureController<Player> {

	private boolean isInShutdownProgress;
	private long lastAttackMilis = 0;
	private long lastAttackedMilis = 0;
	private int stance = 0;
	private int stanceType = 0;
	private Map<Integer, VisibleObject> autoPortals = new LinkedHashMap<Integer, VisibleObject>();

	/**
	 * 玩家看到其他可见对象时同步状态包。
	 * Syncs state packets when the player sees another visible object.
	 *
	 * @param object 进入视野的对象 / the object entering sight
	 */
	@Override
	public void see(VisibleObject object) {
		super.see(object);
		if (object instanceof Player) {
			Player player = (Player) object;
			PacketSendUtility.sendPacket(getOwner(), new SM_PLAYER_INFO(player, getOwner().isAggroIconTo(player)));
			PacketSendUtility.sendPacket(getOwner(), new SM_NOTIFY_VIP_ICON(player));
			PacketSendUtility.sendPacket(getOwner(), new SM_MOTION(player.getObjectId(), player.getMotions().getActiveMotions()));
			if (player.isUseRobot() || player.getRobotId() != 0) {
				PacketSendUtility.sendPacket(getOwner(), new SM_USE_ROBOT(player, getRobotInfo(player).getRobotId()));
			}
			if (player.isTransformed()) {
				TeleportService2.playerTransformation(getOwner());
				TeleportService2.instanceTransformation(getOwner());
				TeleportService2.archdaevaTransformation(getOwner());
				PacketSendUtility.broadcastPacketAndReceive(player, new SM_TRANSFORM(player, player.getTransformedModelId(), true, player.getTransformedItemId()));
				PacketSendUtility.broadcastPacketAndReceive(player, new SM_TRANSFORM(player, true));
			}
			if (player.isInPlayerMode(PlayerMode.RIDE)) {
				PacketSendUtility.sendPacket(getOwner(), new SM_EMOTION(player, EmotionType.RIDE, 0, player.ride.getNpcId()));
			} else if (player.getPet() != null) {
				log.debug("Player " + getOwner().getName() + " sees " + object.getName() + " that has Toypet");
				PacketSendUtility.sendPacket(getOwner(), new SM_PET(3, player.getPet()));
			}
			if (player.getMinion() != null) {
				log.debug("Player " + getOwner().getName() + " sees " + object.getName() + " that has minion");
				MinionCommonData commonData = player.getMinionList().getMinion(object.getObjectId());
				PacketSendUtility.sendPacket(getOwner(), new SM_MINIONS(5, commonData));
			}
			player.getEffectController().sendEffectIconsTo(getOwner());
		} else if (object instanceof Kisk) {
			Kisk kisk = ((Kisk) object);
			PacketSendUtility.sendPacket(getOwner(), new SM_NPC_INFO(kisk, getOwner()));
			if (getOwner().getRace() == kisk.getOwnerRace()) {
				PacketSendUtility.sendPacket(getOwner(), new SM_KISK_UPDATE(kisk));
			}
		} else if (object instanceof Npc) {
			Npc npc = ((Npc) object);
			PacketSendUtility.sendPacket(getOwner(), new SM_NPC_INFO(npc, getOwner()));
			PacketSendUtility.sendPacket(getOwner(), new SM_EMOTION_NPC(npc, npc.getState(), EmotionType.SELECT_TARGET));
			PacketSendUtility.sendPacket(getOwner(), new SM_HEADING_UPDATE(object.getObjectId(), (byte) object.getHeading()));
			if (!npc.getEffectController().isEmpty()) {
				npc.getEffectController().sendEffectIconsTo(getOwner());
			}
		} else if (object instanceof Summon) {
			Summon npc = ((Summon) object);
			PacketSendUtility.sendPacket(getOwner(), new SM_NPC_INFO(npc));
			if (!npc.getEffectController().isEmpty()) {
				npc.getEffectController().sendEffectIconsTo(getOwner());
			}
		} else if (object instanceof Gatherable || object instanceof StaticObject) {
			PacketSendUtility.sendPacket(getOwner(), new SM_GATHERABLE_INFO(object));
		} else if (object instanceof Pet) {
			PacketSendUtility.sendPacket(getOwner(), new SM_PET(3, (Pet) object));
		} else if (object instanceof Minion) {
			MinionCommonData commonData = getOwner().getMinionList().getMinion(object.getObjectId());
			PacketSendUtility.sendPacket(getOwner(), new SM_MINIONS(5, commonData));
		}
	}

	private RobotInfo getRobotInfo(Player player) {
		ItemTemplate template = player.getEquipment().getMainHandWeapon().getItemSkinTemplate();
		return DataManager.ROBOT_DATA.getRobotInfo(template.getRobotId());
	}

	/**
	 * 对象离开玩家视野时回调。
	 * Callback when an object leaves the player's sight.
	 *
	 * @param object 离开视野的对象 / the object leaving sight
	 * @param isOutOfRange 是否因超出距离离开 / whether the leave is due to being out of range
	 */
	@Override
	public void notSee(VisibleObject object, boolean isOutOfRange) {
		super.notSee(object, isOutOfRange);
		if (object instanceof Pet) {
			PacketSendUtility.sendPacket(getOwner(), new SM_PET(4, (Pet) object));
		} else if (object instanceof Minion) {
			MinionCommonData commonData = getOwner().getMinionList().getMinion(object.getObjectId());
			PacketSendUtility.sendPacket(getOwner(), new SM_MINIONS(6, commonData));
		} else {
			PacketSendUtility.sendPacket(getOwner(), new SM_DELETE(object, isOutOfRange ? 0 : 15));
		}
	}

	/**
	 * 更新附近可接任务提示。
	 * Updates nearby quest availability hints.
	 *
	 */
	public void updateNearbyQuests() {
		HashMap<Integer, Integer> nearbyQuestList = new HashMap<>();
		for (int questId : getOwner().getPosition().getMapRegion().getParent().getQuestIds()) {
			int diff = 0;
			if (questId <= 0xFFFF) {
				diff = QuestService.getLevelRequirement(questId, getOwner().getCommonData().getLevel());
			}
			if (diff <= 2 && QuestService.checkStartConditions(new QuestEnv(null, getOwner(), questId, 0), false)) {
				nearbyQuestList.put(questId, diff);
			}
		}
		PacketSendUtility.sendPacket(getOwner(), new SM_NEARBY_QUESTS(nearbyQuestList));
	}

	/**
	 * 玩家进入区域时触发任务/事件逻辑。
	 * Triggers quest/event logic when the player enters a zone.
	 *
	 * @param zone 进入的区域 / entered zone
	 */
	@Override
	public void onEnterZone(ZoneInstance zone) {
		Player player = getOwner();
		if ((!zone.canRide()) && (player.isInPlayerMode(PlayerMode.RIDE))) {
			player.unsetPlayerMode(PlayerMode.RIDE);
		}
		if (zone.getZoneTemplate().getZoneType().equals(ZoneClassName.FORT) && (player.isInState(CreatureState.FLYING))) {
			/**
			 * 玩家飞行中进入「潘尼特拉要塞」区域时，系统强制降落。
	 * If a player enters zone "Panesterra Fortress" while flying, the system will land the player.
			 */
			switch (player.getWorldId()) {
			case 400020000: // Belus.
			case 400040000: // Aspida.
			case 400050000: // Atanatos.
			case 400060000: // Disillon.
				player.setFlyState(0);
				player.getFlyController().endFly(true);
				player.unsetState(CreatureState.FLYING);
				// 此区域无法飞行。 / You cannot fly in this area.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FLYING_FORBIDDEN_ZONE);
				PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.LAND, 0, 0), true);
				break;
			}
		}
		InstanceService.onEnterZone(player, zone);
		if (zone.getAreaTemplate().getZoneName() == null) {
			log.error(I18n.get("log.f297922b6249", zone.getAreaTemplate().getWorldId()));
		} else {
			GameEngineServices.questEngine().onEnterZone(new QuestEnv(null, player, 0, 0), zone.getAreaTemplate().getZoneName());
		}
	/**
		 * 这些副本传送门对敌对种族为「刷新并反向」。玩家进入部分区域时会出现传送门。
	 * These instance portals are "spawn & reversed" to the opposite race. If a player enters a few areas, a portal will appear automatically. These portals
		 * are only 2 minute ingame before despawn. PS: Please, check "portal/AI2" for these portal.
		 */
		SpawnTemplate template;
		if (zone.getAreaTemplate().getZoneName() == ZoneName.get("REIAN_REFUGEE_CAMP_210070000")) {
			switch (player.getRace()) {
			// 伦图斯基地 / Rentus Base
			case ELYOS:
				template = SpawnEngine.addNewSingleTimeSpawn(210070000, 730399, 1147.6155f, 800.88049f, 563.40173f, (byte) 0);
				template.setEntityId(885);
				autoPortals.put(730399, SpawnEngine.spawnObject(template, 1));
				break;
			// 被占领的伦图斯基地 / Occupied Rentus Base
			case ASMODIANS:
				template = SpawnEngine.addNewSingleTimeSpawn(210070000, 832992, 1147.6155f, 800.88049f, 563.40173f, (byte) 0);
				template.setEntityId(885);
				autoPortals.put(832992, SpawnEngine.spawnObject(template, 1));
				break;
			default:
				break;
			}
		} else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("RENTUS_RECOVERY_BASE_220080000")) {
			switch (player.getRace()) {
			// 被占领的伦图斯基地 / Occupied Rentus Base
			case ELYOS:
				template = SpawnEngine.addNewSingleTimeSpawn(220080000, 832991, 1973.3156f, 2017.3612f, 329.13571f, (byte) 0);
				template.setEntityId(900);
				autoPortals.put(832991, SpawnEngine.spawnObject(template, 1));
				break;
			// 伦图斯基地 / Rentus Base
			case ASMODIANS:
				template = SpawnEngine.addNewSingleTimeSpawn(220080000, 730399, 1973.3156f, 2017.3612f, 329.13571f, (byte) 0);
				template.setEntityId(900);
				autoPortals.put(730399, SpawnEngine.spawnObject(template, 1));
				break;
			default:
				break;
			}
		} else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("RUINHOLD_SCATTERINGS_210070000")) {
			switch (player.getRace()) {
			case ELYOS:
				// 提亚马特要塞 / Tiamat Stronghold
				template = SpawnEngine.addNewSingleTimeSpawn(210070000, 832995, 93.335602f, 1474.6055f, 491.90103f, (byte) 0);
				template.setEntityId(306);
				autoPortals.put(832995, SpawnEngine.spawnObject(template, 1));
				// 龙主避难所 / Dragon Lord Refuge
				template = SpawnEngine.addNewSingleTimeSpawn(210070000, 832998, 103.8532f, 1461.7725f, 494.52884f, (byte) 0);
				template.setEntityId(865);
				autoPortals.put(832998, SpawnEngine.spawnObject(template, 1));
				break;
			case ASMODIANS:
				// 【痛苦】龙主避难所 / [Anguished] Dragon Lord Refuge
				template = SpawnEngine.addNewSingleTimeSpawn(210070000, 832997, 103.8532f, 1461.7725f, 494.52884f, (byte) 0);
				template.setEntityId(865);
				autoPortals.put(832997, SpawnEngine.spawnObject(template, 1));
				break;
			default:
				break;
			}
		} else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("DRAGONFALLS_GLARE_220080000")) {
			switch (player.getRace()) {
			case ELYOS:
				// 【痛苦】龙主避难所 / [Anguished] Dragon Lord Refuge
				template = SpawnEngine.addNewSingleTimeSpawn(220080000, 832997, 2862.9939f, 1679.4772f, 308.87949f, (byte) 0);
				template.setEntityId(422);
				autoPortals.put(832997, SpawnEngine.spawnObject(template, 1));
				break;
			case ASMODIANS:
				// 提亚马特要塞 / Tiamat Stronghold
				template = SpawnEngine.addNewSingleTimeSpawn(220080000, 832996, 2845.8596f, 1659.2727f, 302.67017f, (byte) 0);
				template.setEntityId(364);
				autoPortals.put(832996, SpawnEngine.spawnObject(template, 1));
				// 龙主避难所 / Dragon Lord Refuge
				template = SpawnEngine.addNewSingleTimeSpawn(220080000, 832998, 2862.9939f, 1679.4772f, 308.87949f, (byte) 0);
				template.setEntityId(422);
				autoPortals.put(832998, SpawnEngine.spawnObject(template, 1));
				break;
			default:
				break;
			}
		} else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("DANUAR_SANCTUARY_INSPECTOR_210070000")) {
			switch (player.getRace()) {
			// 达努阿尔圣所 / Danuar Sanctuary
			case ELYOS:
				template = SpawnEngine.addNewSingleTimeSpawn(210070000, 731570, 2097.4739f, 2276.1729f, 294.90442f, (byte) 0);
				template.setEntityId(888);
				autoPortals.put(731570, SpawnEngine.spawnObject(template, 1));
				break;
			// 【被占领】达努阿尔圣所 / [Seized] Danuar Sanctuary
			case ASMODIANS:
				template = SpawnEngine.addNewSingleTimeSpawn(210070000, 731549, 2097.4739f, 2276.1729f, 294.90442f, (byte) 0);
				template.setEntityId(888);
				autoPortals.put(731549, SpawnEngine.spawnObject(template, 1));
				break;
			default:
				break;
			}
		} else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("DANUAR_SANCTUARY_INVESTIGATION_AREA_220080000")) {
			switch (player.getRace()) {
			// 【被占领】达努阿尔圣所 / [Seized] Danuar Sanctuary
			case ELYOS:
				template = SpawnEngine.addNewSingleTimeSpawn(220080000, 731549, 1667.7465f, 562.70654f, 258.88382f, (byte) 0);
				template.setEntityId(407);
				autoPortals.put(731549, SpawnEngine.spawnObject(template, 1));
				break;
			// 达努阿尔圣所 / Danuar Sanctuary
			case ASMODIANS:
				template = SpawnEngine.addNewSingleTimeSpawn(220080000, 731570, 1667.7465f, 562.70654f, 258.88382f, (byte) 0);
				template.setEntityId(407);
				autoPortals.put(731570, SpawnEngine.spawnObject(template, 1));
				break;
			default:
				break;
			}
		}
	/**
		 * 保护城市：敌对种族玩家进入这些区域时送回绑定点。
	 * For Protect City: if an opposite-race player enters these zones, return to "Bind Location"
		 */
		if (player.getAccessLevel() == 0) {
			if (
			// 莫尔海姆 / Morheim
			zone.getAreaTemplate().getZoneName() == ZoneName.get("MORHEIM_SNOW_FIELD_220020000") ||
			// 贝卢斯兰 / Beluslan
					zone.getAreaTemplate().getZoneName() == ZoneName.get("KURNGALFBERG_220040000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("RED_MANE_CAVERN_220040000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("BELUSLAN_FORTRESS_220040000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("HOARFROST_SHELTER_220040000") ||
					// 布鲁斯特霍宁 / Brusthonin
					zone.getAreaTemplate().getZoneName() == ZoneName.get("POLLUTED_WASTE_220050000") ||
					// 恩沙尔 / Enshar
					zone.getAreaTemplate().getZoneName() == ZoneName.get("DAWNBREAK_TEMPLE_220080000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("WHIRLPOOL_TEMPLE_220080000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("DRAGONREST_TEMPLE_220080000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("FATEBOUND_LEGION_OUTPOST_220080000") ||
					// 诺斯沃尔德 / Norsvold
					zone.getAreaTemplate().getZoneName() == ZoneName.get("AZPHEL_SANCTUARY_220110000")) {
				switch (player.getRace()) {
				case ELYOS:
					TeleportService2.moveToBindLocation(player, true);
					break;
				default:
					break;
				}
			} else if (
			// 艾特南 / Eltnen
			zone.getAreaTemplate().getZoneName() == ZoneName.get("MANDURI_FOREST_210020000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("GOLDEN_BOUGH_GARRISON_210020000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("MYSTIC_SPRING_OF_AGAIRON_210020000") ||
					// 海隆 / Heiron
					zone.getAreaTemplate().getZoneName() == ZoneName.get("HEIRONOPOLIS_210040000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("PATEMA_RUINS_210040000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("ARBOLUS_HAVEN_210040000") ||
					// 西奥波莫斯 / Theobomos
					zone.getAreaTemplate().getZoneName() == ZoneName.get("PORT_ANANGKE_210060000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("JOSNACKS_VIGIL_210060000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("CRIMSON_BARRENS_210060000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("OBSERVATORY_VILLAGE_210060000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("SOUTHERN_LATHERON_COAST_210060000") ||
					// 西格尼亚 / Cygnea
					zone.getAreaTemplate().getZoneName() == ZoneName.get("AEQUIS_OUTPOST_210070000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("AEQUIS_HEADQUARTERS_210070000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("AEQUIS_ADVANCE_POST_210070000")
					|| zone.getAreaTemplate().getZoneName() == ZoneName.get("AEQUIS_DETACHMENT_POST_210070000") ||
					// 伊卢玛 / Iluma
					zone.getAreaTemplate().getZoneName() == ZoneName.get("ARIEL_SANCTUARY_210100000")) {
				switch (player.getRace()) {
				case ASMODIANS:
					TeleportService2.moveToBindLocation(player, true);
					break;
				default:
					break;
				}
			}
		}
	}

	/**
	 * 玩家离开区域时回调。
	 * Callback when the player leaves a zone.
	 *
	 * @param zone 离开的区域 / left zone
	 */
	@Override
	public void onLeaveZone(ZoneInstance zone) {
		Player player = getOwner();
		InstanceService.onLeaveZone(player, zone);
		ZoneName zoneName = zone.getAreaTemplate().getZoneName();
		if (zoneName == null) {
			log.warn(I18n.get("log.cdc4102324ff", zone.getAreaTemplate().getWorldId()));
			return;
		}
		GameEngineServices.questEngine().onLeaveZone(new QuestEnv(null, player, 0, 0), zoneName);
	}

	/**
	 * {@inheritDoc} Should only be triggered from one place (life stats)
	 */
	/**
	 * 玩家进入世界时的处理。
	 * Processing when the player enters the world.
	 *
	 */
	public void onEnterWorld() {
		InstanceService.onEnterInstance(getOwner());
		TeleportService2.playerTransformation(getOwner());
		TeleportService2.instanceTransformation(getOwner());
		TeleportService2.archdaevaTransformation(getOwner());
		if (getOwner().getPosition().getWorldMapInstance().getParent().isExceptBuff()) {
			getOwner().getEffectController().removeAllEffects();
		}
		for (Effect ef : getOwner().getEffectController().getAbnormalEffects()) {
			if (ef.isDeityAvatar()) {
				// 若世界类型非欧比斯/巴劳雷亚/帕内斯特拉则移除欧比斯变身。 / Remove abyss transformation if worldtype != "Abyss" && worldtype != "Balaurea" && worldtype != "Panesterra"
				if (getOwner().getWorldType() != WorldType.ABYSS && getOwner().getWorldType() != WorldType.BALAUREA && getOwner().getWorldType() != WorldType.PANESTERRA || getOwner().isInInstance()) {
					ef.endEffect();
					getOwner().getEffectController().clearEffect(ef);
				}
			} else if (ef.getSkillTemplate().getDispelCategory() == DispelCategoryType.NPC_BUFF) {
				ef.endEffect();
				getOwner().getEffectController().clearEffect(ef);
			}
		}
	}

	/**
	 * 玩家离开世界时的处理。
	 * Processing when the player leaves the world.
	 *
	 */
	public void onLeaveWorld() {
		GameFeatureServices.protectorConquerorService().onLeaveMap(getOwner());
		InstanceService.onLeaveInstance(getOwner());
	}

	/**
	 * 校验登录落点区域是否合法。
	 * Validates whether the login zone position is legal.
	 *
	 */
	public void validateLoginZone() {
		int mapId;
		float x, y, z;
		byte h;
		boolean moveToBind = false;

		BindPointPosition bind = getOwner().getBindPoint();

		if (bind != null) {
			mapId = bind.getMapId();
			x = bind.getX();
			y = bind.getY();
			z = bind.getZ();
			h = bind.getHeading();
		} else {
			PlayerInitialData.LocationData start = DataManager.PLAYER_INITIAL_DATA.getSpawnLocation(getOwner().getRace());

			mapId = start.getMapId();
			x = start.getX();
			y = start.getY();
			z = start.getZ();
			h = start.getHeading();
		}
		var lastOnline = getOwner().getCommonData().getLastOnline();
		long secondsOffline = lastOnline == null ? 0 : (System.currentTimeMillis() - lastOnline.getTime()) / 1000;
		moveToBind = RetailAreaEngine.isNoPark(getOwner(), secondsOffline);
		if (!moveToBind && secondsOffline > 10 * 60) {
			MapRegion mapRegion = getOwner().getPosition().getWorldMapInstance().getRegion(getOwner().getX(), getOwner().getY(), getOwner().getZ());
			for (ZoneInstance zone : mapRegion.getZones(getOwner())) {
				if (!zone.canRecall()) {
					moveToBind = true;
					break;
				}
			}
		}

		if (moveToBind) {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(getOwner(), mapId, x, y, z, h);
		}
	}

	/**
	 * 玩家死亡完整处理（可选是否显示死亡包）。
	 * Full player death handling (optionally showing the die packet).
	 *
	 * @param lastAttacker 最后攻击者 / last attacker
	 * @param showPacket 是否显示死亡包 / whether to show the die packet
	 */
	public void onDie(Creature lastAttacker, boolean showPacket) {
		Player player = this.getOwner();
		player.getController().cancelCurrentSkill();
		player.setRebirthRevive(getOwner().haveSelfRezEffect());
		showPacket = player.hasResurrectBase() ? false : showPacket;
		Creature master = lastAttacker.getMaster();
		if ((PvPConfig.ENABLE_KILLING_SPREE_SYSTEM) && (getOwner().getRawKillCount() > 0)) {
			if ((master instanceof Npc)) {
				PvPSpreeService.cancelSpree(player, (Npc) master, false);
			}
			if (((master instanceof Player)) && (master.getRace() != player.getRace())) {
				PvPSpreeService.cancelSpree(player, (Player) master, true);
			}
		}
		if (EventsConfig.ENABLE_CRAZY) {
			if (((master instanceof Player)) && (master.getRace() != player.getRace())) {
				GameEventServices.crazyDaevaService().crazyOnDie(player, (Player) master, true);
			}
		}
		AbyssRank ar = player.getAbyssRank();
		if (AbyssService.isOnPvpMap(player) && ar != null) {
			if (ar.getRank().getId() >= 1) {
				if (!player.isInDuel()) {
				    AbyssService.rankedKillAnnounce(player);
				}
			}
		}
		if (GameGameplayServices.duelService().isDueling(player.getObjectId())) {
			if (master != null && GameGameplayServices.duelService().isDueling(player.getObjectId(), master.getObjectId())) {
				GameGameplayServices.duelService().loseDuel(player);
				player.getEffectController().removeAbnormalEffectsByTargetSlot(SkillTargetSlot.DEBUFF);
				player.getLifeStats().setCurrentHp(player.getLifeStats().getMaxHp() / 3);
				return;
			}
			GameGameplayServices.duelService().loseDuel(player);
		}
		player.getCommonData().setDp(0);
		if (GameFeatureServices.ffaService().isInArena(player) && player.isFFA()) {
			player.getAggroList().clear();
			GameFeatureServices.ffaService().onDie(player, master);
			return;
		}
		if (player.isBandit()) {
			player.getAggroList().clear();
			GameFeatureServices.banditService().onDie(player, master);
			return;
		}
		if (player.getBattleground() != null && player.getBattleground() instanceof DeathmatchBg || player.getBattleground() != null && player.getBattleground() instanceof SoloSurvivorBg) {
			player.getAggroList().clear();
			player.getBattleground().onDie(player, master);
			return;
		}

	/**
	 * 释放召唤物 / Release Summon
	 */
		Summon summon = player.getSummon();
		if (summon != null) {
			SummonsService.doMode(SummonMode.RELEASE, summon, UnsummonType.UNSPECIFIED);
		}

	/**
	 * 释放宠物 / Release Pet
	 */
		Pet pet = player.getPet();
		if (pet != null) {
			PetSpawnService.dismissPet(player, true);
		}

	/**
	 * 释放守护灵 / Release Minion
	 */
		Minion minion = player.getMinion();
		if (minion != null) {
			GameEventBootstrapServices.minionService().despawnMinion(player, minion.getObjectId());
		}

		if (player.isInState(CreatureState.FLYING)) {
			player.setIsFlyingBeforeDeath(true);
		}

		// 骑乘 / ride
		player.setPlayerMode(PlayerMode.RIDE, null);
		player.unsetState(CreatureState.RESTING);
		player.unsetState(CreatureState.FLOATING_CORPSE);

		// 取消飞行 / unsetflying
		player.unsetState(CreatureState.FLYING);
		player.unsetState(CreatureState.GLIDING);
		player.setFlyState(0);

		if (player.isInInstance() && !GameFeatureServices.ffaService().isInArena(player) || player.getBattleground() == null || !player.getBattleground().is1v1()) {
			if (player.getPosition().getWorldMapInstance().getInstanceHandler().onDie(player, lastAttacker)) {
				super.onDie(lastAttacker);
				return;
			}
		}
		MapRegion mapRegion = player.getPosition().getMapRegion();
		if (mapRegion != null && mapRegion.onDie(lastAttacker, getOwner())) {
			return;
		}
		this.doReward();
		if (master instanceof Npc || master == player) {
			if (player.getLevel() > 4 && !isNoDeathPenaltyInEffect() && !isNoDeathPenaltyReduceInEffect() && !isDeathPenaltyReduceInEffect()) {
				player.getCommonData().calculateExpLoss();
			}
		}
		super.onDie(lastAttacker);
		sendDieFromCreature(lastAttacker, showPacket);
		GameEngineServices.questEngine().onDie(new QuestEnv(null, player, 0, 0));
		if (player.isInGroup2()) {
			player.getPlayerGroup2().sendPacket(SM_SYSTEM_MESSAGE.STR_MSG_COMBAT_FRIENDLY_DEATH(player.getName()), new ExcludePlayerFilter(player));
		}
	}

	/**
	 * 玩家死亡（默认显示死亡包）。
	 * Player death (shows the die packet by default).
	 *
	 * @param lastAttacker 最后攻击者 / last attacker
	 */
	@Override
	public void onDie(Creature lastAttacker) {
		this.onDie(lastAttacker, true);
	}

	/**
	 * 向客户端发送死亡相关包。
	 * Sends death-related packets to the client.
	 *
	 */
	public void sendDie() {
		sendDieFromCreature(getOwner(), true);
	}

	private void sendDieFromCreature(Creature lastAttacker, boolean showPacket) {
		Player player = this.getOwner();
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.DIE, 0, player.equals(lastAttacker) ? 0 : lastAttacker.getObjectId()), true);
		if (showPacket) {
			if (player.isInInstance()) {
				PacketSendUtility.sendPacket(player, new SM_DIE(player.haveSelfRezEffect(), player.haveSelfRezItem(), 0, 8, false));
			} else {
				int kiskTimeRemaining = (player.getKisk() != null ? player.getKisk().getRemainingLifetime() : 0);
				PacketSendUtility.sendPacket(player, new SM_DIE(player.canUseRebirthRevive(), player.haveSelfRezItem(), kiskTimeRemaining, 0, isInvader(player)));
			}
		}
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_COMBAT_MY_DEATH);
	}

	private boolean isInvader(Player player) {
		if (player.getRace().equals(Race.ASMODIANS)) {
			return player.getWorldId() == 210060000; // Theobomos.
		} else {
			return player.getWorldId() == 220050000; // Brusthonin.
		}
	}

	/**
	 * 处理玩家击杀奖励。
	 * Handles player kill rewards.
	 *
	 */
	@Override
	public void doReward() {
		GameCoreGameplayServices.pvpService().doReward(getOwner());
	}

	/**
	 * 生成前处理。
	 * Processing before spawn.
	 *
	 */
	@Override
	public void onBeforeSpawn() {
		this.onBeforeSpawn(true);
	}

	/**
	 * 生成前处理，可选闪烁保护。
	 * Processing before spawn with optional blink protection.
	 *
	 * @param blink 是否启用闪烁保护 / whether blink protection is enabled
	 */
	public void onBeforeSpawn(boolean blink) {
		super.onBeforeSpawn();
		if (blink) {
			startProtectionActiveTask();
		}
		if (getOwner().getIsFlyingBeforeDeath()) {
			getOwner().unsetState(CreatureState.FLOATING_CORPSE);
		} else {
			getOwner().unsetState(CreatureState.DEAD);
		}
		getOwner().setState(CreatureState.ACTIVE);
	}

	/**
	 * 玩家攻击目标。
	 * Player attacks a target.
	 *
	 * attack target
	 * @param time 攻击时间参数 / attack timing parameter
	 */
	@Override
	public void attackTarget(Creature target, int time) {

		PlayerGameStats gameStats = getOwner().getGameStats();

		if (!RestrictionsManager.canAttack(getOwner(), target)) {
			return;
		}
		if (!MathUtil.isInAttackRange(getOwner(), target, (float) (getOwner().getGameStats().getAttackRange().getCurrent() / 1000f) + 1)) {
			return;
		}
		if (!GameWorldServices.geoService().canSee(getOwner(), target)) {
			PacketSendUtility.sendPacket(getOwner(), SM_SYSTEM_MESSAGE.STR_ATTACK_OBSTACLE_EXIST);
			return;
		}

		if (target instanceof Npc) {
			GameEngineServices.questEngine().onAttack(new QuestEnv(target, getOwner(), 0, 0));
		}

		int attackSpeed = gameStats.getAttackSpeed().getCurrent();

		long milis = System.currentTimeMillis();
		// 网络 ping…… / network ping..
		if (milis - lastAttackMilis + 300 < attackSpeed) {
			// 漏洞利用 / hack
			return;
		}
		lastAttackMilis = milis;

	/**
	 * 通知攻击观察者 / notify attack observers
	 */
		super.attackTarget(target, time);
	}

	/**
	 * 玩家受到攻击时的处理。
	 * Handles the player being attacked.
	 *
	 * attacker
	 * skill id
	 * @param type 伤害类型 / damage type
	 * damage
	 * @param notifyAttack 是否通知攻击 / whether to notify attack
	 * @param log 日志类型 / log type
	 */
	@Override
	public void onAttack(Creature creature, int skillId, TYPE type, int damage, boolean notifyAttack, LOG log,
			AttackStatus attackStatus) {
		if (getOwner().getLifeStats().isAlreadyDead())
			return;

		if (getOwner().isInvul() || getOwner().isProtectionActive())
			damage = 0;

		cancelUseItem();
		cancelGathering();
		super.onAttack(creature, skillId, type, damage, notifyAttack, log, attackStatus);

		PacketSendUtility.broadcastPacket(getOwner(), new SM_ATTACK_STATUS(getOwner(), creature, type, skillId, damage, log), true);

		lastAttackedMilis = System.currentTimeMillis();
	}

	/**
	 * 使用技能（客户端坐标与时间）。
	 * Uses a skill with client coordinates and timing.
	 *
	 * skill id
	 * target type
	 * @param x X 坐标 / x coordinate
	 * @param y Y 坐标 / y coordinate
	 * @param z Z 坐标 / z coordinate
	 * @param time 时间参数 / time parameter
	 */
	public void useSkill(int skillId, int targetType, float x, float y, float z, int time) {
		Player player = getOwner();

		Skill skill = GameEngineServices.skillEngine().getSkillFor(player, skillId, player.getTarget());

		if (skill != null) {
			if (!RestrictionsManager.canUseSkill(player, skill)) {
				return;
			}
			skill.setTargetType(targetType, x, y, z);
			skill.setHitTime(time);
			skill.useSkill();
		}
	}

	/**
	 * 使用技能模板施放技能。
	 * Casts a skill from a skill template.
	 *
	 * skill template
	 * target type
	 * @param x X 坐标 / x coordinate
	 * @param y Y 坐标 / y coordinate
	 * @param z Z 坐标 / z coordinate
	 * @param clientHitTime 客户端命中时间 / client hit time
	 * skill level
	 */
	public void useSkill(SkillTemplate template, int targetType, float x, float y, float z, int clientHitTime, int skillLevel) {
		Player player = getOwner();

		Skill skill = GameEngineServices.skillEngine().getSkillFor(player, template, player.getTarget());
		if ((skill == null) && (player.isTransformed())) {
			SkillPanel panel = DataManager.PANEL_SKILL_DATA.getSkillPanel(player.getTransformModel().getPanelId());
			if ((panel != null) && (panel.canUseSkill(template.getSkillId(), skillLevel))) {
				skill = GameEngineServices.skillEngine().getSkillFor(player, template, player.getTarget(), skillLevel);
			}
		}

		if (skill != null) {
			if (!RestrictionsManager.canUseSkill(player, skill)) {
				return;
			}
			skill.setTargetType(targetType, x, y, z);
			skill.setHitTime(clientHitTime);
			skill.useSkill();
			QuestEnv env = new QuestEnv(player.getTarget(), player, 0, 0);
			GameEngineServices.questEngine().onUseSkill(env, template.getSkillId());
		}
	}

	/**
	 * 玩家移动过程中回调。
	 * Callback while the player is moving.
	 *
	 */
	@Override
	public void onMove() {
		getOwner().getObserveController().notifyMoveObservers();
		super.onMove();
	}

	@Override
	public void onAfterSpawn() {
		super.onAfterSpawn();
		RetailSensoryAreaEngine.onPlayerMoved(getOwner());
	}

	@Override
	public void onDespawn() {
		RetailSensoryAreaEngine.onPlayerDespawned(getOwner());
		super.onDespawn();
	}

	/**
	 * 玩家停止移动时回调。
	 * Callback when the player stops moving.
	 *
	 */
	@Override
	public void onStopMove() {
		GameMovementLoopServices.playerMoveTaskManager().removePlayer(getOwner());
		getOwner().getObserveController().notifyMoveObservers();
		getOwner().getMoveController().setInMove(false);
		cancelCurrentSkill();
		updateZone();
		super.onStopMove();
	}

	/**
	 * 玩家开始移动时回调。
	 * Callback when the player starts moving.
	 *
	 */
	@Override
	public void onStartMove() {
		getOwner().getMoveController().setInMove(true);
		GameMovementLoopServices.playerMoveTaskManager().addPlayer(getOwner());
		cancelUseItem();
		cancelCurrentSkill();
		super.onStartMove();
	}

	/**
	 * 取消当前技能。
	 * Cancels the current skill.
	 *
	 */
	@Override
	public void cancelCurrentSkill() {
		if (getOwner().getCastingSkill() == null) {
			return;
		}

		Player player = getOwner();
		Skill castingSkill = player.getCastingSkill();
		castingSkill.cancelCast();
		player.removeSkillCoolDown(castingSkill.getSkillTemplate().getDelayId());
		player.setCasting(null);
		player.setNextSkillUse(0);
		if (castingSkill.getSkillMethod() == SkillMethod.CAST) {
			PacketSendUtility.broadcastPacket(player, new SM_SKILL_CANCEL(player, castingSkill.getSkillTemplate().getSkillId()), true);
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SKILL_CANCELED);
		} else if (castingSkill.getSkillMethod() == SkillMethod.ITEM) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_CANCELED(new DescriptionId(castingSkill.getItemTemplate().getNameId())));
			player.removeItemCoolDown(castingSkill.getItemTemplate().getUseLimits().getDelayId());
			PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), castingSkill.getFirstTarget().getObjectId(), castingSkill.getItemObjectId(), castingSkill.getItemTemplate().getTemplateId(), 0, 3, 0), true);
		}
	}

	/**
	 * 取消物品使用。
	 * Cancels item use.
	 *
	 */
	@Override
	public void cancelUseItem() {
		Player player = getOwner();
		Item usingItem = player.getUsingItem();
		player.setUsingItem(null);
		if (hasTask(TaskId.ITEM_USE)) {
			cancelTask(TaskId.ITEM_USE);
			PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), usingItem == null ? 0 : usingItem.getObjectId(), usingItem == null ? 0 : usingItem.getItemTemplate().getTemplateId(), 0, 3, 0), true);
		}
	}

	/**
	 * 取消采集。
	 * Cancels gathering.
	 *
	 */
	public void cancelGathering() {
		Player player = getOwner();
		if (player.getTarget() instanceof Gatherable) {
			Gatherable g = (Gatherable) player.getTarget();
			g.getController().finishGathering(player);
		}
	}

	/**
	 * 更新被动属性。
	 * Updates passive stats.
	 *
	 */
	public void updatePassiveStats() {
		Player player = getOwner();
		for (PlayerSkillEntry skillEntry : player.getSkillList().getAllSkills()) {
			Skill skill = GameEngineServices.skillEngine().getSkillFor(player, skillEntry.getSkillId(), player.getTarget());
			if (skill != null && skill.isPassive()) {
				skill.useSkill();
			}
		}
	}

	/**
	 * 获取所有者玩家。
	 * Gets the owner player.
	 *
	 * @return 所有者玩家 / owner player
	 */
	@Override
	public Player getOwner() {
		return (Player) super.getOwner();
	}

	/**
	 * 恢复玩家属性。
	 * Restores player stats.
	 *
	 * heal type
	 * @param value 恢复数值 / restore value
	 */
	@Override
	public void onRestore(HealType healType, int value) {
		super.onRestore(healType, value);
		switch (healType) {
		case DP:
			getOwner().getCommonData().addDp(value);
			break;
		default:
			break;
		}
	}

	/**
	 * @param player
	 * @return
	 */
	/**
	 * 是否正在与指定玩家决斗。
	 * Whether currently dueling the given player.
	 *
	 * opponent player
	 *
	 * @param player
	 * @return 决斗中则为 true / true if dueling
	 */
	public boolean isDueling(Player player) {
		return GameGameplayServices.duelService().isDueling(player.getObjectId(), getOwner().getObjectId());
	}

	/**
	 * 服务器是否处于关闭流程中。
	 * Whether the server is in shutdown progress.
	 *
	 * @return 关闭中则为 true / true if shutting down
	 */
	public boolean isInShutdownProgress() {
		return isInShutdownProgress;
	}

	/**
	 * 设置关闭流程标志。
	 * Sets the shutdown-progress flag.
	 *
	 * @param isInShutdownProgress 是否关闭中 / whether shutting down
	 */
	public void setInShutdownProgress(boolean isInShutdownProgress) {
		this.isInShutdownProgress = isInShutdownProgress;
	}

	/**
	 * 处理对话选项选择。
	 * Handles dialog option selection.
	 *
	 * dialog id
	 * 玩家 / player
	 * quest id
	 * @param extendedRewardIndex 扩展奖励索引 / extended reward index
	 * @param unk 未知参数 / unknown parameter
	 */
	@Override
	public void onDialogSelect(int dialogId, Player player, int questId, int extendedRewardIndex) {
		switch (dialogId) {
		case 2:
			PacketSendUtility.sendPacket(player, new SM_PRIVATE_STORE(getOwner().getStore(), player));
			break;
		}
	}

	/**
	 * 提升/刷新玩家属性与状态。
	 * Upgrades/refreshes player stats and state.
	 *
	 */
	public void upgradePlayer() {
		Player player = getOwner();
		byte level = player.getLevel();
		PlayerStatsTemplate statsTemplate = DataManager.PLAYER_STATS_DATA.getTemplate(player);
		player.setPlayerStatsTemplate(statsTemplate);
		player.getLifeStats().synchronizeWithMaxStats();
		player.getLifeStats().updateCurrentStats();
		SkillLearnService.addNewSkills(player);
		PacketSendUtility.broadcastPacket(player, new SM_LEVEL_UPDATE(player.getObjectId(), 0, level), true);
		if (HTMLConfig.ENABLE_GUIDES) {
			HTMLService.sendGuideHtml(player);
		}
		ClassChangeService.showClassChangeDialog(player);
		GameEngineServices.questEngine().onLvlUp(new QuestEnv(null, player, 0, 0));
		player.getController().updateZone();
		player.getController().updateNearbyQuests();
		player.getController().updatePassiveStats();
		PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
		if (level == 10) {
			GameCraftServices.craftSkillUpdateService().setMorphRecipe(player);
			// 你已达到可加入军团的等级。 / You reached the level where you can join a legion.
			// 使用军团搜索查找你想要的军团。 / Use the legion search to find the legion you want.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GUILD_CAN_JOIN_LEVEL);
		}
		// 烙印之石 5.1 / Stigma 5.1
		// 角色将按职业与等级获得“可充能烙印之石”礼包。 / Characters will receive "Chargeable Stigma" bundles based on their class and level.
		// http://static.ncsoft.com/aion/store/PatchNotes/AION_Patch_Notes_110916.pdf
		if (level == 20) {
			ItemService.addItem(player, 188053787, 1); // 烙印之石支援包。 / Stigma Support Bundle.
			// 额外普通烙印之石槽位现已可用。 / An additional normal Stigma slot is now available.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_OPEN_NORMAL_SLOT);
		}
		if (level == 30) {
			ItemService.addItem(player, 188053787, 2); // 烙印之石支援包。 / Stigma Support Bundle.
		}
		if (level == 40) {
			ItemService.addItem(player, 188053787, 3); // 烙印之石支援包。 / Stigma Support Bundle.
		}
		if (level == 45) {
			ItemService.addItem(player, 188053787, 3); // 烙印之石支援包。 / Stigma Support Bundle.
			ItemService.addItem(player, 188053785, 1); // Greater Stigma Bundle.
			// 额外高级烙印之石槽位现已可用。 / An additional Greater Stigma slot is now available.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_OPEN_ENHANCED1_SLOT);
		}
		if (level == 50) {
			ItemService.addItem(player, 188053787, 3); // 烙印之石支援包。 / Stigma Support Bundle.
			ItemService.addItem(player, 188053785, 2); // Greater Stigma Bundle.
		}
		if (level == 55) {
			ItemService.addItem(player, 188053787, 3); // 烙印之石支援包。 / Stigma Support Bundle.
			ItemService.addItem(player, 188053785, 2); // Greater Stigma Bundle.
			ItemService.addItem(player, 188053786, 1); // Major Stigma Bundle.
			// 额外大型烙印之石槽位现已可用。 / An additional Major Stigma slot is now available.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_OPEN_ENHANCED2_SLOT);
		}
		// 精华核心 5.3 / Essence Cores 5.3
		if (level == 66) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CPSTONE_OPEN_SLOT);
		}
		if (level == 68) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CPSTONE_OPEN_SLOT);
		}
		if (level == 70) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CPSTONE_OPEN_SLOT);
		}
		PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(player, player.getSkillList().getBasicSkills()));
		if (player.isInTeam()) {
			GameTaskManagerServices.teamEffectUpdater().startTask(player);
		}
		if (player.isLegionMember()) {
			GameCoreGameplayServices.legionService().updateMemberInfo(player);
		}

	/**
	 * 导师状态会在满足条件时自动取消。 / http://static.ncsoft.com/aion/store/PatchNotes/AION_Patch_Notes_061715.pdf Mentor status now cancels automatically as soon as the lowest level group member reaches level 51
	 */
		if (level == 51) {
			PlayerGroupService.stopMentoring(player);
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_BE_MENTEE_BY_LEVEL_LIMIT);
		}
		if (level == 66) { // Level 66 is already gated by the ArchDaeva mission in PlayerCommonData.setExp().
			player.getCommonData().setArchDaeva(true);
		}
		if (level >= 66 && level <= 83) {
			reachedPlayerLvl(player);
		}
		player.getNpcFactions().onLevelUp();
		GameCreativityServices.creativityEssenceService().pointPerLevel(player);
	}

	/**
	 * 玩家升级到达某等级时的广播/处理。
	 * Broadcasts/handles when a player reaches a level.
	 *
	 * leveling player
	 */
	public static final void reachedPlayerLvl(final Player player) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player players) {
				// “玩家名”已达到 %1 级。 / "Player Name" has reached level %1.
				byte playerLevel = player.getLevel();
				PacketSendUtility.sendPacket(players, new SM_SYSTEM_MESSAGE(1300086, player.getName(), playerLevel));
			}
		});
	}

	/**
	 * 启动出生/传送保护任务。
	 * Starts spawn/teleport protection task.
	 *
	 */
	public void startProtectionActiveTask() {
		if (!getOwner().isProtectionActive()) {
			TeleportService2.playerTransformation(getOwner());
			TeleportService2.instanceTransformation(getOwner());
			TeleportService2.archdaevaTransformation(getOwner());
			getOwner().setVisualState(CreatureVisualState.BLINKING);
			AttackUtil.cancelCastOn((Creature) getOwner());
			AttackUtil.removeTargetFrom((Creature) getOwner());
			PacketSendUtility.broadcastPacket(getOwner(), new SM_PLAYER_STATE(getOwner()), true);
			Future<?> task = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					stopProtectionActiveTask();
				}
			}, 60000);
			addTask(TaskId.PROTECTION_ACTIVE, task);
		}
	}

	/**
	 * 停止保护任务。
	 * Stops the protection task.
	 *
	 */
	public void stopProtectionActiveTask() {
		cancelTask(TaskId.PROTECTION_ACTIVE);
		Player player = getOwner();
		if (player != null && player.isSpawned()) {
			player.unsetVisualState(CreatureVisualState.BLINKING);
			PacketSendUtility.broadcastPacket(player, new SM_PLAYER_STATE(player), true);
			notifyAIOnMove();
		}
	}

	/**
	 * 飞行传送结束时的处理。
	 * Processing when fly teleport ends.
	 *
	 */
	public void onFlyTeleportEnd() {
		Player player = getOwner();
		if (player.isInPlayerMode(PlayerMode.WINDSTREAM)) {
			player.unsetPlayerMode(PlayerMode.WINDSTREAM);
			player.getLifeStats().triggerFpReduce();
			player.unsetState(CreatureState.FLYING);
			player.setState(CreatureState.ACTIVE);
			player.setState(CreatureState.GLIDING);
			player.setFlyState(2);
			player.getGameStats().updateStatsAndSpeedVisually();
		} else {
			player.unsetState(CreatureState.FLIGHT_TELEPORT);
			player.setFlightTeleportId(0);

			if (SecurityConfig.ENABLE_FLYPATH_VALIDATOR) {
				long diff = (System.currentTimeMillis() - player.getFlyStartTime());
				FlyPathEntry path = player.getCurrentFlyPath();

				if (player.getWorldId() != path.getEndWorldId()) {
					AuditLogger.info(player, "Player tried to use flyPath #" + path.getId() + " from not native start world " + player.getWorldId() + ". expected " + path.getEndWorldId());
				}

				if (diff < path.getTimeInMs()) {
					AuditLogger.info(player, "Player " + player.getName() + " used flypath bug " + diff + " instead of " + path.getTimeInMs());
				}
				// 修复：客户端已改动的飞行通行证无动画 / FIX no anime for fly pass that is changed in client:D
				if (diff < 5000) { // to check x_flipath file in client
					AuditLogger.info(player, "Flypath: " + path.getId() + " bug, time: " + (diff / 1000) + " Fly teleport less than 5 sec; Kick-");
					player.getClientConnection().close(new SM_QUIT_RESPONSE(), false);
				}
				
				player.setCurrentFlypath(null);
			}

			player.setFlightDistance(0);
			player.setState(CreatureState.ACTIVE);
			updateZone();
		}
	}

	/**
	 * 向玩家背包添加物品。
	 * Adds items to the player inventory.
	 *
	 * item id
	 * count
	 *
	 * @return whether successful / 是否成功 / whether successful。
	 */
	public boolean addItems(int itemId, int count) {
		return ItemService.addQuestItems(getOwner(), Collections.singletonList(new QuestItems(itemId, count)));
	}

	/**
	 * 开始姿态技能。
	 * Starts a stance skill.
	 *
	 * stance skill id
	 */
	public void startStance(final int skillId) {
		startStance(skillId, skillId == 0 ? 0 : 1);
	}

	public void startStance(final int skillId, final int type) {
		stance = skillId;
		stanceType = type;
	}

	/**
	 * 停止当前姿态。
	 * Stops the current stance.
	 *
	 */
	public void stopStance() {
		getOwner().getEffectController().removeEffect(stance);
		PacketSendUtility.sendPacket(getOwner(), new SM_PLAYER_STANCE(getOwner(), 0));
		stance = 0;
		stanceType = 0;
	}

	/**
	 * 获取当前姿态技能 ID。
	 * Gets the current stance skill id.
	 *
	 * @return stance skill id / 姿态技能 ID / stance skill id。
	 */
	public int getStanceSkillId() {
		return stance;
	}

	/**
	 * 是否处于姿态中。
	 * Whether currently under stance.
	 *
	 * @return 处于姿态则为 true / true if under stance
	 */
	public boolean isUnderStance() {
		return stance != 0;
	}

	public int getStanceType() {
		return stanceType;
	}

	/**
	 * 更新灵魂疾病效果。
	 * Updates soul sickness effect.
	 *
	 * skill id
	 */
	public void updateSoulSickness(int skillId) {
		Player player = getOwner();
		House house = player.getActiveHouse();
		if (house != null) {
			switch (house.getHouseType()) {
			case MANSION:
			case ESTATE:
			case PALACE:
				return;
			default:
				break;
			}
		}
		if (!player.havePermission(MembershipConfig.DISABLE_SOULSICKNESS)) {
			int deathCount = player.getCommonData().getDeathCount();
			if (deathCount < 10) {
				deathCount++;
				player.getCommonData().setDeathCount(deathCount);
			}
			if (skillId == 0) {
				skillId = 8291;
			}
			GameEngineServices.skillEngine().getSkill(player, skillId, deathCount, player).useSkill();
		}
	}

	/**
	 * 是否处于战斗状态。
	 * Whether currently in combat.
	 *
	 * @return 战斗中则为 true / true if in combat / true if the player is actively in combat
	 */
	public boolean isInCombat() {
		return (((System.currentTimeMillis() - lastAttackedMilis) <= 10000) || ((System.currentTimeMillis() - lastAttackMilis) <= 10000));
	}

	/**
	 * 是否有无死亡惩罚效果。
	 * Whether a no-death-penalty effect is active.
	 *
	 * @return true if active / 有效则为 true / true if active。
	 */
	public boolean isNoDeathPenaltyInEffect() {
		Iterator<Effect> iterator = getOwner().getEffectController().iterator();
		while (iterator.hasNext()) {
			Effect effect = (Effect) iterator.next();
			if (effect.isNoDeathPenalty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 是否有无死亡惩罚减免效果。
	 * Whether a no-death-penalty-reduce effect is active.
	 *
	 * @return true if active / 有效则为 true / true if active。
	 */
	public boolean isNoDeathPenaltyReduceInEffect() {
		Iterator<Effect> iterator = getOwner().getEffectController().iterator();
		while (iterator.hasNext()) {
			Effect effect = (Effect) iterator.next();
			if (effect.isNoDeathPenaltyReduce()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 是否有死亡惩罚减免效果。
	 * Whether a death-penalty-reduce effect is active.
	 *
	 * @return true if active / 有效则为 true / true if active。
	 */
	public boolean isDeathPenaltyReduceInEffect() {
		Iterator<Effect> iterator = getOwner().getEffectController().iterator();
		while (iterator.hasNext()) {
			Effect effect = (Effect) iterator.next();
			if (effect.isDeathPenaltyReduce()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 是否有无复活惩罚效果。
	 * Whether a no-resurrect-penalty effect is active.
	 *
	 * @return true if active / 有效则为 true / true if active。
	 */
	public boolean isNoResurrectPenaltyInEffect() {
		Iterator<Effect> iterator = getOwner().getEffectController().iterator();
		while (iterator.hasNext()) {
			Effect effect = (Effect) iterator.next();
			if (effect.isNoResurrectPenalty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 是否有 HiPass 效果。
	 * Whether a HiPass effect is active.
	 *
	 * @return true if active / 有效则为 true / true if active。
	 */
	public boolean isHiPassInEffect() {
		Iterator<Effect> iterator = getOwner().getEffectController().iterator();
		while (iterator.hasNext()) {
			Effect effect = (Effect) iterator.next();
			if (effect.isHiPass()) {
				return true;
			}
		}
		return false;
	}
}
