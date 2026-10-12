package com.aionemu.gameserver.persistence.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.aionemu.gameserver.persistence.entity.PlayerPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 players 的映射层，仅保留无法用 Flex CRUD 表达的方言/函数 SQL。
 * Mapper for table players, keeping only dialect/function SQL that Flex CRUD cannot express.
 */
public interface PlayerMapper extends BaseMapper<PlayerPo> {

	/**
	 * 批量重置全部角色在线状态（无 WHERE，Flex 运行期拒绝）。
	 * Resets the online flag of every player (no WHERE, rejected by Flex at runtime).
	 *
	 * @param online 目标在线状态 / target online flag
	 * @return 影响行数 / affected rows
	 */
	@Update("UPDATE players SET online = #{online}")
	int setAllOnline(@Param("online") boolean online);

	/**
	 * 统计账号下未删除（或删除时间未到）的角色数量（依赖库内 CURRENT_TIMESTAMP）。
	 * Counts non-deleted characters on an account (relies on in-database CURRENT_TIMESTAMP).
	 *
	 * @param accountId 账号 ID / account id
	 * @return 角色数量 / character count
	 */
	@Select("SELECT COUNT(*) AS cnt FROM `players` WHERE `account_id` = #{accountId} "
		+ "AND (deletion_date IS NULL OR deletion_date > CURRENT_TIMESTAMP)")
	int countCharactersOnAccount(@Param("accountId") int accountId);

	/**
	 * 按种族统计达到经验阈值的去重账号数（COUNT(DISTINCT)）。
	 * Counts distinct accounts of a race meeting the exp threshold (COUNT(DISTINCT)).
	 *
	 * @param race   阵营名 / race name
	 * @param minExp 最低经验阈值 / minimum exp threshold
	 * @return 账号数量 / account count
	 */
	@Select("SELECT COUNT(DISTINCT(`account_name`)) AS `count` FROM `players` "
		+ "WHERE `race` = #{race} AND `exp` >= #{minExp}")
	int countAccountsForRace(@Param("race") String race, @Param("minExp") long minExp);

	/**
	 * 查询因长时间未登录而待删除的角色 ID（UNIX_TIMESTAMP/CURDATE 方言函数）。
	 * Lists player IDs inactive long enough to be deleted (UNIX_TIMESTAMP/CURDATE dialect functions).
	 *
	 * @param daysOfInactivity 不活跃天数阈值 / inactivity threshold in days
	 * @return 待删除角色 ID 列表 / list of player ids to delete
	 */
	@Select("SELECT id FROM players WHERE UNIX_TIMESTAMP(CURDATE()) - "
		+ "UNIX_TIMESTAMP(last_online) > #{daysOfInactivity} * 24 * 60 * 60")
	List<Integer> selectPlayersToDelete(@Param("daysOfInactivity") int daysOfInactivity);

	/**
	 * 同 {@link #selectPlayersToDelete(int)}，但限制最大返回条数。
	 * Same as {@link #selectPlayersToDelete(int)}, limited to a maximum number of rows.
	 *
	 * @param daysOfInactivity 不活跃天数阈值 / inactivity threshold in days
	 * @param limitation       最大返回条数 / maximum rows
	 * @return 待删除角色 ID 列表 / list of player ids to delete
	 */
	@Select("SELECT id FROM players WHERE UNIX_TIMESTAMP(CURDATE()) - "
		+ "UNIX_TIMESTAMP(last_online) > #{daysOfInactivity} * 24 * 60 * 60 "
		+ "LIMIT #{limitation}")
	List<Integer> selectPlayersToDeleteLimited(@Param("daysOfInactivity") int daysOfInactivity,
		@Param("limitation") int limitation);

	/**
	 * 更新玩家任务奖励列（含 durable 奖励的 exp/aura_of_growth，原生 JDBC 常量）。
	 * Updates player quest-reward columns (raw JDBC constant).
	 */
	String UPDATE_QUEST_CURRENCIES_QUERY_JDBC = "UPDATE players SET exp = ?, dp = ?, creativity_point = ?, "
		+ "abyss_favor = ?, aura_of_growth = ? WHERE id = ?";

	/**
	 * 在任务事务中持久化高阶守护者晋升（GREATEST 取高值，原生 JDBC 常量）。
	 * Persists ArchDaeva promotion in the quest transaction (raw JDBC constant).
	 */
	String PROMOTE_ARCHDAEVA_QUERY_JDBC = "UPDATE players SET exp = GREATEST(exp, ?), is_archdaeva = TRUE WHERE id = ?";
}
