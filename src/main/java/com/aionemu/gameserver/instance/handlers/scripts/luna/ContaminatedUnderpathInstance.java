package com.aionemu.gameserver.instance.handlers.scripts.luna;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.ContaminatedUnderpathReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.ContaminatedUnderpathPlayerReward;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 污染暗道副本事件处理器。
 * 刷怪与原版阶段链完全由 {@code condition-spawns.xml} 与 AI pattern 负责，实例层只保留计时、计分、门、掉落与结算。
 * Instance event handler for Contaminated Underpath.
 * Spawning and the phase chain are owned by {@code condition-spawns.xml} and the retail AI patterns; the instance layer
 * only keeps the timer, score, doors, drops and settlement.
 *
 * @author Rinzler (Encom
 * @author MATTY (ADev Team
 */

@InstanceID(301630000)
public class ContaminatedUnderpathInstance extends GeneralInstanceHandler {
	/**
	 * 排名 / rank
	 */
	private int rank;
	/**
	 * 开始时间 / start time
	 */
	private long startTime;
	/**
	 * 准备计时器 / prepare timer
	 */
	private Future<?> timerPrepare;
	/**
	 * 门映射 / door map
	 */
	private Map<Integer, StaticDoor> doors;
	/**
	 * 准备计时秒数 / prepare timer seconds
	 */
	private final int prepareTimerSeconds = 180000; // 红色时间：等待副本开始 3 分钟。 / Red time: waiting 3 minutes for dungeon start.
	/**
	 * 副本奖励对象 / instance reward object
	 */
	private ContaminatedUnderpathReward instanceReward;

	protected ContaminatedUnderpathPlayerReward getPlayerReward(Integer object) {
		return (ContaminatedUnderpathPlayerReward) instanceReward.getPlayerReward(object);
	}

	@SuppressWarnings("unchecked")
	protected void addPlayerReward(Player player) {
		instanceReward.addPlayerReward(new ContaminatedUnderpathPlayerReward(player.getObjectId()));
	}

	/**
	 * 返回本副本奖励对象。
	 * Return this instance's reward object.
	 *
	 * @return 奖励对象 / reward object
	 */
	@Override
	public InstanceReward<?> getInstanceReward() {
		return instanceReward;
	}

	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * @param npc npc
	 */
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = DropRegistrationService.getInstance().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		switch (npcId) {
			case 703384: // 感染的骨头堆 / Infected Bone Mound.
				dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 182216109, 1)); // MAAD-S 臼齿 / Maad-S Molar.
				break;
			case 703385: // 感染的肉块 / Infected Flesh Lump.
				dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 182216110, 1)); // MAAD-S 皮肤组织 / Maad-S Skin Tissue.
				break;
			case 833866: // 不稳定的奥德能量 / Unstable Aether Energy.
				dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 182007405, 3)); // 明亮的奥德 / Bright Aether.
				break;
			case 834253: // Maedrunerk 军团宝藏 / Maedrunerk Legion Treasures.
				break;
		}
	}

	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(182007405, storage.getItemCountByItemId(182007405)); // 明亮的奥德 / Bright Aether.
	}

	/**
	 * 处理死亡事件：只记分与路障扣分，刷怪/阶段推进由原版 AI pattern 负责。
	 * Handle death events: score and barricade penalties only; spawning and phase progression belong to the retail AI patterns.
	 *
	 * @param npc npc
	 */
	@Override
	public void onDie(Npc npc) {
		int points = 0;
		int npcId = npc.getNpcId();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 243639: // IDLuna_ZombieMiddleageD_65_An
			case 243640: // IDLuna_ZombieMiddleageFD_65_An
			case 243641: // IDLuna_Zombie_65_Ae
			case 243647: // IDLuna_ZombieGiant_As_N_65_Ae / MAD-74C
				points = 50;
				break;
			case 243642: // IDLuna_YZombie_Fi_65_An
			case 243643: // IDLuna_YZombie_Wi_65_An
				points = 100;
				break;
			case 243645: // IDLuna_MutantBeast_Undead_65_An
			case 245565: // IDLuna_def_ZombieSpider_65_3rd
				points = 150;
				break;
			case 243644: // IDLuna_YZombie_Wi_65_Ae
			case 245545: // IDLuna_def_ZombieSpider_65_1st
			case 245555: // IDLuna_def_ZombieSpider_65_2nd
				points = 200;
				break;
			case 243646: // IDLuna_ZombieButcher_As_N_65_Ae
				points = 300;
				break;
			case 245551: // IDLuna_def_MutantBeast_65_1st
			case 245561: // IDLuna_def_MutantBeast_65_2nd
				points = 500;
				break;
			case 245547: // IDLuna_def_YZombie_65_1st
			case 245548: // IDLuna_def_YZombieF_65_1st
			case 245557: // IDLuna_def_YZombie_65_2nd
			case 245558: // IDLuna_def_YZombieF_65_2nd
				points = 600;
				break;
			case 245550: // IDLuna_def_MZombie_65_1st
			case 245552: // IDLuna_def_ZombieVampireD_65_1st
			case 245553: // IDLuna_def_ZombieVampireFD_65_1st
			case 245560: // IDLuna_def_MZombie_65_2nd
			case 245562: // IDLuna_def_ZombieVampireD_65_2nd
			case 245563: // IDLuna_def_ZombieVampireFD_65_2nd
			case 245572: // IDLuna_def_ZombieVampireD_65_3rd
			case 245573: // IDLuna_def_ZombieVampireFD_65_3rd
				points = 700;
				break;
			case 245554: // IDLuna_def_Lucky_65_1st
			case 245564: // IDLuna_def_Lucky_65_2nd
			case 245574: // IDLuna_def_Lucky_65_3rd
				points = 2000;
				break;
			case 245546: // IDLuna_def_ZombieButcher_As_N_65_1st
			case 245556: // IDLuna_def_ZombieButcher_As_N_65_2nd
				points = 2500;
				break;
			case 245549: // IDLuna_def_Orcshandura_65_1st
			case 245559: // IDLuna_def_Orcshandura_65_2nd
			case 245569: // IDLuna_def_Orcshandura_65_3rd
				points = 3500;
				break;
			case 245575: // MAAD-S 主体 / MAAD-S.
				points = 500000;
				// 原版由 Phasenpc_05/IDLUNA_DEF_END 关闭副本；实例层只负责结算计时，不补刷任何实体。
				// Retail closes the instance through Phasenpc_05 / IDLUNA_DEF_END; the instance only settles and stops the timer.
				ThreadPoolManager.getInstance().schedule(() -> instance.doOnAllPlayers(new Visitor<>() {
					@Override
					public void visit(Player player) {
						stopInstance(player);
					}
				}), 5000);
				break;
		}
		if (instanceReward.getInstanceScoreType().isStartProgress()) {
			instanceReward.addNpcKill();
			instanceReward.addPoints(points);
			sendPacket(npc.getObjectTemplate().getNameId(), points);
		}
		switch (npcId) {
			case 833863: // 前路障 / Frontal Barricade.
				// 前方路障被摧毁。 / The barricade in the front was destroyed.
				sendMsgByRace(1403609, Race.PC_ALL, 0);
				instanceReward.addPoints(-5000);
				break;
			case 833864: // 后路障 / Rear Barricade.
				// 后方路障被摧毁。阿特雷亚危在旦夕。 / The barricade in the back was destroyed. Atreia is in danger.
				sendMsgByRace(1403610, Race.PC_ALL, 0);
				instanceReward.addPoints(-5000);
				break;
		}
	}

	private int getTime() {
		long result = (int) (System.currentTimeMillis() - startTime);
		return 3600000 - (int) result;
	}

	private void sendPacket(final int nameId, final int point) {
		instance.doOnAllPlayers(new Visitor<>() {
			@Override
			public void visit(Player player) {
				if (nameId != 0) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400237, new DescriptionId(nameId * 2 + 1), point));
				}
				PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(getTime(), instanceReward, null));
			}
		});
	}

	private int checkRank(int totalPoints) {
		if (totalPoints >= 549000) { // S 级 / Rank S.
			rank = 1;
		} else if (totalPoints >= 544000) { // A 级 / Rank A.
			rank = 2;
		} else if (totalPoints >= 50) { // B / C / D 级 / Rank B/C/D.
			rank = 3;
		} else {
			rank = 6;
		}
		return rank;
	}

	/**
	 * 玩家打开门时处理：开门即开始副本计时并启动原版时间攻击链。
	 * Handle a player opening a door: the open triggers the instance timer and the retail time-attack chain.
	 *
	 * @param player 玩家 / player
	 * @param doorId doorId
	 */
	@Override
	public void onOpenDoor(Player player, int doorId) {
		if (doorId == 28) {
			startMainInstanceTimer();
			doors.get(28).setOpen(true);
			sendMsgByRace(1403696, Race.PC_ALL, 0);
		}
	}

	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(final Player player) {
		if (!instanceReward.containPlayer(player.getObjectId())) {
			addPlayerReward(player);
		}
		ContaminatedUnderpathPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (playerReward.isRewarded()) {
			doReward(player);
		}
		startPrepareTimer();
		final int lunaDetachment = player.getRace() == Race.ASMODIANS ? 21346 : 21345;
		SkillEngine.getInstance().applyEffectDirectly(lunaDetachment, player, player, 3000000);
	}

	private void startPrepareTimer() {
		if (timerPrepare == null) {
			timerPrepare = ThreadPoolManager.getInstance().schedule(this::startMainInstanceTimer, prepareTimerSeconds);
		}
		instance.doOnAllPlayers(new Visitor<>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(prepareTimerSeconds, instanceReward, null));
			}
		});
	}

	private void startMainInstanceTimer() {
		if (timerPrepare != null && !timerPrepare.isDone()) {
			timerPrepare.cancel(false);
		}
		if (startTime != 0) {
			return;
		}
		startTime = System.currentTimeMillis();
		instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		// 原版时间攻击从副本计时开始；该变量驱动阶段触发器与常驻怪物条件刷出。
		// Retail starts the time attack with the instance timer; the variable drives phase triggers and ambient spawns.
		RetailConditionSpawnEngine.setVariable(instance, "TIMEATTACK_PLAY_START", 1, 0);
		sendPacket(0, 0);
	}

	protected void stopInstance(Player player) {
		instanceReward.setRank(6);
		instanceReward.setRank(checkRank(instanceReward.getPoints()));
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		doReward(player);
		sendPacket(0, 0);
	}

	/**
	 * 结算并发放奖励。
	 * Settle and grant rewards.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void doReward(Player player) {
		ContaminatedUnderpathPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (!playerReward.isRewarded()) {
			playerReward.setRewarded();
			int contaminatedRank = instanceReward.getRank();
			switch (contaminatedRank) {
				case 1: // S 级 / Rank S
					playerReward.setContaminatedPremiumRewardBundle(1);
					// 污染高级奖励包。 / Contaminated Premium Reward Bundle.
					ItemService.addItem(player, 188055598, 1);
					break;
				case 2: // A 级 / Rank A
					playerReward.setContaminatedHighestRewardBundle(1);
					// 污染最高奖励包。 / Contaminated Highest Reward Bundle.
					ItemService.addItem(player, 188055599, 1);
					break;
				case 3: // B 级 / Rank B
				case 4: // C 级 / Rank C
				case 5: // D 级 / Rank D
					playerReward.setContaminatedUnderpathSpecialPouch(1);
					// 真端 B 档仅发放 b_bonus_item1。 / Retail rank B grants only b_bonus_item1.
					ItemService.addItem(player, 188057960, 1);
					break;
			}
		}
	}

	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		instanceReward = new ContaminatedUnderpathReward(mapId, instanceId);
		instanceReward.setInstanceScoreType(InstanceScoreType.PREPARING);
		doors = instance.getDoors();
	}

	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
	public void onInstanceDestroy() {
		if (timerPrepare != null) {
			timerPrepare.cancel(false);
		}
		instanceReward.clear();
		doors.clear();
	}

	/**
	 * 玩家从该副本登出时处理。
	 * Handle a player logging out from this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
		removeEffects(player);
	}

	/**
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
		removeEffects(player);
	}

	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		effectController.removeEffect(21345);
		effectController.removeEffect(21346);
		effectController.removeEffect(22741);
	}

	protected void sendMsgByRace(final int msg, final Race race, int time) {
		ThreadPoolManager.getInstance().schedule(() -> instance.doOnAllPlayers(new Visitor<>() {
			@Override
			public void visit(Player player) {
				if (player.getRace().equals(race) || race.equals(Race.PC_ALL)) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msg));
				}
			}
		}), time);
	}
}
