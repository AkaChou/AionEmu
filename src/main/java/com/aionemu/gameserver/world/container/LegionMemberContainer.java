package com.aionemu.gameserver.world.container;

import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.model.team.legion.LegionMemberEx;
import com.aionemu.gameserver.world.exceptions.DuplicateAionObjectException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 军团成员容器：按 ID / 名称缓存 {@link LegionMember} 与 {@link LegionMemberEx}。
 * {@link LegionMemberEx}).
 *
 * @author Simple
 */
public class LegionMemberContainer {

	/**
	 * 按 objectId 索引的基础成员 / Basic members indexed by objectId
	 */
	private final Map<Integer, LegionMember> legionMemberById = new LinkedHashMap<Integer, LegionMember>();

	/**
	 * 按 objectId 索引的扩展成员 / Extended members indexed by objectId
	 */
	private final Map<Integer, LegionMemberEx> legionMemberExById = new LinkedHashMap<Integer, LegionMemberEx>();

	/**
	 * 按名称索引的扩展成员 / Extended members indexed by name
	 */
	private final Map<String, LegionMemberEx> legionMemberExByName = new LinkedHashMap<String, LegionMemberEx>();

	/**
	 * 添加基础军团成员（已存在则忽略）。
	 * Adds a basic legion member (ignored if already present).
	 *
	 * @param legionMember 待添加成员 / member to add
	 */
	public synchronized void addMember(LegionMember legionMember) {
		if (!legionMemberById.containsKey(legionMember.getObjectId())) {
			legionMemberById.put(legionMember.getObjectId(), legionMember);
		}
	}

	/**
	 * 按 objectId 获取基础成员。
	 * Returns a basic member from the cache by objectId.
	 *
	 * @param memberObjId 成员 objectId / member objectId
	 * @return 成员实例，不存在则返回 null / member instance, or null if absent
	 */
	public synchronized LegionMember getMember(int memberObjId) {
		return legionMemberById.get(memberObjId);
	}

	/**
	 * 添加扩展军团成员；ID 或名称冲突时抛出 {@link DuplicateAionObjectException}。
	 * Adds an extended legion member; throws {@link DuplicateAionObjectException} on ID or name conflict.
	 *
	 * @param legionMember 待添加扩展成员 / extended member to add
	 */
	public synchronized void addMemberEx(LegionMemberEx legionMember) {
		if (legionMemberExById.containsKey(legionMember.getObjectId())
				|| legionMemberExByName.containsKey(legionMember.getName()))
			throw new DuplicateAionObjectException();
		legionMemberExById.put(legionMember.getObjectId(), legionMember);
		legionMemberExByName.put(legionMember.getName(), legionMember);
	}

	/**
	 * 按 objectId 获取扩展成员。
	 * Returns an extended member from the cache by objectId.
	 *
	 * @param memberObjId 成员 objectId / member objectId
	 * @return 扩展成员实例，不存在则返回 null / extended member, or null if absent
	 */
	public synchronized LegionMemberEx getMemberEx(int memberObjId) {
		return legionMemberExById.get(memberObjId);
	}

	/**
	 * 按名称获取扩展成员。
	 * Returns an extended member from the cache by name.
	 *
	 * @param memberName 成员名称 / member name
	 * @return 扩展成员实例，不存在则返回 null / extended member, or null if absent
	 */
	public synchronized LegionMemberEx getMemberEx(String memberName) {
		return legionMemberExByName.get(memberName);
	}

	/**
	 * 从容器中移除成员（同时清理基础与扩展索引）。
	 * Removes the member from this container (clears both basic and extended indexes).
	 *
	 * @param legionMember 待移除扩展成员 / extended member to remove
	 */
	public synchronized void remove(LegionMemberEx legionMember) {
		legionMemberById.remove(legionMember.getObjectId());
		legionMemberExById.remove(legionMember.getObjectId());
		legionMemberExByName.remove(legionMember.getName());
	}

	/**
	 * 是否缓存了指定 objectId 的基础成员。
	 * Whether a basic member with the given objectId is cached.
	 *
	 * @param memberObjId 成员 objectId / member objectId
	 * @return 存在则为 true / true if present
	 */
	public synchronized boolean contains(int memberObjId) {
		return legionMemberById.containsKey(memberObjId);
	}

	/**
	 * 是否缓存了指定 objectId 的扩展成员。
	 * Whether an extended member with the given objectId is cached.
	 *
	 * @param memberObjId 成员 objectId / member objectId
	 * @return 存在则为 true / true if present
	 */
	public synchronized boolean containsEx(int memberObjId) {
		return legionMemberExById.containsKey(memberObjId);
	}

	/**
	 * 是否缓存了指定名称的扩展成员。
	 * Whether an extended member with the given name is cached.
	 *
	 * @param memberName 成员名称 / member name
	 * @return 存在则为 true / true if present
	 */
	public synchronized boolean containsEx(String memberName) {
		return legionMemberExByName.containsKey(memberName);
	}

	/**
	 * 清空全部成员缓存。
	 * Clears all member caches.
	 */
	public synchronized void clear() {
		legionMemberById.clear();
		legionMemberExById.clear();
		legionMemberExByName.clear();
	}
}
