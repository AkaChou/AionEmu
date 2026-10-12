package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.gameserver.persistence.dao.LegionEmblemDao;
import com.aionemu.gameserver.persistence.entity.LegionEmblemPo;
import com.aionemu.gameserver.persistence.mapper.LegionEmblemMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

/**
 * 表 legion_emblems 的数据访问实现。
 * Data access implementation for table legion_emblems.
 */
@Service
@Lazy
public class LegionEmblemDaoImpl
	extends ServiceImpl<LegionEmblemMapper, LegionEmblemPo>
	implements LegionEmblemDao {
}
