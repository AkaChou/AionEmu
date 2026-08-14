package com.aionemu.gameserver.model.team2;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

/**
 * 通用团队，用于团队2相关逻辑。
 * General Team for team 2 logic.
 *
 * @author ATracer
 */
@Slf4j
public abstract class GeneralTeam<M extends AionObject, TM extends TeamMember<M>> extends AionObject
		implements Team<M, TM> {
	protected final Map<Integer, TM> members = new ConcurrentHashMap<Integer, TM>();
	protected final Lock teamLock = new ReentrantLock();
	private TM leader;
	private final MemberTransformFunction<TM, M> TRANSFORM_FUNCTION = new MemberTransformFunction<TM, M>();

	public GeneralTeam(Integer objId) {
		super(objId);
	}

	/** 处理团队事件。 / Handles team events. */
	@Override
	public void onEvent(TeamEvent event) {
		lock();
		try {
			if (event.checkCondition()) {
				event.handleEvent();
			} else {
				log.warn(I18n.get("log.f1fc52eddeee", event, this));
			}
		} finally {
			unlock();
		}
	}

	/** 返回成员 / Returns the member */
	@Override
	public TM getMember(Integer objectId) {
		return members.get(objectId);
	}

	/** 是否包含成员 / Whether member */
	@Override
	public boolean hasMember(Integer objectId) {
		return members.get(objectId) != null;
	}

	/** 添加成员 / Adds member */
	@Override
	public void addMember(TM member) {
		Preconditions.checkNotNull(member, "Team member should be not null");
		Preconditions.checkState(members.get(member.getObjectId()) == null, "Team member is already added");
		members.put(member.getObjectId(), member);
	}

	/** 移除成员 / Removes member */
	@Override
	public void removeMember(TM member) {
		Preconditions.checkNotNull(member, "Team member should be not null");
		Preconditions.checkState(members.get(member.getObjectId()) != null, "Team member is already removed");
		members.remove(member.getObjectId());
	}

	/** 移除成员 / Removes member */
	@Override
	public final void removeMember(Integer objectId) {
		removeMember(members.get(objectId));
	}

	/**
	 * 对所有队员应用谓词（仅用于改变队伍或成员状态）。 / Apply some predicate on all group members<br> Should be used only to change state of the group or its members
	 */
	public void apply(Predicate<TM> predicate) {
		lock();
		try {
			for (TM member : members.values()) {
				if (!predicate.apply(member)) {
					return;
				}
			}
		} finally {
			unlock();
		}
	}

	/**
	 * 对所有队员对象应用谓词（仅用于改变队伍或成员状态）。 / Apply some predicate on all group member's objects<br> Should be used only to change state of the group or its members
	 */
	public void applyOnMembers(Predicate<M> predicate) {
		lock();
		try {
			for (TM member : members.values()) {
				if (!predicate.apply(member.getObject())) {
					return;
				}
			}
		} finally {
			unlock();
		}
	}

	/** 过滤。 / Filter. */
	@Override
	public Collection<TM> filter(Predicate<TM> predicate) {
		return Collections2.filter(members.values(), predicate);
	}

	/** 过滤成员对象。 / Filters member objects. */
	@Override
	public Collection<M> filterMembers(Predicate<M> predicate) {
		return Collections2.filter(Collections2.transform(members.values(), TRANSFORM_FUNCTION), predicate);
	}

	/** 返回成员对象集合 / Returns the members */
	@Override
	public Collection<M> getMembers() {
		return filterMembers(Predicates.<M>alwaysTrue());
	}

	/** 返回队伍人数 / size. */
	@Override
	public int size() {
		return members.size();
	}

	/** 返回团队 ID / Returns the team id */
	@Override
	public final Integer getTeamId() {
		return getObjectId();
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return GeneralTeam.class.getName();
	}

	/** 返回队长 / Returns the leader*/
	public final TM getLeader() {
		return leader;
	}

	/** 返回队长对象 / Returns the leader object */
	public final M getLeaderObject() {
		return leader.getObject();
	}

	/** 判断是否队长。 / Whether Leader. */
	public final boolean isLeader(M member) {
		return leader.getObject().getObjectId().equals(member.getObjectId());
	}

	/** 更换队长。 / Changes the leader. */
	public final void changeLeader(TM member) {
		Preconditions.checkNotNull(leader, "Leader should already be set");
		Preconditions.checkNotNull(member, "New leader should not be null");
		this.leader = member;
	}

	protected final void setLeader(TM member) {
		Preconditions.checkState(leader == null, "Leader should be not initialized");
		Preconditions.checkNotNull(member, "Leader should not be null");
		this.leader = member;
	}

	protected final void lock() {
		teamLock.lock();
	}

	protected final void unlock() {
		teamLock.unlock();
	}

	private static final class MemberTransformFunction<TM extends TeamMember<M>, M> implements Function<TM, M> {

		/** 应用。 / Apply. */
		@Override
		public M apply(TM member) {
			return member.getObject();
		}
	}
}
