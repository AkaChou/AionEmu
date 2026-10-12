package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.gameserver.persistence.dao.SkillMotionDao;
import com.aionemu.gameserver.persistence.entity.SkillMotionPo;
import com.aionemu.gameserver.persistence.mapper.SkillMotionMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

/**
 * 表 skill_motions 的数据访问实现。
 * Data access implementation for table skill_motions.
 */
@Service
@Lazy
public class SkillMotionDaoImpl
	extends ServiceImpl<SkillMotionMapper, SkillMotionPo>
	implements SkillMotionDao {
}
