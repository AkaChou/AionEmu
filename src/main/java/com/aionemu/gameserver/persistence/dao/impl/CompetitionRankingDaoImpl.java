package com.aionemu.gameserver.persistence.dao.impl;

import java.util.ArrayList;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.ranking.Arena6V6Ranking;
import com.aionemu.gameserver.model.gameobjects.player.ranking.ArenaOfTenacityRank;
import com.aionemu.gameserver.model.gameobjects.player.ranking.GoldArenaRank;
import com.aionemu.gameserver.model.gameobjects.player.ranking.TowerOfChallengeRank;
import com.aionemu.gameserver.model.ranking.SeasonRankingResult;
import com.aionemu.gameserver.persistence.dao.CompetitionRankingDao;
import com.aionemu.gameserver.persistence.entity.CompetitionRankingPo;
import com.aionemu.gameserver.persistence.mapper.CompetitionRankingMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 赛季竞技排行数据访问实现。
 * 覆盖黄金竞技场、挑战之塔、孤独竞技场、6v6 等 competition_ranking 表数据。
 * Data access implementation for season arena ranking over the competition_ranking table.
 * @author Wnkrz
 */
@Slf4j
@Service
@Lazy
public class CompetitionRankingDaoImpl extends ServiceImpl<CompetitionRankingMapper, CompetitionRankingPo>
	implements CompetitionRankingDao {

	/** 挑战之塔（Tower of Challenge）所属排行表 ID。 / Ranking table id of the Tower of Challenge. */
	private static final int TOWER_TABLE_ID = 2;

	/**
	 * 获取指定排行表的前 300 名竞技排行玩家。
	 * Loads the top 300 competition ranking players for the given table id.
	 * @param tableId 排名表 ID / ranking table id
	 * @return 排行结果列表 / ranking result list
	 */
	@Override
	public ArrayList<SeasonRankingResult> getCompetitionRankingPlayers(int tableId) {
		ArrayList<SeasonRankingResult> results = new ArrayList<>();

		try {
			mapper.selectCompetitionRankingPlayers(tableId).forEach(row -> {
				String playerClassStr = (String) row.get("player_class");

				PlayerClass playerClass;
				try {
					playerClass = PlayerClass.getPlayerClassByString(playerClassStr);
				} catch (IllegalArgumentException e) {
					log.warn(I18n.get("log.9e67b184f360", playerClassStr));
					return;
				}

				Race race = Race.getRaceByString((String) row.get("race"));

				if (playerClass != null && race != null) {
					results.add(new SeasonRankingResult((String) row.get("name"), intValue(row.get("last_rank")),
						intValue(row.get("rank")), intValue(row.get("points")), playerClass, race.getRaceId(),
						intValue(row.get("id"))));
				}
			});
		} catch (DataAccessException e) {
			log.error(I18n.get("log.2d8dc2963806", tableId), e);
		}

		return results;
	}

	/**
	 * 加载玩家黄金竞技场排行数据；无记录时返回 NEW 状态的空对象。
	 * Loads the player's Gold Arena rank; returns a NEW empty rank when none exists.
	 * @param playerId 玩家对象 ID / player object id
	 * @param tableId 排名表 ID / ranking table id
	 * @return 黄金竞技场排行 / Gold Arena rank
	 */
	@Override
	public GoldArenaRank loadGoldArenaRank(int playerId, int tableId) {
		try {
			CompetitionRankingPo po = selectRankingRow(playerId, tableId);

			if (po == null) {
				GoldArenaRank empty = new GoldArenaRank(0, 0, 0, 0, 0, 0, 0);
				empty.setPersistentState(PersistentState.NEW);
				return empty;
			}
			GoldArenaRank arenaRank = new GoldArenaRank(intValue(po.getRank()), intValue(po.getLastRank()),
				intValue(po.getPoints()), intValue(po.getLastPoints()), intValue(po.getHighPoints()),
				intValue(po.getLowPoints()), intValue(po.getPositionMatch()));
			arenaRank.setPersistentState(PersistentState.UPDATED);
			return arenaRank;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.d516d865c7d9", playerId, tableId), e);
			GoldArenaRank fallback = new GoldArenaRank(0, 0, 0, 0, 0, 0, 0);
			fallback.setPersistentState(PersistentState.NEW);
			return fallback;
		}
	}

	/**
	 * 加载玩家孤独竞技场排行数据；无记录时返回 NEW 状态的空对象。
	 * Loads the player's Arena of Tenacity rank; returns a NEW empty rank when none exists.
	 * @param playerId 玩家对象 ID / player object id
	 * @param tableId 排名表 ID / ranking table id
	 * @return 孤独竞技场排行 / Arena of Tenacity rank
	 */
	@Override
	public ArenaOfTenacityRank loadArenaOfTenacityRank(int playerId, int tableId) {
		try {
			CompetitionRankingPo po = selectRankingRow(playerId, tableId);

			if (po == null) {
				ArenaOfTenacityRank empty = new ArenaOfTenacityRank(0, 0, 0, 0, 0, 0, 0);
				empty.setPersistentState(PersistentState.NEW);
				return empty;
			}
			ArenaOfTenacityRank ranking = new ArenaOfTenacityRank(intValue(po.getRank()), intValue(po.getLastRank()),
				intValue(po.getPoints()), intValue(po.getLastPoints()), intValue(po.getHighPoints()),
				intValue(po.getLowPoints()), intValue(po.getPositionMatch()));
			ranking.setPersistentState(PersistentState.UPDATED);
			return ranking;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.1c6ce1dd5b7e", playerId, tableId), e);
			ArenaOfTenacityRank fallback = new ArenaOfTenacityRank(0, 0, 0, 0, 0, 0, 0);
			fallback.setPersistentState(PersistentState.NEW);
			return fallback;
		}
	}

	/**
	 * 加载玩家挑战之塔排行数据；无记录时返回 NEW 状态的空对象。
	 * Loads the player's Tower of Challenge rank; returns a NEW empty rank when none exists.
	 * @param playerId 玩家对象 ID / player object id
	 * @param tableId 排名表 ID / ranking table id
	 * @return 挑战之塔排行 / Tower of Challenge rank
	 */
	@Override
	public TowerOfChallengeRank loadTowerOfChallengeRank(int playerId, int tableId) {
		try {
			CompetitionRankingPo po = selectRankingRow(playerId, tableId);

			if (po == null) {
				TowerOfChallengeRank empty = new TowerOfChallengeRank(0, 0, 0, 0, 0, 0);
				empty.setPersistentState(PersistentState.NEW);
				return empty;
			}
			TowerOfChallengeRank ranking = new TowerOfChallengeRank(intValue(po.getRank()), intValue(po.getLastRank()),
				intValue(po.getPoints()), intValue(po.getLastPoints()), intValue(po.getHighPoints()),
				intValue(po.getLowPoints()));
			ranking.setPersistentState(PersistentState.UPDATED);
			return ranking;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a937504b345f", playerId, tableId), e);
			TowerOfChallengeRank fallback = new TowerOfChallengeRank(0, 0, 0, 0, 0, 0);
			fallback.setPersistentState(PersistentState.NEW);
			return fallback;
		}
	}

	/**
	 * 加载玩家 6v6 竞技场排行数据；无记录时返回 NEW 状态的空对象。
	 * Loads the player's 6v6 Arena rank; returns a NEW empty rank when none exists.
	 * @param playerId 玩家对象 ID / player object id
	 * @param tableId 排名表 ID / ranking table id
	 * @return 6v6 竞技场排行 / 6v6 Arena rank
	 */
	@Override
	public Arena6V6Ranking loadArena6v6Rank(int playerId, int tableId) {
		try {
			CompetitionRankingPo po = selectRankingRow(playerId, tableId);

			if (po == null) {
				Arena6V6Ranking empty = new Arena6V6Ranking(0, 0, 0, 0, 0, 0, 0);
				empty.setPersistentState(PersistentState.NEW);
				return empty;
			}
			Arena6V6Ranking ranking = new Arena6V6Ranking(intValue(po.getRank()), intValue(po.getLastRank()),
				intValue(po.getPoints()), intValue(po.getLastPoints()), intValue(po.getHighPoints()),
				intValue(po.getLowPoints()), intValue(po.getPositionMatch()));
			ranking.setPersistentState(PersistentState.UPDATED);
			return ranking;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a8b4326794d1", playerId, tableId), e);
			Arena6V6Ranking fallback = new Arena6V6Ranking(0, 0, 0, 0, 0, 0, 0);
			fallback.setPersistentState(PersistentState.NEW);
			return fallback;
		}
	}

	/**
	 * 按持久化状态保存玩家挑战之塔排行（table_id = 2）。
	 * Stores the player's Tower of Challenge rank by persistent state (table_id = 2).
	 * @param player 玩家 / player
	 */
	@Override
	public void storeTowerRank(Player player) {
		TowerOfChallengeRank rank = player.getTowerRank();
		if (rank == null) {
			return;
		}

		boolean result;
		switch (rank.getPersistentState()) {
			case NEW:
				result = addTowerRank(player.getObjectId(), rank);
				break;
			case UPDATE_REQUIRED:
				result = updateTowerRank(player.getObjectId(), rank);
				break;
			default:
				return;
		}

		if (result) {
			rank.setPersistentState(PersistentState.UPDATED);
		}
	}

	/**
	 * 查询玩家的排行行（competition_ranking 单表）。
	 * Selects the player's ranking row (single competition_ranking table).
	 * @param playerId 玩家对象 ID / player object id
	 * @param tableId 排名表 ID / ranking table id
	 * @return 排行行；不存在为 null / ranking row, or null
	 */
	private CompetitionRankingPo selectRankingRow(int playerId, int tableId) {
		return mapper.selectOneByQuery(QueryWrapper.create()
			.where(CompetitionRankingPo::getPlayerId).eq(playerId)
			.and(CompetitionRankingPo::getTableId).eq(tableId));
	}

	/**
	 * 插入挑战之塔排行记录（table_id = 2）。
	 * Inserts a Tower of Challenge ranking row (table_id = 2).
	 * @param objectId 玩家对象 ID / player object id
	 * @param rank 排行 / ranking
	 * @return 是否成功 / whether successful
	 */
	private boolean addTowerRank(final int objectId, final TowerOfChallengeRank rank) {
		try {
			CompetitionRankingPo po = new CompetitionRankingPo();
			po.setPlayerId(objectId);
			po.setTableId(TOWER_TABLE_ID);
			po.setRank(rank.getRank());
			po.setLastRank(rank.getBestRank());
			po.setPoints(rank.getCurrentTime());
			po.setLastPoints(rank.getLastTime());
			po.setHighPoints(rank.getBestTime());
			po.setLowPoints(rank.getLowRank());
			po.setPositionMatch(0);
			return mapper.insert(po) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.33e0d184fd35", objectId), e);
			return false;
		}
	}

	/**
	 * 更新挑战之塔排行记录（table_id = 2）。
	 * Updates the Tower of Challenge ranking row (table_id = 2).
	 * @param objectId 玩家对象 ID / player object id
	 * @param rank 排行 / ranking
	 * @return 是否成功 / whether successful
	 */
	private boolean updateTowerRank(final int objectId, TowerOfChallengeRank rank) {
		try {
			return UpdateChain.of(CompetitionRankingPo.class)
				.set(CompetitionRankingPo::getRank, rank.getRank())
				.set(CompetitionRankingPo::getLastRank, rank.getBestRank())
				.set(CompetitionRankingPo::getPoints, rank.getCurrentTime())
				.set(CompetitionRankingPo::getLastPoints, rank.getLastTime())
				.set(CompetitionRankingPo::getHighPoints, rank.getBestTime())
				.set(CompetitionRankingPo::getLowPoints, rank.getLowRank())
				.set(CompetitionRankingPo::getPositionMatch, 0)
				.where(CompetitionRankingPo::getPlayerId).eq(objectId)
				.and(CompetitionRankingPo::getTableId).eq(TOWER_TABLE_ID)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.525c0a41a0d5", objectId), e);
			return false;
		}
	}

	/**
	 * 空值安全取整：与原 rs.getInt 的 NULL→0 语义一致。
	 * Null-safe int, matching the original rs.getInt NULL→0 semantics.
	 */
	private static int intValue(Object value) {
		return value == null ? 0 : ((Number) value).intValue();
	}
}
