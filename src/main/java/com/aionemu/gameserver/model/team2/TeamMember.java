package com.aionemu.gameserver.model.team2;

/**
 * 团队 Member 接口。
 * Team Member interface.
 *
 * @author ATracer
 */
public interface TeamMember<M> {

	Integer getObjectId();

	String getName();

	M getObject();
}
