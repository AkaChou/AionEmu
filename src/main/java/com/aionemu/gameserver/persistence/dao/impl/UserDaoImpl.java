package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.gameserver.persistence.dao.UserDao;
import com.aionemu.gameserver.persistence.entity.UserPo;
import com.aionemu.gameserver.persistence.mapper.UserMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

/**
 * 表 users 的数据访问实现。
 * Data access implementation for table users.
 */
@Service
@Lazy
public class UserDaoImpl
	extends ServiceImpl<UserMapper, UserPo>
	implements UserDao {
}
