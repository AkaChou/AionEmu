package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.persistence.dao.LimitedQuestCounterDao;
import com.aionemu.gameserver.persistence.entity.LimitedQuestCounterPo;
import com.aionemu.gameserver.persistence.mapper.LimitedQuestCounterMapper;
import com.mybatisflex.core.query.FunctionQueryColumn;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.RawQueryColumn;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.core.util.LambdaUtil;
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
			QueryColumn remaining = LambdaUtil.getQueryColumn(LimitedQuestCounterPo::getRemaining);
			return UpdateChain.of(LimitedQuestCounterPo.class)
				.set(remaining, remaining.subtract(1))
				.where(LimitedQuestCounterPo::getQuestId).eq(questId)
				.and(remaining.gt(0))
				.update();
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
			QueryColumn remaining = LambdaUtil.getQueryColumn(LimitedQuestCounterPo::getRemaining);
			QueryColumn capped = new FunctionQueryColumn("LEAST", remaining.add(amount),
				new RawQueryColumn(String.valueOf(maxCount)));
			UpdateChain.of(LimitedQuestCounterPo.class)
				.set(remaining, capped)
				.where(LimitedQuestCounterPo::getQuestId).eq(questId)
				.update();
			return true;
		} catch (RuntimeException e) {
			log.error(I18n.get("log.limited_quest.recover_failed", questId), e);
			return false;
		}
	}
}
