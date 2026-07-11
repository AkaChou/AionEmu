package com.aionemu.gameserver.model.team2.league;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.GeneralTeam;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceMember;
import com.aionemu.gameserver.model.team2.common.legacy.LootGroupRules;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

/**
 * 战团，用于团队2相关逻辑。
 * League for team 2 logic.
 *
 * @author ATracer
 */
public class League extends GeneralTeam<PlayerAlliance, LeagueMember> {
	private LootGroupRules lootGroupRules = new LootGroupRules();
	private static final LeagueMemberComparator MEMBER_COMPARATOR = new LeagueMemberComparator();

	public League(LeagueMember leader) {
		super(GameWorldBootstrapServices.idFactory().nextId());
		initializeTeam(leader);
	}

	protected final void initializeTeam(LeagueMember leader) {
		setLeader(leader);
	}

	/** 返回 online members / Returns the online members */
	@Override
	public Collection<PlayerAlliance> getOnlineMembers() {
		return getMembers();
	}

	/** 添加 member / Adds member */
	@Override
	public void addMember(LeagueMember member) {
		super.addMember(member);
		member.getObject().setLeague(this);
	}

	/** 移除 member / Removes member */
	@Override
	public void removeMember(LeagueMember member) {
		super.removeMember(member);
		member.getObject().setLeague(null);
	}

	/** 发送数据包。 / Send packet. */
	@Override
	public void sendPacket(AionServerPacket packet) {
		for (PlayerAlliance alliance : getMembers()) {
			alliance.sendPacket(packet);
		}
	}

	/** 发送数据包。 / Send packet. */
	@Override
	public void sendPacket(AionServerPacket packet, Predicate<PlayerAlliance> predicate) {
		for (PlayerAlliance alliance : getMembers()) {
			if (predicate.apply(alliance)) {
				alliance.sendPacket(packet, Predicates.<Player>alwaysTrue());
			}
		}
	}

	/** 在线成员 / online Members. */
	@Override
	public int onlineMembers() {
		return getMembers().size();
	}

	/** 获取种族。 / Returns the race. */
	@Override
	public Race getRace() {
		return getLeaderObject().getRace();
	}

	/** 是否已满。 / Whether Full. */
	@Override
	public boolean isFull() {
		return size() == 8;
	}

	/** 返回 loot group rules / Returns the loot group rules */
	public LootGroupRules getLootGroupRules() {
		return lootGroupRules;
	}

	/** 设置 loot group rules / Sets the loot group rules */
	public void setLootGroupRules(LootGroupRules lootGroupRules) {
		this.lootGroupRules = lootGroupRules;
	}

	/** 返回 sorted members / Returns the sorted members */
	public Collection<LeagueMember> getSortedMembers() {
		ArrayList<LeagueMember> newArrayList = Lists.newArrayList(members.values());
		Collections.sort(newArrayList, MEMBER_COMPARATOR);
		return newArrayList;
	}

	/** 返回 player member / Returns the player member */
	public Player getPlayerMember(Integer playerObjId) {
		for (PlayerAlliance member : getMembers()) {
			PlayerAllianceMember playerMember = member.getMember(playerObjId);
			if (playerMember != null) {
				return playerMember.getObject();
			}
		}
		return null;
	}

	static class LeagueMemberComparator implements Comparator<LeagueMember> {
		/** 比较 / compare. */
		@Override
		public int compare(LeagueMember o1, LeagueMember o2) {
			return o1.getLeaguePosition() > o2.getLeaguePosition() ? 1 : -1;
		}
	}
}
