package com.aionemu.gameserver.model.team2.league;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.team2.GeneralTeam;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.common.legacy.LootGroupRules;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import com.aionemu.gameserver.utils.idfactory.IDFactory;

/**
 * 战团，用于团队2相关逻辑。
 * League for team 2 logic.
 *
 * @author ATracer
 */
@Setter
@Getter
public class League extends GeneralTeam<PlayerAlliance, LeagueMember> {
	/**
	 * -- GETTER --
	 * 返回 loot group rules / Returns the loot group rules
	 * -- SETTER --
	 * 设置 loot group rules / Sets the loot group rules
	 */
	private LootGroupRules lootGroupRules = new LootGroupRules();
	private static final LeagueMemberComparator MEMBER_COMPARATOR = new LeagueMemberComparator();

	public League(LeagueMember leader) {
		super(IDFactory.getInstance().nextId());
		initializeTeam(leader);
	}

	protected final void initializeTeam(LeagueMember leader) {
		setLeader(leader);
	}

	/**
	 * 返回 online members / Returns the online members
	 */
	@Override
	public Collection<PlayerAlliance> getOnlineMembers() {
		return getMembers();
	}

	/**
	 * 添加 member / Adds member
	 */
	@Override
	public void addMember(LeagueMember member) {
		super.addMember(member);
		member.getObject().setLeague(this);
	}

	/**
	 * 移除 member / Removes member
	 */
	@Override
	public void removeMember(LeagueMember member) {
		super.removeMember(member);
		member.getObject().setLeague(null);
	}

	/**
	 * 发送数据包。 / Send packet.
	 */
	@Override
	public void sendPacket(AionServerPacket packet) {
		for (PlayerAlliance alliance : getMembers()) {
			alliance.sendPacket(packet);
		}
	}

	/**
	 * 发送数据包。 / Send packet.
	 */
	@Override
	public void sendPacket(AionServerPacket packet, Predicate<PlayerAlliance> predicate) {
		for (PlayerAlliance alliance : getMembers()) {
			if (predicate.apply(alliance)) {
				alliance.sendPacket(packet, Predicates.alwaysTrue());
			}
		}
	}

	/**
	 * 在线成员 / online Members.
	 */
	@Override
	public int onlineMembers() {
		return getMembers().size();
	}

	/**
	 * 获取种族。 / Returns the race.
	 */
	@Override
	public Race getRace() {
		return getLeaderObject().getRace();
	}

	/**
	 * 是否已满。 / Whether Full.
	 */
	@Override
	public boolean isFull() {
		return size() == 8;
	}

	/**
	 * 返回 sorted members / Returns the sorted members
	 */
	public Collection<LeagueMember> getSortedMembers() {
		ArrayList<LeagueMember> newArrayList = Lists.newArrayList(members.values());
		newArrayList.sort(MEMBER_COMPARATOR);
		return newArrayList;
	}

	static class LeagueMemberComparator implements Comparator<LeagueMember> {
		/**
		 * 比较 / compare.
		 */
		@Override
		public int compare(LeagueMember o1, LeagueMember o2) {
			return Integer.compare(o1.getLeaguePosition(), o2.getLeaguePosition());
		}
	}
}
