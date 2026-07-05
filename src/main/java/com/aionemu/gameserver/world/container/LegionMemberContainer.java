/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.world.container;

import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.model.team.legion.LegionMemberEx;
import com.aionemu.gameserver.world.exceptions.DuplicateAionObjectException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Container for storing Legion members by Id and name.
 *
 * @author Simple
 */
public class LegionMemberContainer {

	private final Map<Integer, LegionMember> legionMemberById = new LinkedHashMap<Integer, LegionMember>();

	private final Map<Integer, LegionMemberEx> legionMemberExById = new LinkedHashMap<Integer, LegionMemberEx>();
	private final Map<String, LegionMemberEx> legionMemberExByName = new LinkedHashMap<String, LegionMemberEx>();

	/**
	 * Add LegionMember to this Container.
	 *
	 * @param legionMember
	 */
	public synchronized void addMember(LegionMember legionMember) {
		if (!legionMemberById.containsKey(legionMember.getObjectId())) {
			legionMemberById.put(legionMember.getObjectId(), legionMember);
		}
	}

	/**
	 * This method will return a member from cache
	 *
	 * @param memberObjId
	 */
	public synchronized LegionMember getMember(int memberObjId) {
		return legionMemberById.get(memberObjId);
	}

	/**
	 * Add LegionMemberEx to this Container.
	 *
	 * @param legionMember
	 */
	public synchronized void addMemberEx(LegionMemberEx legionMember) {
		if (legionMemberExById.containsKey(legionMember.getObjectId())
				|| legionMemberExByName.containsKey(legionMember.getName()))
			throw new DuplicateAionObjectException();
		legionMemberExById.put(legionMember.getObjectId(), legionMember);
		legionMemberExByName.put(legionMember.getName(), legionMember);
	}

	/**
	 * This method will return a memberEx from cache
	 *
	 * @param memberObjId
	 */
	public synchronized LegionMemberEx getMemberEx(int memberObjId) {
		return legionMemberExById.get(memberObjId);
	}

	/**
	 * This method will return a memberEx from cache
	 *
	 * @param memberName
	 */
	public synchronized LegionMemberEx getMemberEx(String memberName) {
		return legionMemberExByName.get(memberName);
	}

	/**
	 * Remove LegionMember from this Container.
	 *
	 * @param legionMember
	 */
	public synchronized void remove(LegionMemberEx legionMember) {
		legionMemberById.remove(legionMember.getObjectId());
		legionMemberExById.remove(legionMember.getObjectId());
		legionMemberExByName.remove(legionMember.getName());
	}

	/**
	 * Returns true if legion is in cached by id
	 *
	 * @param memberObjId
	 * @return true or false
	 */
	public synchronized boolean contains(int memberObjId) {
		return legionMemberById.containsKey(memberObjId);
	}

	/**
	 * Returns true if legion is in cached by id
	 *
	 * @param memberObjId
	 * @return true or false
	 */
	public synchronized boolean containsEx(int memberObjId) {
		return legionMemberExById.containsKey(memberObjId);
	}

	/**
	 * Returns true if legion is in cached by id
	 *
	 * @param memberName
	 * @return true or false
	 */
	public synchronized boolean containsEx(String memberName) {
		return legionMemberExByName.containsKey(memberName);
	}

	public synchronized void clear() {
		legionMemberById.clear();
		legionMemberExById.clear();
		legionMemberExByName.clear();
	}
}
