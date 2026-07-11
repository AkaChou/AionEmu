package com.aionemu.gameserver.services;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.Future;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.DuelResult;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.summons.SummonMode;
import com.aionemu.gameserver.model.summons.UnsummonType;
import com.aionemu.gameserver.model.templates.zone.ZoneType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DUEL;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.summons.SummonsService;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 决斗服务，管理玩家间决斗请求、开战、超时与胜负结算。
 * Duel service managing player duel requests, start, timeout and result resolution.
 */
@Slf4j
public class DuelService {
	private static volatile ObjectProvider<DuelService> instanceProvider;

	/** 玩家对象 ID 决斗配对映射 / Duel pair map of player object ids */
	private Map<Integer, Integer> duels;
	/** 决斗超时任务映射。 / Duel timeout task map. */
	private Map<Integer, Future<?>> timeOutTask;

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static final DuelService getInstance() {
		ObjectProvider<DuelService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<DuelService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 构造服务并初始化决斗映射。
	 * Constructs the service and initializes duel maps.
	 */
	public DuelService() {
		this.duels = new ConcurrentHashMap<Integer, Integer>();
		timeOutTask = new ConcurrentHashMap<Integer, Future<?>>();
	}

	/**
	 * 处理决斗请求：校验区域与状态后向对方发送确认框。
	 * Handles a duel request: validates zone/state then sends confirmation to the responder.
	 *
	 * requester
	 * responder
	 */
	public void onDuelRequest(Player requester, Player responder) {
		if (requester.isInsideZoneType(ZoneType.PVP) || responder.isInsideZoneType(ZoneType.PVP)) {
			PacketSendUtility.sendPacket(requester, SM_SYSTEM_MESSAGE.STR_DUEL_PARTNER_INVALID(responder.getName()));
			return;
		}
		if (isDueling(requester.getObjectId()) || isDueling(responder.getObjectId())) {
			PacketSendUtility.sendPacket(requester, SM_SYSTEM_MESSAGE.STR_DUEL_HE_REJECT_DUEL(responder.getName()));
			return;
		}
		for (ZoneInstance zone : responder.getPosition().getMapRegion().getZones(responder)) {
			if (((!zone.isOtherRaceDuelsAllowed()) && (!responder.getRace().equals(requester.getRace())))
					|| ((!zone.isSameRaceDuelsAllowed()) && (responder.getRace().equals(requester.getRace())))) {
				PacketSendUtility.sendPacket(requester, SM_SYSTEM_MESSAGE.STR_MSG_DUEL_CANT_IN_THIS_ZONE);
				return;
			}
		}
		RequestResponseHandler rrh = new RequestResponseHandler(requester) {
			@Override
			public void denyRequest(Creature requester, Player responder) {
				rejectDuelRequest((Player) requester, responder);
			}

			@Override
			public void acceptRequest(Creature requester, Player responder) {
				startDuel((Player) requester, responder);
			}
		};
		responder.getResponseRequester().putRequest(SM_QUESTION_WINDOW.STR_DUEL_DO_YOU_ACCEPT_REQUEST, rrh);
		PacketSendUtility.sendPacket(responder,
				new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_DUEL_DO_YOU_ACCEPT_REQUEST, 0, 0, requester.getName()));
		PacketSendUtility.sendPacket(responder, SM_SYSTEM_MESSAGE.STR_DUEL_REQUESTED(requester.getName()));
	}

