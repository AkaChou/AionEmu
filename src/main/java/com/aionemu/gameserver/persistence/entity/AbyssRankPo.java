package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 abyss_rank 的持久化实体。
 * Persistence entity for table abyss_rank.
 */
@Data
@Table("abyss_rank")
public class AbyssRankPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	private Integer dailyAp;

	private Integer dailyGp;

	private Integer weeklyAp;

	private Integer weeklyGp;

	private Integer ap;

	private Integer gp;

	/** 欧比斯军阶（列 rank 为 MySQL 8 保留字，由 Flex 统一加引号）。 / Abyss rank (the column rank is a MySQL 8 reserved word; Flex quotes it). */
	@Column("rank")
	private Integer rank;

	private Integer topRanking;

	private Integer dailyKill;

	private Integer weeklyKill;

	private Integer allKill;

	private Integer maxRank;

	private Integer lastKill;

	private Integer lastAp;

	private Integer lastGp;

	/**
	 * 最后更新时间。DDL 为 decimal(20,0)，遗留实现以 rs.getLong 读取并 setLong 写入，故用 Long。
	 * Last update time. DDL is decimal(20,0); the legacy implementation read it with rs.getLong
	 * and wrote it with setLong, so this is typed Long.
	 */
	private Long lastUpdate;

	/**
	 * 排行位置列。遗留 INSERT/UPDATE 均不含该列，其取值完全来自 DDL 默认值
	 * （{@code NOT NULL DEFAULT '0'}），后续由 {@code UPDATE_PLAYER_RANK_LIST_JDBC}
	 * （{@code SET old_rank_pos = rank_pos, rank_pos = @a:=@a+1}）重算。
	 * 故退出 Flex 的 INSERT：纳入会显式写入 {@code null} 而违反 NOT NULL 约束。
	 * Rank position column. Neither the legacy INSERT nor UPDATE listed it, so its value came
	 * solely from the DDL default ({@code NOT NULL DEFAULT '0'}) and is recalculated later by
	 * {@code UPDATE_PLAYER_RANK_LIST_JDBC}. Excluded from Flex INSERT: including it would write
	 * an explicit {@code null} and violate the NOT NULL constraint.
	 */
	@Column(insertable = false)
	private Integer rankPos;

	/** 旧排行位置，同上依赖 DDL 默认值并由排行榜重算 SQL 维护。 / Previous rank position; same DDL-default and recalc-SQL handling as {@link #rankPos}. */
	@Column(insertable = false)
	private Integer oldRankPos;

	/** 排行 AP。遗留实现从未读写该列，取值完全来自 DDL 默认值。 / Rank AP; the legacy implementation never read or wrote it, so its value came solely from the DDL default. */
	@Column(insertable = false)
	private Integer rankAp;

}
