package com.aionemu.commons.database.dao;

/**
 * DAO 类提供者接口，按服务上下文声明可加载的 DAO 实现
 * DAO class provider that declares loadable DAO implementations for a service context
 */
public interface DAOClassProvider {

	/**
	 * 获取服务上下文名称
	 * Get the service context name
	 *
	 * @return 上下文名称 / Context name
	 */
	String contextName();

	/**
	 * 获取该上下文下的 DAO 实现类数组
	 * Get DAO implementation classes for this context
	 *
	 * @return DAO 实现类数组 / Array of DAO classes
	 */
	Class<?>[] daoClasses();
}