	/**
	 * 向发起者弹出“是否撤回决斗请求”确认。
	 * Shows the requester a withdraw-duel confirmation dialog.
	 *
	 * requester
	 * responder
	 */
	public void confirmDuelWith(Player requester, Player responder) {
		if (requester.isEnemy(responder)) {
			return;
		}
		RequestResponseHandler rrh = new RequestResponseHandler(responder) {
			@Override
			public void denyRequest(Creature requester, Player responder) {
			}

			@Override
			public void acceptRequest(Creature requester, Player responder) {
				cancelDuelRequest(responder, (Player) requester);
			}
		};
		requester.getResponseRequester().putRequest(SM_QUESTION_WINDOW.STR_DUEL_DO_YOU_WITHDRAW_REQUEST, rrh);
		PacketSendUtility.sendPacket(requester,
				new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_DUEL_DO_YOU_WITHDRAW_REQUEST, 0, 0, responder.getName()));
		PacketSendUtility.sendPacket(requester, SM_SYSTEM_MESSAGE.STR_DUEL_REQUEST_TO_PARTNER(responder.getName()));
	}

	/**
	 * 拒绝决斗请求并通知双方。
	 * Rejects a duel request and notifies both players.
	 *
	 * requester
	 * responder
	 */
	private void rejectDuelRequest(Player requester, Player responder) {
		PacketSendUtility.sendPacket(requester, SM_SYSTEM_MESSAGE.STR_DUEL_HE_REJECT_DUEL(responder.getName()));
		PacketSendUtility.sendPacket(responder, SM_SYSTEM_MESSAGE.STR_DUEL_REJECT_DUEL(requester.getName()));
	}

	/**
	 * 撤回决斗请求并通知双方。
	 * Cancels a duel request and notifies both players.
	 *
	 * owner cancelling
	 * target
	 */
	private void cancelDuelRequest(Player owner, Player target) {
		PacketSendUtility.sendPacket(target, SM_SYSTEM_MESSAGE.STR_DUEL_REQUESTER_WITHDRAW_REQUEST(owner.getName()));
		PacketSendUtility.sendPacket(owner, SM_SYSTEM_MESSAGE.STR_DUEL_WITHDRAW_REQUEST(target.getName()));
	}

	/**
	 * 正式开始决斗：发包、广播并创建超时任务。
	 * Starts the duel: sends packets, broadcasts and creates a timeout task.
	 *
	 * requester
	 * responder
	 */
	private void startDuel(final Player requester, final Player responder) {
		PacketSendUtility.sendPacket(requester, SM_DUEL.SM_DUEL_STARTED(responder.getObjectId()));
		PacketSendUtility.sendPacket(responder, SM_DUEL.SM_DUEL_STARTED(requester.getObjectId()));
		startDuelMsg(requester, responder);
		createDuel(requester.getObjectId(), responder.getObjectId());
		createTask(requester, responder);
	}

	/**
	 * 向附近玩家广播决斗开始消息。
	 * Broadcasts duel-start message to nearby players.
	 *
	 * player 1
	 * player 2
	 */
	private void startDuelMsg(final Player player1, final Player player2) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player object) {
				if (MathUtil.isInRange(player1, object, 100)) {
					// %0 与 %1 的决斗已开始。 / A duel between %0 and %1 has started.
					PacketSendUtility.sendPacket(object,
							SM_SYSTEM_MESSAGE.STR_DUEL_START_BROADCAST(player2.getName(), player1.getName()));
				}
			}
		});
	}

	/**
	 * 向附近玩家广播决斗胜负消息。
	 * Broadcasts duel-lose message to nearby players.
	 *
	 * loser
	 * winner
	 */
	private void loseDuelMsg(final Player player1, final Player player2) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player object) {
				if (MathUtil.isInRange(player1, object, 100)) {
					// %0 在决斗中击败了 %1。 / %0 defeated %1 in a duel.
					PacketSendUtility.sendPacket(object,
							SM_SYSTEM_MESSAGE.STR_DUEL_STOP_BROADCAST(player2.getName(), player1.getName()));
				}
			}
		});
	}

	/**
	 * 向附近玩家广播决斗平局消息。
	 * Broadcasts duel-draw message to nearby players.
	 *
	 * player 1
	 * player 2
	 */
	private void drawDuelMsg(final Player player1, final Player player2) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player object) {
				if (MathUtil.isInRange(player1, object, 100)) {
					// %0 与 %1 的决斗平局。 / The duel between %0 and %1 was a draw.
					PacketSendUtility.sendPacket(object,
							SM_SYSTEM_MESSAGE.STR_DUEL_TIMEOUT_BROADCAST(player2.getName(), player1.getName()));
				}
			}
		});
	}

	/**
	 * 处理决斗失败：清理 debuff/召唤物并通知胜负结果。
	 * Handles duel loss: cleans debuffs/summons and notifies win/lose results.
	 *
	 * losing player
	 */
	public void loseDuel(Player player) {
		if (!isDueling(player.getObjectId())) {
			return;
		}
		int opponnentId = duels.get(player.getObjectId());
		Player opponent = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(opponnentId);
		if (opponent != null) {
			opponent.getEffectController().removeAbnormalEffectsByTargetSlot(SkillTargetSlot.DEBUFF);
			opponent.getController().cancelCurrentSkill();
			if (player.getSummon() != null) {
				SummonsService.doMode(SummonMode.GUARD, player.getSummon(), UnsummonType.UNSPECIFIED);
			}
			if (opponent.getSummon() != null) {
				SummonsService.doMode(SummonMode.GUARD, opponent.getSummon(), UnsummonType.UNSPECIFIED);
			}
			if (player.getSummonedObj() != null) {
				player.getSummonedObj().getController().cancelCurrentSkill();
			}
			if (opponent.getSummonedObj() != null) {
				opponent.getSummonedObj().getController().cancelCurrentSkill();
			}
			loseDuelMsg(player, opponent);
			PacketSendUtility.sendPacket(opponent, new SM_QUEST_ACTION(0, 0));
			PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 0));
			PacketSendUtility.sendPacket(opponent, SM_DUEL.SM_DUEL_RESULT(DuelResult.DUEL_YOU_WIN, player.getName()));
			PacketSendUtility.sendPacket(player, SM_DUEL.SM_DUEL_RESULT(DuelResult.DUEL_YOU_LOSE, opponent.getName()));
		}
		removeDuel(player.getObjectId(), opponnentId);
	}

	/**
	 * 竞技场决斗失败时的轻量清理（不广播普通决斗结果）。
	 * Lightweight arena duel loss cleanup without normal duel result broadcast.
	 *
	 * losing player
	 */
	public void loseArenaDuel(Player player) {
		if (!isDueling(player.getObjectId())) {
			return;
		}
		player.getEffectController().removeAbnormalEffectsByTargetSlot(SkillTargetSlot.DEBUFF);
		player.getController().cancelCurrentSkill();
		int opponnentId = duels.get(player.getObjectId());
		Player opponent = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(opponnentId);
		if (opponent != null) {
			opponent.getEffectController().removeAbnormalEffectsByTargetSlot(SkillTargetSlot.DEBUFF);
			opponent.getController().cancelCurrentSkill();
		}
		removeDuel(player.getObjectId(), opponnentId);
	}

	/**
	 * 创建 5 分钟决斗超时任务。
	 * Creates a 5-minute duel timeout task.
	 *
	 * requester
	 * responder
	 */
	private void createTask(final Player requester, final Player responder) {
		Future<?> task = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			public void run() {
				if (isDueling(requester.getObjectId(), responder.getObjectId())) {
					drawDuelMsg(requester, responder);
					PacketSendUtility.sendPacket(requester,
							SM_DUEL.SM_DUEL_RESULT(DuelResult.DUEL_TIMEOUT, requester.getName()));
					PacketSendUtility.sendPacket(responder,
							SM_DUEL.SM_DUEL_RESULT(DuelResult.DUEL_TIMEOUT, responder.getName()));
					DuelService.this.removeDuel(requester.getObjectId(), responder.getObjectId());
				}
			}
		}, 5 * 60 * 1000);
		PacketSendUtility.sendPacket(requester, new SM_QUEST_ACTION(0, 300));
		PacketSendUtility.sendPacket(responder, new SM_QUEST_ACTION(0, 300));
		timeOutTask.put(requester.getObjectId(), task);
		timeOutTask.put(responder.getObjectId(), task);
	}

	/**
	 * 判断玩家是否处于决斗中。
	 * Returns whether the player is currently dueling.
	 *
	 * player object id
	 *
	 * @param playerObjId @return 决斗中返回 true / true if dueling
	 */
	public boolean isDueling(int playerObjId) {
		return duels.containsKey(playerObjId) && duels.containsValue(playerObjId);
	}

	/**
	 * 判断两名玩家是否正在互相对决。
	 * Returns whether the two players are currently dueling each other.
	 *
	 * player object id
	 * target object id
	 *
	 * @return 对决中返回 true / true if paired
	 */
	public boolean isDueling(int playerObjId, int targetObjId) {
		return duels.containsKey(playerObjId) && duels.get(playerObjId) == targetObjId;
	}

	/**
	 * 建立双方决斗映射。
	 * Creates bidirectional duel mapping for both players.
	 *
	 * requester object id
	 * responder object id
	 */
	public void createDuel(int requesterObjId, int responderObjId) {
		duels.put(requesterObjId, responderObjId);
		duels.put(responderObjId, requesterObjId);
	}

	/**
	 * 移除决斗映射并取消超时任务。
	 * Removes duel mapping and cancels timeout tasks.
	 *
	 * requester object id
	 * responder object id
	 */
	private void removeDuel(int requesterObjId, int responderObjId) {
		duels.remove(requesterObjId);
		duels.remove(responderObjId);
		removeTask(requesterObjId);
		removeTask(responderObjId);
	}

	/**
	 * 取消并移除玩家的超时任务。
	 * Cancels and removes the timeout task for the player.
	 *
	 * player object id
	 */
	private void removeTask(int playerId) {
		Future<?> task = timeOutTask.get(playerId);
		if (task != null && !task.isDone()) {
			task.cancel(true);
			timeOutTask.remove(playerId);
		}
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final DuelService instance = new DuelService();
	}
}
