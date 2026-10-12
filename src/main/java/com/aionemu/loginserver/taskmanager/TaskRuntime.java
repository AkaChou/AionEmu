package com.aionemu.loginserver.taskmanager;

import com.aionemu.loginserver.Shutdown;
import com.aionemu.loginserver.persistence.dao.AccountDataDao;
import com.aionemu.loginserver.utils.ThreadPoolManager;

/**
 * 数据库任务的运行时依赖：反射创建的触发器/处理器无法构造注入，改由
 * {@link TaskFromDBManager} 装配本上下文。
 * Runtime dependencies for DB tasks: reflectively created triggers/handlers cannot use constructor
 * injection, so {@link TaskFromDBManager} injects this context instead.
 *
 * @param threadPoolManager 登录服线程池 / login-server thread pool
 * @param accountDao 账号 DAO / account DAO
 * @param shutdown 关机协调器 / shutdown coordinator
 */
public record TaskRuntime(ThreadPoolManager threadPoolManager, AccountDataDao accountDao, Shutdown shutdown) {
}
