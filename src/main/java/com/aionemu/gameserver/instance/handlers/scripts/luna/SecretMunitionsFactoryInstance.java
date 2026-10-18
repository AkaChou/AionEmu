package com.aionemu.gameserver.instance.handlers.scripts.luna;

import com.aionemu.commons.utils.Rnd;
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
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.SecretMunitionsFactoryReward;
import com.aionemu.gameserver.model.instance.playerreward.SecretMunitionsFactoryPlayerReward;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 秘密军工厂副本事件处理器。
 * 刷怪与原版阶段链完全由 {@code condition-spawns.xml} 与 AI pattern 负责，实例层只保留计时、计分、门、掉落与结算。
 * Instance event handler for Secret Munitions Factory.
 * Spawning and the phase chain are owned by {@code condition-spawns.xml} and the retail AI patterns; the instance layer
 * only keeps the timer, score, doors, drops and settlement.
 *
 * @author Encom
 */

@InstanceID(301640000)
public class SecretMunitionsFactoryInstance extends GeneralInstanceHandler {
	/**
	 * 军阶 / rank
	 */
	private int rank;
	/**
	 * 开始时间 / start time
	 */
	private long startTime;
	/**
	 * 准备计时器 / timer prepare
	 */
	private Future<?> timerPrepare;
	/**
	 * 副本是否已销毁 / whether the instance is destroyed
	 */
	private boolean isInstanceDestroyed;
	/**
	 * 门映射 / door map
	 */
	private Map<Integer, StaticDoor> doors;
	/**
	 * 准备计时秒数 / prepare timer seconds
	 */
	private final int prepareTimerSeconds = 120000; // 红色时间：等待副本开始 2 分钟（对齐真端 wait_time=120）。 / Waiting 2 minutes for dungeon start.
	/**
	 * 副本奖励对象 / instance reward object
	 */
	private SecretMunitionsFactoryReward instanceReward;
	/**
	 * factory task2 / factory task2
	 */
	private final List<Future<?>> factoryTask2 = new ArrayList<>();

	/**
	 * 返回玩家奖励记录。
	 * Return the player's reward record.
	 *
	 * @param object 可见对象 / visible object
	 * @return 奖励记录 / result
	 */
	protected SecretMunitionsFactoryPlayerReward getPlayerReward(Integer object) {
		return (SecretMunitionsFactoryPlayerReward) instanceReward.getPlayerReward(object);
	}

	@SuppressWarnings("unchecked")
	protected void addPlayerReward(Player player) {
		instanceReward.addPlayerReward(new SecretMunitionsFactoryPlayerReward(player.getObjectId()));
	}

