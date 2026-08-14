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

/**
 * 挑战任务数据容器，按任务 ID 与任务链任务 ID 多路索引。
 * Challenge task data holder, multi-indexed by task id and quest id.
 */
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

	/**
	 * JAXB 反序列化完成后，建立任务/任务链多路索引并释放列表。
	 * After JAXB unmarshalling, builds multi-indexes for tasks and quests, then clears the list.
	 */
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

	/**
	 * 返回按任务 ID 索引的全部挑战任务。
	 * Returns all challenge tasks indexed by task id.
	 *
	 * @return 任务 ID 到模板的映射 / map of task id to template
	 */
	public Map<Integer, ChallengeTaskTemplate> getTasks() {
		return this.tasksById;
	}

	/**
	 * 按任务 ID 获取挑战任务模板。
	 * Returns the challenge task template for the given task id.
	 *
	 * @param taskId 任务 ID / task id
	 * @return 模板，不存在则为 null / template or null
	 */
	public ChallengeTaskTemplate getTaskByTaskId(int taskId) {
		return tasksById.get(taskId);
	}

	/**
	 * 按任务链任务 ID 获取所属挑战任务模板。
	 * Returns the challenge task that owns the given quest id.
	 *
	 * @param questId 任务链任务 ID / quest id
	 * @return 任务模板，不存在则为 null / task template or null
	 */
	public ChallengeTaskTemplate getTaskByQuestId(int questId) {
		return tasksByQuestId.get(questId);
	}

	/**
	 * 按任务链任务 ID 获取挑战任务条目。
	 * Returns the challenge quest template for the given quest id.
	 *
	 * @param questId 任务链任务 ID / quest id
	 * @return 任务条目，不存在则为 null / quest template or null
	 */
	public ChallengeQuestTemplate getQuestByQuestId(int questId) {
		return questsById.get(questId);
	}

	/**
	 * 返回已加载的挑战任务数量。
	 * Returns the number of loaded challenge tasks.
	 *
	 * @return 已加载的挑战任务数量 / Returns the number of loaded challenge tasks.
	 */
	public int size() {
		return this.tasksById.size();
	}
}
