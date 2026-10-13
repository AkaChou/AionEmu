package com.aionemu.gameserver.persistence.dao.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.Petition;
import com.aionemu.gameserver.model.PetitionStatus;
import com.aionemu.gameserver.persistence.dao.PetitionDao;
import com.aionemu.gameserver.persistence.entity.PetitionPo;
import com.aionemu.gameserver.persistence.mapper.PetitionMapper;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家请愿/客服工单数据访问实现。
 * Data access implementation for player petitions.
 */
@Slf4j
@Service
@Lazy
public class PetitionDaoImpl extends ServiceImpl<PetitionMapper, PetitionPo> implements PetitionDao {

	/** 未结案的请愿状态，对应原 SQL 的 {@code status IN ('PENDING', 'IN_PROGRESS')}。 */
	private static final List<String> OPEN_STATUSES = List.of(PetitionStatus.PENDING.toString(),
		PetitionStatus.IN_PROGRESS.toString());

	@Override
	public synchronized int getNextAvailableId() {
		try {
			Object maxId = mapper.selectObjectByQuery(
				QueryWrapper.create().select(QueryMethods.max(PetitionPo::getId)));
			return maxId == null ? 1 : ((Number) maxId).intValue() + 1;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.fb167a91b87e"), e);
			return 0;
		}
	}

	@Override
	public Petition getPetitionById(int petitionId) {
		try {
			PetitionPo po = mapper.selectOneByQuery(QueryWrapper.create().where(PetitionPo::getId).eq(petitionId));
			return po == null ? null : toPetition(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.72ffd18827d7", petitionId), e);
			return null;
		}
	}

	@Override
	public Set<Petition> getPetitions() {
		Set<Petition> results = new HashSet<>();
		try {
			mapper.selectListByQuery(QueryWrapper.create().where(PetitionPo::getStatus).in(OPEN_STATUSES)
				.orderBy(PetitionPo::getId, true)).forEach(po -> results.add(toPetition(po)));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.70ecf26917fd"), e);
		}
		return results;
	}

	@Override
	public void deletePetition(int playerObjId) {
		try {
			mapper.deleteByQuery(QueryWrapper.create()
				.where(PetitionPo::getPlayerId).eq(playerObjId)
				.and(PetitionPo::getStatus).in(OPEN_STATUSES));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.eb43a2da10f3"), e);
		}
	}

	@Override
	public void insertPetition(Petition petition) {
		try {
			PetitionPo po = new PetitionPo();
			po.setId(petition.petitionId());
			po.setPlayerId(petition.playerObjId());
			po.setType(petition.getPetitionType().getElementId());
			po.setTitle(petition.title());
			po.setMessage(petition.contentText());
			po.setAddData(petition.additionalData());
			po.setTime(System.currentTimeMillis() / 1000);
			po.setStatus(petition.status().toString());
			mapper.insert(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.d40ee1b26acb"), e);
		}
	}

	@Override
	public void setReplied(int petitionId) {
		try {
			UpdateChain.of(PetitionPo.class)
				.set(PetitionPo::getStatus, PetitionStatus.REPLIED.toString())
				.where(PetitionPo::getId).eq(petitionId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.77073357738f"), e);
		}
	}

	/**
	 * 将持久化对象转换为请愿模型，状态按原逻辑映射为客户端状态 ID。
	 * Converts a persistence object into the petition model, mapping the status to the client status id.
	 * @param po 请愿持久化对象 / petition persistence object
	 * @return 申诉 / petition
	 */
	private static Petition toPetition(PetitionPo po) {
		return new Petition(po.getId(), po.getPlayerId(), po.getType(), po.getTitle(), po.getMessage(), po.getAddData(),
			getPetitionStatus(po.getStatus()).getElementId());
	}

	/**
	 * 将状态字符串解析为请愿状态枚举。
	 * Parses a status string into a PetitionStatus enum value.
	 * @param statusValue 状态字符串 / status string
	 * @return 申诉状态 / petition status
	 */
	private static PetitionStatus getPetitionStatus(String statusValue) {
		if (PetitionStatus.PENDING.toString().equals(statusValue)) {
			return PetitionStatus.PENDING;
		} else if (PetitionStatus.IN_PROGRESS.toString().equals(statusValue)) {
			return PetitionStatus.IN_PROGRESS;
		} else {
			return PetitionStatus.PENDING;
		}
	}
}
