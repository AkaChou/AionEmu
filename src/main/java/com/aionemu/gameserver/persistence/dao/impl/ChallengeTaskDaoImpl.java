package com.aionemu.gameserver.persistence.dao.impl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.challenge.ChallengeQuest;
import com.aionemu.gameserver.model.challenge.ChallengeTask;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.templates.challenge.ChallengeQuestTemplate;
import com.aionemu.gameserver.model.templates.challenge.ChallengeType;
import com.aionemu.gameserver.persistence.dao.ChallengeTaskDao;
import com.aionemu.gameserver.persistence.entity.ChallengeTaskPo;
import com.aionemu.gameserver.persistence.mapper.ChallengeTaskMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 挑战任务数据访问实现。
 * Data access implementation for challenge tasks.
 */
@Slf4j
@Service
@Lazy
public class ChallengeTaskDaoImpl extends ServiceImpl<ChallengeTaskMapper, ChallengeTaskPo>
	implements ChallengeTaskDao {

	@Override
	public Map<Integer, ChallengeTask> load(int ownerId, ChallengeType type) {
		Map<Integer, ChallengeTask> tasks = new LinkedHashMap<>();
		try {
			mapper.selectListByQuery(QueryWrapper.create()
				.where(ChallengeTaskPo::getOwnerId).eq(ownerId)
				.and(ChallengeTaskPo::getOwnerType).eq(type.toString()))
				.forEach(po -> {
					int taskId = po.getTaskId();
					int questId = po.getQuestId();
					ChallengeQuestTemplate template = DataManager.CHALLENGE_DATA.getQuestByQuestId(questId);
					ChallengeQuest quest = new ChallengeQuest(template, po.getCompleteCount());
					quest.setPersistentState(PersistentState.UPDATED);

					if (!tasks.containsKey(taskId)) {
						Map<Integer, ChallengeQuest> quests = new HashMap<>(2);
						quests.put(quest.getQuestId(), quest);
						tasks.put(taskId, new ChallengeTask(taskId, ownerId, quests, po.getCompleteTime()));
					} else {
						tasks.get(taskId).getQuests().put(questId, quest);
					}
				});
		} catch (DataAccessException e) {
			log.error(I18n.get("log.617c98736ef3"), e);
		}
		return tasks;
	}

	@Override
	public void storeTask(ChallengeTask task) {
		for (ChallengeQuest quest : task.getQuests().values()) {
			switch (quest.getPersistentState()) {
				case NEW:
					insertQuestEntry(task, quest);
					break;
				case UPDATE_REQUIRED:
					updateQuestEntry(task, quest);
					break;
				default:
					break;
			}
		}
	}

	/**
	 * 插入挑战任务条目。
	 * Inserts a challenge quest entry.
	 * @param task 挑战任务 / challenge task
	 * @param quest 挑战任务条目 / challenge quest
	 */
	private void insertQuestEntry(ChallengeTask task, ChallengeQuest quest) {
		try {
			ChallengeTaskPo po = new ChallengeTaskPo();
			po.setTaskId(task.getTaskId());
			po.setQuestId(quest.getQuestId());
			po.setOwnerId(task.getOwnerId());
			po.setOwnerType(task.getTemplate().getType().toString());
			po.setCompleteCount(quest.getCompleteCount());
			po.setCompleteTime(task.getCompleteTime());
			mapper.insert(po);

			quest.setPersistentState(PersistentState.UPDATED);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.037d4d23c971"), e);
		}
	}

	/**
	 * 更新挑战任务条目；WHERE 与原 SQL 一致，只按 task_id/quest_id/owner_id 定位。
	 * Updates a challenge quest entry; the WHERE matches the legacy SQL, locating the row by
	 * task_id/quest_id/owner_id only (owner_type excluded).
	 * @param task 挑战任务 / challenge task
	 * @param quest 挑战任务条目 / challenge quest
	 */
	private void updateQuestEntry(ChallengeTask task, ChallengeQuest quest) {
		try {
			UpdateChain.of(ChallengeTaskPo.class)
				.set(ChallengeTaskPo::getCompleteCount, quest.getCompleteCount())
				.set(ChallengeTaskPo::getCompleteTime, task.getCompleteTime())
				.where(ChallengeTaskPo::getTaskId).eq(task.getTaskId())
				.and(ChallengeTaskPo::getQuestId).eq(quest.getQuestId())
				.and(ChallengeTaskPo::getOwnerId).eq(task.getOwnerId())
				.update();

			quest.setPersistentState(PersistentState.UPDATED);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b3f513aa406e"), e);
		}
	}
}
