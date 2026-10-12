package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 competition_ranking 的持久化实体。
 * Persistence entity for table competition_ranking.
 */
@Data
@Table("competition_ranking")
public class CompetitionRankingPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private Integer playerId;

	private Integer tableId;

	private Integer rank;

	private Integer lastRank;

	private Integer points;

	private Integer lastPoints;

	private Integer highPoints;

	private Integer lowPoints;

	private Integer positionMatch;

}
