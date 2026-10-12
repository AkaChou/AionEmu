package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.gameserver.persistence.dao.LegionJoinRequestDao;
import com.aionemu.gameserver.persistence.entity.LegionJoinRequestPo;
import com.aionemu.gameserver.persistence.mapper.LegionJoinRequestMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

/**
 * 表 legion_join_requests 的数据访问实现。
 * Data access implementation for table legion_join_requests.
 */
@Service
@Lazy
public class LegionJoinRequestDaoImpl
	extends ServiceImpl<LegionJoinRequestMapper, LegionJoinRequestPo>
	implements LegionJoinRequestDao {
}
