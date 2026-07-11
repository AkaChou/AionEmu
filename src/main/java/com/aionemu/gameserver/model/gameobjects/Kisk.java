package com.aionemu.gameserver.model.gameobjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.stats.KiskStatsTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_KISK_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.HashSet;

/**
 * 归还之石游戏对象。
 * Kisk game object.
 */

public class Kisk extends SummonedObject<Player> {
	private final Legion ownerLegion;
	private final Race ownerRace;
	private KiskStatsTemplate kiskStatsTemplate;
	private int remainingResurrections;
	private long kiskSpawnTime;
	public final int KISK_LIFETIME_IN_SEC = 2 * 60 * 60;
	private final Set<Integer> kiskMemberIds;

	public Kisk(int objId, NpcController controller, SpawnTemplate spawnTemplate, NpcTemplate npcTemplate,
			Player owner) {
		super(objId, controller, spawnTemplate, npcTemplate, npcTemplate.getLevel());
		this.kiskStatsTemplate = npcTemplate.getKiskStatsTemplate();
		if (this.kiskStatsTemplate == null) {
			this.kiskStatsTemplate = new KiskStatsTemplate();
		}
		this.kiskMemberIds = new HashSet<Integer>(kiskStatsTemplate.getMaxMembers());
		this.remainingResurrections = this.kiskStatsTemplate.getMaxResurrects();
		this.kiskSpawnTime = System.currentTimeMillis() / 1000;
		this.ownerLegion = owner.getLegion();
		this.ownerRace = owner.getRace();
	}

	/** 是否敌对。 / Whether Enemy. */
	@Override
	public boolean isEnemy(Creature creature) {
		return creature.isEnemyFrom(this);
	}

	/**
	 * @param npc 是否 enemy 从 / 是否 enemy 从。 / Whether enemy from / Whether enemy from
	 */
	@Override
	public boolean isEnemyFrom(Npc npc) {
		return npc.isAttackableNpc() || npc.isAggressiveTo(this);
	}

	/**
	 * @param player 是否 enemy 从 / 是否 enemy 从。 / Whether enemy from / Whether enemy from
	 */
	@Override
	public boolean isEnemyFrom(Player player) {
		return player.getRace() != this.ownerRace;
	}

	/** 返回 npc object type / Returns the npc object type */
	@Override
	public NpcObjectType getNpcObjectType() {
		return NpcObjectType.NORMAL;
	}

	/** 返回 use mask / Returns the use mask */
	public int getUseMask() {
		return this.kiskStatsTemplate.getUseMask();
	}

	/** 返回 current member list / Returns the current member list */
	public List<Player> getCurrentMemberList() {
		List<Player> currentMemberList = new ArrayList<Player>();
		for (int memberId : this.kiskMemberIds) {
			Player member = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(memberId);
			if (member != null) {
				currentMemberList.add(member);
			}
		}
		return currentMemberList;
	}

	/** 返回 current member count / Returns the current member count */
	public int getCurrentMemberCount() {
		return this.kiskMemberIds.size();
	}

	/** 返回 current member ids / Returns the current member ids */
	public Set<Integer> getCurrentMemberIds() {
		return this.kiskMemberIds;
	}

	/** 返回 max members / Returns the max members */
	public int getMaxMembers() {
		return this.kiskStatsTemplate.getMaxMembers();
	}

	/** 返回 remaining resurrects / Returns the remaining resurrects */
	public int getRemainingResurrects() {
		return this.remainingResurrections;
	}

	/** 返回 max ressurects / Returns the max ressurects */
	public int getMaxRessurects() {
		return this.kiskStatsTemplate.getMaxResurrects();
	}

	/** 返回 remaining lifetime / Returns the remaining lifetime */
	public int getRemainingLifetime() {
		long timeElapsed = (System.currentTimeMillis() / 1000) - kiskSpawnTime;
		int timeRemaining = (int) (KISK_LIFETIME_IN_SEC - timeElapsed);
		return (timeRemaining > 0 ? timeRemaining : 0);
	}

	/** 是否可以绑定。 / Whether bind. */
	public boolean canBind(Player player) {
		if (!player.getName().equals(getMasterName())) {
			switch (this.getUseMask()) {
			case 0:
			case 1:
				if (this.ownerRace != player.getRace())
					return false;
				break;
			case 2:
				if (ownerLegion == null || !ownerLegion.isMember(player.getObjectId()))
					return false;
				break;
			case 3:
				return false;
			case 4:
				if (!player.isInTeam() || !player.getCurrentGroup().hasMember(getCreatorId()))
					return false;
				break;
			case 5:
			case 6:
				if (!player.isInTeam()
						|| (player.isInAlliance2() && !player.getPlayerAlliance2().hasMember(getCreatorId()))
						|| (player.isInGroup2() && !player.getPlayerGroup2().hasMember(getCreatorId())))
					return false;
				break;
			default:
				return false;
			}
		}
		if (this.getCurrentMemberCount() >= getMaxMembers()) {
			return false;
		}
		return true;
	}

	/** 添加玩家。 / Adds player. */
	public synchronized void addPlayer(Player player) {
		if (kiskMemberIds.add(player.getObjectId())) {
			this.broadcastKiskUpdate();
		} else {
			PacketSendUtility.sendPacket(player, new SM_KISK_UPDATE(this));
		}
		player.setKisk(this);
	}

	/** 移除玩家。 / Removes player. */
	public synchronized void removePlayer(Player player) {
		player.setKisk(null);
		if (kiskMemberIds.remove(player.getObjectId())) {
			this.broadcastKiskUpdate();
		}
	}

	private void broadcastKiskUpdate() {
		for (Player member : this.getCurrentMemberList()) {
			if (!this.getKnownList().knowns(member)) {
				PacketSendUtility.sendPacket(member, new SM_KISK_UPDATE(this));
			}
		}
		final Kisk kisk = this;
		getKnownList().doOnAllPlayers(new Visitor<Player>() {
			/** 访问 / visit. */
			@Override
			public void visit(Player object) {
				if (object.getRace() == ownerRace) {
					PacketSendUtility.sendPacket(object, new SM_KISK_UPDATE(kisk));
				}
			}
		});
	}

	/** Broadcast Packet / Broadcast Packet */
	public void broadcastPacket(SM_SYSTEM_MESSAGE message) {
		for (Player member : this.getCurrentMemberList()) {
			if (member != null) {
				PacketSendUtility.sendPacket(member, message);
			}
		}
	}

	/** 已使用复活 / resurrection Used. */
	public void resurrectionUsed() {
		remainingResurrections -= 1;
		broadcastKiskUpdate();
		if (remainingResurrections <= 0) {
			this.getController().onDelete();
		}
	}

	/** 返回所有者种族 / Returns the owner race*/
	public Race getOwnerRace() {
		return this.ownerRace;
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return !this.getLifeStats().isAlreadyDead() && this.getRemainingResurrects() > 0;
	}
}
