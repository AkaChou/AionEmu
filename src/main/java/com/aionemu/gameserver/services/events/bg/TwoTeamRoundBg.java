package com.aionemu.gameserver.services.events.bg;

import com.aionemu.gameserver.services.events.LadderService;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * 多回合双队战场公共骨架：死亡判定、回合推进与终局结算流程。
 * Shared skeleton for multi-round two-team battlegrounds: death detection, round progression and final settlement.
 * <p>子类通过 {@link #isAfkCountedAsDead()}、{@link #tournamentMatchLength()} 与 {@link #rewardPlayer} 定制语义分叉。</p>
 */
public abstract class TwoTeamRoundBg extends Battleground {

	/**
	 * 结算排名：平局、胜方、败方。
	 * Settlement rank: draw, winner or loser.
	 */
	protected enum MatchRank {
		/** 平局 / draw */
		DRAW,
		/** 胜方 / winner */
		WINNER,
		/** 败方 / loser */
		LOSER
	}

	/**
	 * 队伍索引 → 回合胜场。 / Team index → round wins.
	 */
	protected final Map<Integer, Integer> roundResults = new HashMap<>();
	/**
	 * 最大回合数。 / Maximum rounds.
	 */
	protected int maxRounds = 3;
	/**
	 * 已完成回合数。 / Rounds completed.
	 */
	protected int roundsDone = 0;
	/**
	 * 是否已触发结束。 / Whether end already called.
	 */
	protected boolean endCalled = false;

	/**
	 * 判定回合胜负时是否把挂机玩家计入“已死亡”。
	 * Whether AFK players count as dead when detecting the round winner.
	 */
	protected boolean isAfkCountedAsDead() {
		return false;
	}

	/**
	 * 锦标赛对局时长（秒）。 / Tournament match length in seconds.
	 */
	protected abstract int tournamentMatchLength();

	/**
	 * 按结算排名发放奖励。
	 * Grants rewards for the given settlement rank.
	 *
	 * @param pl   玩家 / player
	 * @param rank 结算排名 / settlement rank
	 */
	protected abstract void rewardPlayer(Player pl, MatchRank rank);

	/**
	 * 处理死亡：检测回合胜负。
	 * Handles death: detect round winner.
	 *
	 * @param player       死亡玩家 / dead player
	 * @param lastAttacker 最后攻击者 / last attacker
	 */
	public void onDie(final Player player, Creature lastAttacker) {
		super.onDieDefault(player, lastAttacker);
		if (lastAttacker instanceof Player killer && !killer.getObjectId().equals(player.getObjectId())) {
			if (killer.getPlayerGroup2() != null && player.getPlayerGroup2() != null) {
				int deadCounter = 0;
				for (Player pl : player.getPlayerGroup2().getMembers()) {
					if (pl.getLifeStats().isAlreadyDead() || pl.isAfk()) {
						deadCounter++;
					}
				}
				if (deadCounter >= player.getPlayerGroup2().size()) {
					endTwoTeamMatch(false);
				}
			}
		}
	}

	@Override
	public boolean createTournament(List<List<Player>> teams) {
		if (!super.createGroups(teams)) {
			return false;
		}
		this.maxRounds = 5;
		this.description = "Your must defeat the opposing team. There is " + maxRounds
			+ " rounds. The team with the most won round, win's !!!";
		this.matchLength = tournamentMatchLength();
		startMatch();
		return true;
	}

	/**
	 * 处理玩家离开多回合双队战场。
	 * Handles a player leaving a multi-round two-team BG.
	 *
	 * @param player   玩家 / player
	 * @param isLogout 是否登出 / whether logout
	 * @param isAfk    是否挂机 / whether AFK
	 */
	public void onLeave(Player player, boolean isLogout, boolean isAfk) {
		super.onLeaveDefault(player, isLogout, isAfk);
		for (PlayerGroup group : super.getGroups()) {
			if (group.isEmpty()) {
				endTwoTeamMatch(false);
				break;
			}
		}
	}

	/**
	 * 开始新回合：重置并重新布置双方。
	 * Starts a new round: reset and re-position both teams.
	 */
	protected void startNewRound() {
		super.setStartStamp(System.currentTimeMillis());
		synchronized (super.getGroups()) {
			for (final PlayerGroup group : super.getGroups()) {
				for (final Player pl : group.getMembers()) {
					ThreadPoolManager.getInstance().schedule(() -> {
						if (!pl.isAfk()) {
							preparePlayer(pl, 8000, false);
							SpawnPosition pos = getSpawnPositions().get(group.getBgIndex());
							if (pos != null) {
								TeleportService2.teleportTo(pl, getMapId(), pos.getX(), pos.getY(), pos.getZ());
							}
						}
					}, 5000);
				}
			}
		}
		for (Player pl : super.getSpectators()) {
			super.createTimer(pl, getMatchLength());
		}
		super.setExpireTask(ThreadPoolManager.getInstance().schedule(() -> endTwoTeamMatch(true), getMatchLength() * 1000L));
		super.startBackgroundTask();
		ThreadPoolManager.getInstance().schedule(() -> {
			endCalled = false;
			for (PlayerGroup group : getGroups()) {
				if (group.isEmpty()) {
					endTwoTeamMatch(false);
				}
			}
		}, 1000);
	}

	/**
	 * 为指定小队累计回合胜场。
	 * Adds a round win for the given group.
	 *
	 * @param group 胜方小队 / winning group
	 */
	private void addRoundWin(PlayerGroup group) {
		if (group == null) {
			return;
		}
		roundResults.merge(group.getBgIndex(), 1, Integer::sum);
	}

	/**
	 * 判定本回合胜方小队。
	 * Determines the current round winning group.
	 *
	 * @return 回合胜方小队 / winning group
	 */
	private PlayerGroup getRoundWinner() {
		PlayerGroup winner = null;
		Map<PlayerGroup, Integer> deadCounts = new HashMap<>();
		boolean countAfkAsDead = isAfkCountedAsDead();
		for (PlayerGroup group : super.getGroups()) {
			int deadCounter = 0;
			for (Player pl : group.getMembers()) {
				if (pl.getLifeStats().isAlreadyDead() || (countAfkAsDead && pl.isAfk())) {
					deadCounter++;
				}
			}
			deadCounts.put(group, deadCounter);
		}
		int winDeadCount = Collections.min(deadCounts.values());
		int winnerCount = 0;
		for (Integer deadCount : deadCounts.values()) {
			if (deadCount == winDeadCount) {
				winnerCount++;
			}
		}
		if (winnerCount == 1) {
			for (Map.Entry<PlayerGroup, Integer> entry : deadCounts.entrySet()) {
				if (entry.getValue() == winDeadCount) {
					winner = entry.getKey();
				}
			}
		}
		return winner;
	}

	/**
	 * 获取整场胜方小队。
	 * Returns overall match winning group.
	 *
	 * @return 整场胜方小队 / winning group
	 */
	private PlayerGroup getWinner() {
		PlayerGroup winner = null;
		int drawPoints = 0;
		for (Map.Entry<Integer, Integer> entry : roundResults.entrySet()) {
			if (winner == null && entry.getValue() > drawPoints) {
				winner = super.getGroups().get(entry.getKey());
			} else if (winner != null && roundResults.get(winner.getBgIndex()) < entry.getValue()) {
				winner = super.getGroups().get(entry.getKey());
			} else if (winner != null && Objects.equals(roundResults.get(winner.getBgIndex()), entry.getValue())) {
				drawPoints = roundResults.get(winner.getBgIndex());
				winner = null;
			}
		}
		return winner;
	}

	/**
	 * 结束双队对局并结算奖励。
	 * Ends the two-team match and settles rewards.
	 *
	 * @param isDraw 是否平局 / whether draw
	 */
	protected void endTwoTeamMatch(boolean isDraw) {
		if (endCalled) {
			return;
		}
		endCalled = true;
		for (PlayerGroup group : super.getGroups()) {
			for (Player pl : group.getMembers()) {
				if (!pl.isAfk()) {
					super.freezeNoEnd(pl);
				}
			}
		}
		super.onEndFirstDefault();
		PlayerGroup roundWinner = getRoundWinner();
		if (roundWinner != null) {
			isDraw = false;
		}
		if (roundsDone < maxRounds) {
			roundsDone++;
			if (!isDraw) {
				addRoundWin(roundWinner);
			}
		}
		if (roundsDone < maxRounds) {
			if (!isDraw) {
				if (roundWinner != null) {
					for (PlayerGroup group : super.getGroups()) {
						for (Player pl : group.getMembers()) {
							super.scheduleAnnouncement(pl,
								LadderService.getInstance().getNameByIndex(roundWinner.getBgIndex())
									+ " Win's the round!",
								0);
						}
					}
					super.specAnnounce(
						LadderService.getInstance().getNameByIndex(roundWinner.getBgIndex()) + " Win's the round!");
					startNewRound();
					return;
				}
			} else {
				for (PlayerGroup group : super.getGroups()) {
					for (Player pl : group.getMembers()) {
						super.scheduleAnnouncement(pl, "Win's the round!", 0);
					}
				}
				super.specAnnounce("Draw!");
			}
		}
		PlayerGroup winner = getWinner();
		if (winner == null) {
			for (PlayerGroup group : super.getGroups()) {
				for (Player pl : group.getMembers()) {
					super.scheduleAnnouncement(pl, "Draw!", 0);
					// 若为“平局”则奖励玩家 / Reward Player if "DRAW"
					rewardPlayer(pl, MatchRank.DRAW);
				}
			}
			super.specAnnounce("Draw!");
		} else {
			for (PlayerGroup group : super.getGroups()) {
				if (Objects.equals(group.getTeamId(), winner.getTeamId())) {
					for (Player pl : group.getMembers()) {
						super.playerWinMatch(pl, super.K_VALUE / 2);
						// 若为“胜者”则奖励玩家 / Reward Player if "WINNER"
						rewardPlayer(pl, MatchRank.WINNER);
						super.scheduleAnnouncement(pl, "Your team wins the match!", 0);
						super.scheduleAnnouncement(pl, "You have received rewards!", 3000);
					}
				} else {
					for (Player pl : group.getMembers()) {
						super.playerLoseMatch(pl, -super.K_VALUE / 4);
						// 若为“败者”则奖励玩家 / Reward Player if "LOSER"
						rewardPlayer(pl, MatchRank.LOSER);
						super.scheduleAnnouncement(pl, "Your team lost the match!", 0);
						super.scheduleAnnouncement(pl, "You have received rewards!", 3000);
					}
				}
			}
			super.specAnnounce(LadderService.getInstance().getNameByIndex(winner.getBgIndex()) + " Win's the round!");
		}
		super.onEndDefault();
	}
}
