package com.aionemu.gameserver.model.team2;

import java.util.Collection;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.google.common.base.Predicate;

/**
 * 团队接口。
 * Team interface.
 * @author ATracer
 */
public interface Team<M, TM extends TeamMember<M>> {

	Integer getTeamId();

	TM getMember(Integer objectId);

	boolean hasMember(Integer objectId);

	void addMember(TM member);

	void removeMember(TM member);

	void removeMember(Integer objectId);

	Collection<M> getMembers();

	Collection<M> getOnlineMembers();

	void onEvent(TeamEvent event);

	Collection<TM> filter(Predicate<TM> predicate);

	Collection<M> filterMembers(Predicate<M> predicate);

	void sendPacket(AionServerPacket packet);

	void sendPacket(AionServerPacket packet, Predicate<M> predicate);

	int onlineMembers();

	Race getRace();

	int size();

	/**
	 * 返回队伍是否没有任何成员。
	 * Returns whether the team has no members at all.
	 * @return 无成员时为 true / true if empty
	 */
	default boolean isEmpty() {
		return size() == 0;
	}

	boolean isFull();
}
