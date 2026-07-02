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
package com.aionemu.gameserver.dataholders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.challenge.ChallengeQuestTemplate;
import com.aionemu.gameserver.model.templates.challenge.ChallengeTaskTemplate;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "task" })
@XmlRootElement(name = "challenge_tasks")
public class ChallengeData {
	protected List<ChallengeTaskTemplate> task;

	@XmlTransient
	protected Map<Integer, ChallengeTaskTemplate> tasksById = new HashMap<Integer, ChallengeTaskTemplate>();
	@XmlTransient
	private Map<Integer, ChallengeTaskTemplate> tasksByQuestId = new HashMap<Integer, ChallengeTaskTemplate>();
	@XmlTransient
	private Map<Integer, ChallengeQuestTemplate> questsById = new HashMap<Integer, ChallengeQuestTemplate>();

	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (ChallengeTaskTemplate t : task) {
			tasksById.put(t.getId(), t);
			for (ChallengeQuestTemplate q : t.getQuests()) {
				tasksByQuestId.put(q.getId(), t);
				questsById.put(q.getId(), q);
			}
		}
		task.clear();
		task = null;
	}

	public Map<Integer, ChallengeTaskTemplate> getTasks() {
		return this.tasksById;
	}

	public ChallengeTaskTemplate getTaskByTaskId(int taskId) {
		return tasksById.get(taskId);
	}

	public ChallengeTaskTemplate getTaskByQuestId(int questId) {
		return tasksByQuestId.get(questId);
	}

	public ChallengeQuestTemplate getQuestByQuestId(int questId) {
		return questsById.get(questId);
	}

	public int size() {
		return this.tasksById.size();
	}
}
