package com.aionemu.gameserver.model.gameobjects.player;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Objects;
import java.util.function.IntFunction;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 任务状态列表。
 * Quest State List game object.
 *
 * @author MrPoke
 */
@Slf4j
public class QuestStateList {


	private final SortedMap<Integer, QuestState> _quests;
	private final IntFunction<QuestMetadata> metadata;

	/**
	 * 创建空任务列表。
	 * Creates an empty quests list
	 */
	public QuestStateList() {
		this(questId -> GameEngineServices.questEngine().questCatalog().findMetadata(questId).orElse(null));
	}

	QuestStateList(IntFunction<QuestMetadata> metadata) {
		_quests = new TreeMap<Integer, QuestState>();
		this.metadata = Objects.requireNonNull(metadata, "metadata");
	}

	/** 添加任务。 / Adds quest. */
	public synchronized boolean addQuest(int questId, QuestState questState) {
		if (_quests.containsKey(questId)) {
			log.warn(I18n.get("log.51dd791f0695"));
			return false;
		}
		_quests.put(questId, questState);
		return true;
	}

	/** 移除任务。 / Removes quest. */
	public synchronized boolean removeQuest(int questId) {
		if (_quests.containsKey(questId)) {
			_quests.remove(questId);
			return true;
		}
		return false;
	}

	/** 获取任务状态。 / Returns the quest state. */
	public QuestState getQuestState(int questId) {
		return _quests.get(questId);
	}

	/** 返回全部任务状态 / Returns the all quest state*/
	public Collection<QuestState> getAllQuestState() {
		return _quests.values();
	}

	/** 返回 all finished quests / Returns the all finished quests */
	public List<QuestState> getAllFinishedQuests() {
		List<QuestState> completeQuestList = new ArrayList<QuestState>();
		for (QuestState qs : _quests.values()) {
			if (qs.getStatus() == QuestStatus.COMPLETE) {
				completeQuestList.add(qs);
			}
		}
		return completeQuestList;
	}

	/*
	 * Issue #13 fix Used by the QuestService to check the amount of normal quests
	 * in the player's list
	 * 
	 * @author vlog
	 */
	public int getNormalQuestListSize() {
		return this.getNormalQuests().size();
	}

	/*
	 * Issue #13 fix Returns the list of normal quests
	 * 
	 * @author vlog
	 */
	public Collection<QuestState> getNormalQuests() {
		Collection<QuestState> l = new ArrayList<QuestState>();

		for (QuestState qs : this.getAllQuestState()) {
			QuestMetadata questMetadata = metadata.apply(qs.getQuestId());
			QuestStatus s = qs.getStatus();

			if (s != QuestStatus.COMPLETE && s != QuestStatus.LOCKED && s != QuestStatus.NONE
					&& questMetadata != null && "QUEST".equals(questMetadata.category())) {
				l.add(qs);
			}
		}
		return l;
	}

	/*
	 * Returns true if there is a quest in the list with this id Used by the
	 * QuestService
	 * 
	 * @author vlog
	 */
	public boolean hasQuest(int questId) {
		return _quests.containsKey(questId);
	}

	/*
	 * Change the old value of the quest status to the new one Used by the
	 * QuestService
	 * 
	 * @author vlog
	 */
	public void changeQuestStatus(Integer key, QuestStatus newStatus) {
		_quests.get(key).setStatus(newStatus);
	}

	/** 大小 / size. */
	public int size() {
		return this._quests.size();
	}

	/** 返回 quests / Returns the quests */
	public SortedMap<Integer, QuestState> getQuests() {
		return this._quests;
	}
}
