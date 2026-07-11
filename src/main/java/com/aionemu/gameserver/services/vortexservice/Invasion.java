package com.aionemu.gameserver.services.vortexservice;

import java.util.LinkedHashMap;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Kisk;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.team2.TeamType;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.model.vortex.VortexLocation;
import com.aionemu.gameserver.model.vortex.VortexStateType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 次元漩涡入侵默认实现：攻防同盟、Kisk 清理与入队/踢出。
 * Default dimensional-vortex invasion: offence/defence alliances, kisk cleanup, join/kick.
 *
 * @author Rinzler (Encom)
 */
public class Invasion extends DimensionalVortex<VortexLocation> {

	PlayerAlliance invAlliance, defAlliance;
	protected Map<Integer, Player> invaders = new LinkedHashMap<Integer, Player>();
	protected Map<Integer, Player> defenders = new LinkedHashMap<Integer, Player>();

	/**
	 * 绑定漩涡地点。
	 * Binds the vortex location.
	 *
	 * vortex location
	 */
	public Invasion(VortexLocation vortex) {
		super(vortex);
	}

	/**
	 * 激活入侵、刷 INVASION 怪、初始化生成器并同步守方同盟。
	 * Activates invasion, spawns INVASION entities, inits generator, syncs defender alliance.
	 */
	@Override
	public void startInvasion() {
		getVortexLocation().setActiveVortex(this);
		despawn();
		spawn(VortexStateType.INVASION);
		initRiftGenerator();
		updateAlliance();
	}

	/**
	 * 结束入侵：注销监听、摧毁攻方 Kisk、踢出攻方并恢复 PEACE。
	 * Ends invasion: unregisters listeners, kills invader kisks, kicks invaders, restores PEACE.
	 */
	@Override
	public void stopInvasion() {
		getVortexLocation().setActiveVortex(null);
		unregisterSiegeBossListeners();
		for (Kisk kisk : getVortexLocation().getInvadersKisks().values()) {
			kisk.getController().die();
		}
		for (Player invader : invaders.values()) {
			if (invader.isOnline()) {
				kickPlayer(invader, true);
			}
		}
		despawn();
		spawn(VortexStateType.PEACE);
	}

	/**
	 * 将玩家加入攻/守方并维护对应同盟。
	 * Adds a player to invader/defender side and maintains the matching alliance.
	 *
	 * 玩家 / player
	 * whether invader
	 */
	@Override
	public void addPlayer(Player player, boolean isInvader) {
		Map<Integer, Player> list = isInvader ? invaders : defenders;
		PlayerAlliance alliance = isInvader ? invAlliance : defAlliance;
		if (alliance != null && alliance.size() > 0) {
			PlayerAllianceService.addPlayer(alliance, player);
		} else if (!list.isEmpty()) {
			Player first = null;
			for (Player firstOne : list.values()) {
				if (firstOne.isInGroup2()) {
					PlayerGroupService.removePlayer(firstOne);
				} else if (firstOne.isInAlliance2()) {
					PlayerAllianceService.removePlayer(firstOne);
				}
				first = firstOne;
			}
			if (first.getObjectId() != player.getObjectId()) {
				if (isInvader) {
					invAlliance = PlayerAllianceService.createAlliance(first, player, TeamType.ALLIANCE_OFFENCE);
				} else {
					defAlliance = PlayerAllianceService.createAlliance(first, player, TeamType.ALLIANCE_DEFENCE);
				}
			} else {
				kickPlayer(player, isInvader);
			}
		}
		list.put(player.getObjectId(), player);
	}

	/**
	 * 将玩家踢出并在攻方场景内时传送回家点。
	 * Kicks a player and teleports home when still inside the invasion world as invader.
	 *
	 * 玩家 / player
	 * whether invader
	 */
	@Override
	public void kickPlayer(Player player, boolean isInvader) {
		Map<Integer, Player> list = isInvader ? invaders : defenders;
		PlayerAlliance alliance = isInvader ? invAlliance : defAlliance;
		list.remove(player.getObjectId());
		if (alliance != null && alliance.hasMember(player.getObjectId())) {
			if (player.isOnline()) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(isInvader ? 1401452 : 1401476));
			}
			PlayerAllianceService.removePlayer(player);
			if (alliance.size() == 0) {
				if (isInvader) {
					invAlliance = null;
				} else {
					defAlliance = null;
				}
			}
		}
		if (isInvader && player.isOnline() && player.getWorldId() == getVortexLocation().getInvasionWorldId()) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401474));
			TeleportService2.teleportTo(player, getVortexLocation().getHomePoint());
		}
		getVortexLocation().getVortexController().getPassedPlayers().remove(player.getObjectId());
		getVortexLocation().getVortexController().syncPassed(true);
	}

	/**
	 * 向守方玩家弹出加入同盟确认；同意后入队。
	 * Prompts a defender to join the defence alliance; accepts join on confirm.
	 *
	 * defender
	 */
	@Override
	public void updateDefenders(Player defender) {
		if (defenders.containsKey(defender.getObjectId())) {
			return;
		}
		if (defAlliance == null || !defAlliance.isFull()) {
			RequestResponseHandler responseHandler = new RequestResponseHandler(defender) {
				@Override
				public void acceptRequest(Creature requester, Player responder) {
					if (responder.isInGroup2()) {
						PlayerGroupService.removePlayer(responder);
					} else if (responder.isInAlliance2()) {
						PlayerAllianceService.removePlayer(responder);
					}
					if (defAlliance == null || !defAlliance.isFull()) {
						addPlayer(responder, false);
					}
				}

				@Override
				public void denyRequest(Creature requester, Player responder) {
					onDeny(responder);
				}
			};
			boolean requested = defender.getResponseRequester().putRequest(904306, responseHandler);
			if (requested) {
				PacketSendUtility.sendPacket(defender, new SM_QUESTION_WINDOW(904306, 0, 0));
			}
		}
	}

	/**
	 * 拒绝对话处理（恒为 true）。
	 * Deny-dialog handler (always true).
	 *
	 * 玩家 / player
	 * always true
	 */
	private boolean onDeny(Player player) {
		return true;
	}

	/**
	 * 将攻方玩家直接加入攻方列表。
	 * Directly registers an invader player.
	 *
	 * invader
	 */
	@Override
	public void updateInvaders(Player invader) {
		if (invaders.containsKey(invader.getObjectId())) {
			return;
		}
		addPlayer(invader, true);
	}

	/**
	 * 对地点内守方种族玩家同步守方登记。
	 * Syncs defender registration for players of the defender race on the location.
	 */
	private void updateAlliance() {
		for (Player player : getVortexLocation().getPlayers().values()) {
			if (player.getRace().equals(getVortexLocation().getDefendersRace())) {
				updateDefenders(player);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<Integer, Player> getInvaders() {
		return invaders;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<Integer, Player> getDefenders() {
		return defenders;
	}
}