	/**
	 * 返回本副本奖励对象。
	 * Return this instance's reward object.
	 *
	 * @return 奖励记录 / result
	 */
	@Override
	public InstanceReward<?> getInstanceReward() {
		return instanceReward;
	}

	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * @param npc NPC / npc
	 */
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = DropRegistrationService.getInstance().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		switch (npcId) {
			case 245185: // 机械图尔克的核心 / Mechaturerk's Core.
				switch (Rnd.get(1, 7)) {
					case 1:
						dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 152150000, 2)); // 未切割的水晶 / Uncut Crystal.
						break;
					case 2:
						dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 152150001, 2)); // 有缺口的水晶 / Chipped Crystal.
						break;
					case 3:
						dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 152150002, 2)); // 浑浊的水晶 / Cloudy Crystal.
						break;
					case 4:
						dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 152150003, 2)); // 透明的水晶 / Clear Crystal.
						break;
					case 5:
						dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 152150004, 2)); // 无瑕的水晶 / Flawless Crystal.
						break;
					case 6:
						dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 152150005, 2)); // 露娜之光 / Luna's Light.
						break;
					case 7:
						dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 152150006, 2)); // 露娜的祝福 / Luna's Blessing.
						break;
				}
				break;
			case 834443: // 机械图尔克的宝物箱 / Mechaturerk's Treasure Box.
			case 834444: // 机械图尔克的特殊宝物箱 / Mechaturerk's Special Treasure Box.
				break;
		}
	}

	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(164000418, storage.getItemCountByItemId(164000418)); // 臭气弹 / Stink Bomb.
		storage.decreaseByItemId(164002362, storage.getItemCountByItemId(164002362)); // 机械图尔克油桶 / Mechaturerk Oil Cask.
	}

	/**
	 * 处理死亡事件：只记分与结算，刷怪/阶段推进由原版 AI pattern 负责。
	 * Handle death events: score and settlement only; spawning and phase progression belong to the retail AI patterns.
	 *
	 * @param npc NPC / npc
	 */
	@Override
	public void onDie(Npc npc) {
		int points = 0;
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 244147: // 最终首领 / Final boss (IDLUNA_SIEGE_S2_Nepilim_Fi_N_Ah_02).
				// 真端 npc_scores.xml 仅 244147 计 878600；243664（Boss_Figure 召唤的傀儡）无分值，击杀不结算。
				// Retail npc_scores.xml scores only 244147 at 878600; 243664 (the Boss_Figure decoy) has no score
				// and must not end the instance.
				points = 878600;
				ThreadPoolManager.getInstance().schedule(() -> instance.doOnAllPlayers(new Visitor<>() {
					@Override
					public void visit(Player player) {
						stopInstance(player);
					}
				}), 5000);
				break;
			case 243968: // 雷米鲁内克 / Remirunerk (IDLUNA_SIEGE_S1_Shulack_Ri_SN_Ae_02).
				points = 678600;
				break;
			case 243969: // 波米鲁内克 / Bomirunrunerk (IDLUNA_SIEGE_S1_Shulack_Ri_SN_Ae_03).
				points = 478600;
				break;
		}
		if (instanceReward.getInstanceScoreType().isStartProgress()) {
			instanceReward.addNpcKill();
			instanceReward.addPoints(points);
			sendPacket(npc.getObjectTemplate().getNameId(), points);
		}
	}

	/**
	 * 玩家对 NPC 使用物品完成时处理。
	 * Handle item-use finish on an NPC.
	 *
	 * @param player 玩家 / player
	 * @param npc NPC / npc
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 833833: // 炸弹箱 / Bomb Chest.
				if (player.getInventory().isFull()) {
					sendMsgByRace(1390149, Race.PC_ALL, 0);
				}
				ItemService.addItem(player, 164000418, 1); // 臭气弹 / Stink Bomb.
				break;
			case 703349: // 治疗舱 / Health Pod.
				despawnNpc(npc);
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.HP, 50000);
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.MP, 50000);
				break;
			case 243660: // 油桶 / Oil Cask.
				despawnNpc(npc);
				if (player.getInventory().isFull()) {
					sendMsgByRace(1390149, Race.PC_ALL, 0);
				}
				ItemService.addItem(player, 164002362, 5); // 机械图尔克油桶 / Mechaturerk Oil Cask.
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
		if (totalPoints >= 878600) {
			int elapsedSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
			if (elapsedSeconds <= 780) { // S 级：耗时 <= 13分钟 (780s) / Rank S: <= 13 mins.
				rank = 1;
			} else if (elapsedSeconds <= 1140) { // A 级：耗时 <= 19分钟 (1140s) / Rank A: <= 19 mins.
				rank = 2;
			} else if (elapsedSeconds <= 5000) { // B / C / D 级 / Rank B/C/D.
				rank = 3;
			} else {
				rank = 6;
			}
		} else {
			rank = 6;
		}
		return rank;
	}

	/**
	 * 副本计时器：一小时总时长。
	 * Instance timer: one hour total duration.
	 */
	protected void startFactoryTask2() {
		factoryTask2.add(ThreadPoolManager.getInstance().schedule(() -> instance.doOnAllPlayers(new Visitor<>() {
			@Override
			public void visit(Player player) {
				stopInstance(player);
			}
		}), 3600000)); // 1 小时 / 1 Hour.
	}

	/**
	 * 玩家打开门时处理：开门即开始副本计时并启动原版时间攻击链。
	 * Handle a player opening a door: the open triggers the instance timer and the retail time-attack chain.
	 *
	 * @param player 玩家 / player
	 * @param doorId 门 id / doorId
	 */
	@Override
	public void onOpenDoor(Player player, int doorId) {
		if (doorId == 27) {
			startFactoryTask2();
			doors.get(27).setOpen(true);
			killNpc(getNpcs(833868)); // 岩石堆 / Rock Pile.
			if ((timerPrepare != null) && (!timerPrepare.isDone() || !timerPrepare.isCancelled())) {
				startMainInstanceTimer();
			}
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
		SecretMunitionsFactoryPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (playerReward.isRewarded()) {
			doReward(player);
		}
		startPrepareTimer();
		final int lunaDetachment = player.getRace() == Race.ASMODIANS ? 21348 : 21347;
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
		// 原版时间攻击从副本计时开始；该变量驱动联盟小队、魔像链与炸弹链的条件刷出。
		// Retail starts the time attack with the instance timer; the variable drives ally squad, golem and bomb spawns.
		RetailConditionSpawnEngine.setVariable(instance, "TIMEATTACK_PLAY_START", 1, 0);
		sendPacket(0, 0);
	}

	protected void stopInstance(Player player) {
		stopInstanceTask2();
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
		SecretMunitionsFactoryPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (!playerReward.isRewarded()) {
			playerReward.setRewarded();
			int factoryRank = instanceReward.getRank();
			switch (factoryRank) {
				case 1: // S 级 / Rank S
					playerReward.setMechaturerkSecretBox(1);
					// 机械图尔克的特殊宝箱。 / Mechaturerk's Secret Box.
					ItemService.addItem(player, 188055475, 1);
					break;
				case 2: // A 级 / Rank A
					playerReward.setMechaturerkNormalTreasureChest(1);
					// 机械图尔克的最高宝物箱（对齐真端 item1_id）。 / Mechaturerk's Highest Treasure Box.
					ItemService.addItem(player, 188057961, 1);
					break;
				case 3: // B 级 / Rank B
				case 4: // C 级 / Rank C
				case 5: // D 级 / Rank D
					playerReward.setMechaturerkSpecialTreasureBox(1);
					// 机械图尔克的普通宝物箱（对齐真端 b_bonus_item1）。 / Mechaturerk's Normal Treasure Chest.
					ItemService.addItem(player, 188055647, 1);
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
		instanceReward = new SecretMunitionsFactoryReward(mapId, instanceId);
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
		stopInstanceTask2();
		isInstanceDestroyed = true;
		instanceReward.clear();
		doors.clear();
	}

	private void stopInstanceTask2() {
		for (Future<?> task : factoryTask2) {
			if (task != null) {
				task.cancel(true);
			}
		}
	}

	protected void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}

	protected void killNpc(List<Npc> npcs) {
		if (npcs == null) {
			return;
		}
		for (Npc npc : npcs) {
			npc.getController().die();
		}
	}

	protected List<Npc> getNpcs(int npcId) {
		if (!isInstanceDestroyed) {
			return instance.getNpcs(npcId);
		}
		return null;
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
		effectController.removeEffect(21347);
		effectController.removeEffect(21348);
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
