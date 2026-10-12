package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.persistence.dao.LimitedQuestCounterDao;
import com.aionemu.gameserver.persistence.entity.LimitedQuestCounterPo;
import com.aionemu.gameserver.persistence.mapper.LimitedQuestCounterMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 全服共享限量任务名额数据访问实现。
 * Data access implementation for server-wide shared limited quest quotas.
 * <p>
 * 原实现以 {@code TransactionTemplate} 包裹“初始化 + 扣减/恢复”两条语句；此处改用 Spring 的
 * {@code @Transactional}，使 Flex 映射层与该事务共用同一连接。
 * The legacy implementation wrapped "initialize + acquire/recover" in a {@code TransactionTemplate}; here the
 * Spring {@code @Transactional} annotation is used instead so the Flex mapper joins the same transaction.
 * </p>
 */
@Slf4j
@Service
@Lazy
public class LimitedQuestCounterDaoImpl extends ServiceImpl<LimitedQuestCounterMapper, LimitedQuestCounterPo>
	implements LimitedQuestCounterDao {

	@Override
	@Transactional
	public boolean tryAcquire(int questId, int maxCount) {
		if (questId <= 0 || maxCount <= 0) {
			return false;
		}
		try {
			mapper.initialize(questId, maxCount);
			return mapper.acquire(questId) == 1;
		} catch (RuntimeException e) {
			log.error(I18n.get("log.limited_quest.acquire_failed", questId), e);
			return false;
		}
	}

	@Override
	@Transactional
	public boolean recover(int questId, int amount, int maxCount) {
		if (questId <= 0 || amount <= 0 || maxCount <= 0) {
			return false;
		}
		try {
			mapper.initialize(questId, maxCount);
			mapper.recover(amount, maxCount, questId);
			return true;
		} catch (RuntimeException e) {
			log.error(I18n.get("log.limited_quest.recover_failed", questId), e);
			return false;
		}
	}
}
