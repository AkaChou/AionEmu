package com.aionemu.gameserver.dao.impl;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.challenge.ChallengeQuest;
import com.aionemu.gameserver.model.challenge.ChallengeTask;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.templates.challenge.ChallengeQuestTemplate;
import com.aionemu.gameserver.model.templates.challenge.ChallengeType;

import lombok.extern.slf4j.Slf4j;

/**
 * 挑战任务 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of ChallengeTasksDAO using JdbcClient.
 * @author ViAl
 */
@Slf4j
public class ChallengeTasksDAO extends com.aionemu.gameserver.dao.ChallengeTasksDAO {

    /** 查询挑战任务 SQL / Select challenge tasks SQL*/
    private static final String SELECT_QUERY = "SELECT * FROM `challenge_tasks` WHERE `owner_id` = ? AND `owner_type` = ?";

    /** 插入挑战任务 SQL / Insert challenge task SQL*/
    private static final String INSERT_QUERY = "INSERT INTO `challenge_tasks` (`task_id`, `quest_id`, `owner_id`, `owner_type`, `complete_count`, `complete_time`) VALUES (?, ?, ?, ?, ?, ?)";

    /** 更新挑战任务 SQL / Update challenge task SQL*/
    private static final String UPDATE_QUERY = "UPDATE `challenge_tasks` SET `complete_count` = ?, `complete_time` = ? WHERE `task_id` = ? AND `quest_id` = ? AND `owner_id` = ?";

    private final JdbcClient jdbcClient;

    public ChallengeTasksDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    public ChallengeTasksDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 按所有者与类型加载挑战任务。
     * Loads challenge tasks by owner and type.
     * @param ownerId 房主 ID / owner id
     * @param type 挑战类型 / challenge type
     * @return 任务映射 / task map
     */
    @Override
    public Map<Integer, ChallengeTask> load(int ownerId, ChallengeType type) {
        Map<Integer, ChallengeTask> tasks = new LinkedHashMap<>();

        try {
            jdbcClient.sql(SELECT_QUERY)
                .param(ownerId)
                .param(type.toString())
                .query((rset, rowNum) -> {
                    int taskId = rset.getInt("task_id");
                    int questId = rset.getInt("quest_id");
                    int completeCount = rset.getInt("complete_count");
                    Timestamp date = rset.getTimestamp("complete_time");

                    ChallengeQuestTemplate template = DataManager.CHALLENGE_DATA.getQuestByQuestId(questId);
                    ChallengeQuest quest = new ChallengeQuest(template, completeCount);
                    quest.setPersistentState(PersistentState.UPDATED);

                    if (!tasks.containsKey(taskId)) {
                        Map<Integer, ChallengeQuest> quests = new HashMap<>(2);
                        quests.put(quest.getQuestId(), quest);
                        ChallengeTask task = new ChallengeTask(taskId, ownerId, quests, date);
                        tasks.put(taskId, task);
                    } else {
                        tasks.get(taskId).getQuests().put(questId, quest);
                    }
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.617c98736ef3"), e);
        }
        return tasks;
    }

    /**
     * 按持久化状态存储挑战任务。
     * Stores a challenge task according to quest persistent states.
     * @param task 挑战任务 / challenge task
     */
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
     * 插入挑战任务任务条目。
     * Inserts a challenge quest entry.
     * @param task 挑战任务 / challenge task
     * @param quest 挑战任务 / challenge quest
     */
    private void insertQuestEntry(ChallengeTask task, ChallengeQuest quest) {
        try {
            jdbcClient.sql(INSERT_QUERY)
                .param(task.getTaskId())
                .param(quest.getQuestId())
                .param(task.getOwnerId())
                .param(task.getTemplate().getType().toString())
                .param(quest.getCompleteCount())
                .param(task.getCompleteTime())
                .update();

            quest.setPersistentState(PersistentState.UPDATED);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.037d4d23c971"), e);
        }
    }

    /**
     * 更新挑战任务任务条目。
     * Updates a challenge quest entry.
     * @param task 挑战任务 / challenge task
     * @param quest 挑战任务 / challenge quest
     */
    private void updateQuestEntry(ChallengeTask task, ChallengeQuest quest) {
        try {
            jdbcClient.sql(UPDATE_QUERY)
                .param(quest.getCompleteCount())
                .param(task.getCompleteTime())
                .param(task.getTaskId())
                .param(quest.getQuestId())
                .param(task.getOwnerId())
                .update();

            quest.setPersistentState(PersistentState.UPDATED);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.b3f513aa406e"), e);
        }
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     * @param databaseName 数据库名 / database name
     * @param majorVersion 主版本 / major version
     * @param minorVersion 次版本 / minor version
     * @return 是否支持 / whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
