package com.aionemu.gameserver.persistence.dao.impl;

import com.aionemu.gameserver.services.LegionService;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.model.team.legion.LegionMemberEx;
import com.aionemu.gameserver.model.team.legion.LegionRank;
import com.aionemu.gameserver.persistence.dao.LegionMemberDao;
import com.aionemu.gameserver.persistence.entity.LegionMemberPo;
import com.aionemu.gameserver.persistence.mapper.LegionMemberMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 军团成员数据访问的 MyBatis-Flex 实现。
 * MyBatis-Flex implementation of the legion member data access layer.
 */
@Slf4j
@Service
@Lazy
public class LegionMemberDaoImpl extends ServiceImpl<LegionMemberMapper, LegionMemberPo> implements LegionMemberDao {

	@Override
	public boolean isIdUsed(final int playerObjId) {
		try {
			return mapper.selectCountByQuery(QueryWrapper.create()
				.where(LegionMemberPo::getPlayerId).eq(playerObjId)) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.808625122cbd", playerObjId), e);
			return true;
		}
	}

	@Override
	public void saveNewLegionMember(final LegionMember legionMember) {
		try {
			LegionMemberPo po = new LegionMemberPo();
			po.setLegionId(legionMember.getLegion().getLegionId());
			po.setPlayerId(legionMember.getObjectId());
			po.setRank(legionMember.getRank().toString());
			// 仅插入 legion_id/player_id/rank，其余列依赖数据库默认值 / Insert only legion_id/player_id/rank
			mapper.insertSelective(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.9e9361f5deaa", legionMember.getObjectId()), e);
		}
	}

	@Override
	public void storeLegionMember(final int playerId, final LegionMember legionMember) {
		try {
			UpdateChain.of(LegionMemberPo.class)
				.set(LegionMemberPo::getNickname, legionMember.getNickname())
				.set(LegionMemberPo::getRank, legionMember.getRank().toString())
				.set(LegionMemberPo::getSelfintro, legionMember.getSelfIntro())
				.set(LegionMemberPo::getChallengeScore, legionMember.getChallengeScore())
				.where(LegionMemberPo::getPlayerId).eq(playerId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a9693496afec", playerId), e);
		}
	}

	@Override
	public LegionMember loadLegionMember(final int playerObjId) {
		if (playerObjId == 0) {
			return null;
		}

		try {
			LegionMemberPo po = mapper.selectOneByQuery(QueryWrapper.create()
				.where(LegionMemberPo::getPlayerId).eq(playerObjId));
			if (po == null) {
				return null;
			}

			LegionMember legionMember = new LegionMember(playerObjId);
			try {
				legionMember.setRank(LegionRank.valueOf(po.getRank()));
			} catch (IllegalArgumentException e) {
				log.warn(I18n.get("log.70eb91ce3a9a", playerObjId, po.getRank()));
				legionMember.setRank(LegionRank.VOLUNTEER);
			}
			legionMember.setNickname(po.getNickname());
			legionMember.setSelfIntro(po.getSelfintro());
			legionMember.setChallengeScore(po.getChallengeScore());
			legionMember.setLegion(LegionService.getInstance().getLegion(po.getLegionId()));
			return legionMember;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b98d06a4d45a", playerObjId), e);
			return null;
		}
	}

	@Override
	public LegionMemberEx loadLegionMemberEx(final int playerObjId) {
		try {
			Map<String, Object> row = mapper.selectLegionMemberExById(playerObjId);
			if (row == null) {
				return null;
			}

			LegionMemberEx legionMemberEx = new LegionMemberEx(playerObjId);
			legionMemberEx.setName((String) row.get("name"));
			legionMemberEx.setExp(toLong(row.get("exp")));
			try {
				legionMemberEx.setPlayerClass(PlayerClass.valueOf((String) row.get("player_class")));
			} catch (IllegalArgumentException e) {
				log.warn(I18n.get("log.40c87a64d2f0", playerObjId, row.get("player_class")));
			}
			legionMemberEx.setLastOnline((Timestamp) row.get("last_online"));
			legionMemberEx.setWorldId(toInt(row.get("world_id")));

			try {
				legionMemberEx.setRank(LegionRank.valueOf((String) row.get("rank")));
			} catch (IllegalArgumentException e) {
				log.warn(I18n.get("log.70eb91ce3a9a", playerObjId, row.get("rank")));
				legionMemberEx.setRank(LegionRank.VOLUNTEER);
			}
			legionMemberEx.setNickname((String) row.get("nickname"));
			legionMemberEx.setSelfIntro((String) row.get("selfintro"));
			legionMemberEx.setLegion(LegionService.getInstance().getLegion(toInt(row.get("legion_id"))));
			return legionMemberEx;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.dfcfd04a2c84", playerObjId), e);
			return null;
		}
	}

	@Override
	public LegionMemberEx loadLegionMemberEx(final String playerName) {
		try {
			Map<String, Object> row = mapper.selectLegionMemberExByName(playerName);
			if (row == null) {
				return null;
			}

			LegionMemberEx legionMember = new LegionMemberEx(playerName);
			legionMember.setObjectId(toInt(row.get("id")));
			legionMember.setExp(toLong(row.get("exp")));
			try {
				legionMember.setPlayerClass(PlayerClass.valueOf((String) row.get("player_class")));
			} catch (IllegalArgumentException e) {
				log.warn(I18n.get("log.40c87a64d2f0", playerName, row.get("player_class")));
			}
			legionMember.setLastOnline((Timestamp) row.get("last_online"));
			legionMember.setWorldId(toInt(row.get("world_id")));

			try {
				legionMember.setRank(LegionRank.valueOf((String) row.get("rank")));
			} catch (IllegalArgumentException e) {
				log.warn(I18n.get("log.70eb91ce3a9a", playerName, row.get("rank")));
				legionMember.setRank(LegionRank.VOLUNTEER);
			}
			legionMember.setNickname((String) row.get("nickname"));
			legionMember.setSelfIntro((String) row.get("selfintro"));
			legionMember.setLegion(LegionService.getInstance().getLegion(toInt(row.get("legion_id"))));
			return legionMember;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.29172f0eb8e2", playerName), e);
			return null;
		}
	}

	@Override
	public ArrayList<Integer> loadLegionMembers(final int legionId) {
		final ArrayList<Integer> legionMembers = new ArrayList<>();
		try {
			mapper.selectListByQuery(QueryWrapper.create().where(LegionMemberPo::getLegionId).eq(legionId))
				.forEach(po -> legionMembers.add(po.getPlayerId()));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.580dbf1f5523", legionId), e);
		}

		return legionMembers.isEmpty() ? null : legionMembers;
	}

	@Override
	public void deleteLegionMember(int playerObjId) {
		try {
			mapper.deleteByQuery(QueryWrapper.create().where(LegionMemberPo::getPlayerId).eq(playerObjId));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.d5662c3ac841", playerObjId), e);
		}
	}

	@Override
	public int[] getUsedIDs() {
		try {
			List<LegionMemberPo> members = mapper.selectListByQuery(QueryWrapper.create());
			int[] result = new int[members.size()];
			for (int i = 0; i < members.size(); i++) {
				result[i] = members.get(i).getPlayerId();
			}
			return result;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.fec518ccee9a"), e);
			return new int[0];
		}
	}

	private static int toInt(Object value) {
		return value == null ? 0 : ((Number) value).intValue();
	}

	private static long toLong(Object value) {
		return value == null ? 0L : ((Number) value).longValue();
	}
}
