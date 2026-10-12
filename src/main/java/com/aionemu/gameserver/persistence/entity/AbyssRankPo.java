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

	private Integer rankPos;

	private Integer oldRankPos;

	private Integer rankAp;

}
