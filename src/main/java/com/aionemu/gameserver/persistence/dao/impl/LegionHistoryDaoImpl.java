package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.gameserver.persistence.dao.LegionHistoryDao;
import com.aionemu.gameserver.persistence.entity.LegionHistoryPo;
import com.aionemu.gameserver.persistence.mapper.LegionHistoryMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

/**
 * 表 legion_history 的数据访问实现。
 * Data access implementation for table legion_history.
 */
@Service
@Lazy
public class LegionHistoryDaoImpl
	extends ServiceImpl<LegionHistoryMapper, LegionHistoryPo>
	implements LegionHistoryDao {
}
