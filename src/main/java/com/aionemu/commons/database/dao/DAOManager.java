package com.aionemu.commons.database.dao;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import static com.aionemu.commons.database.DatabaseFactory.getDatabaseMajorVersion;
import static com.aionemu.commons.database.DatabaseFactory.getDatabaseMinorVersion;
import static com.aionemu.commons.database.DatabaseFactory.getDatabaseName;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.commons.services.ServiceContext;

/**
 * DAO 管理器类
 * DAO Manager Class
 *
 * 这个类负责管理所有 DAO 实现类的注册和获取。它维护了一个 DAO 实现类的注册表，
 * 并提供了注册、注销和获取 DAO 实现的方法。
 * This class manages the registration and retrieval of all DAO implementations.
 * It maintains a registry of DAO implementations and provides methods for
 * registering, unregistering and retrieving DAO implementations.
 *
 * @author SoulKeeper
 * @author Saelya
 */
@Slf4j
public class DAOManager {


    /**
 * 已注册的 DAO 集合
     * Collection of registered DAOs
     */
    private static final Map<String, DaoState> states = new ConcurrentHashMap<String, DaoState>();

    /**
 * 初始化 DAOManager
     * Initializes DAOManager
     */
    public static void init() {
        String context = ServiceContext.current();
        DaoState state = new DaoState();
        DaoState oldState = states.putIfAbsent(context, state);
        if (oldState != null) {
            return;
        }
        try {
            loadCompiledDaos(context);
        } catch (RuntimeException e) {
            states.remove(context);
            throw new Error(e.getMessage(), e);
        } catch (Exception e) {
            states.remove(context);
            throw new Error("A fatal error occurred during loading database handlers", e);
        }

        log.info(I18n.get("log.2041e56fee03", state.daoMap.size(), context));
    }

    /**
 * 关闭 DAOManager
     * Shuts down DAOManager
     */
    public static void shutdown() {
        DaoState state = states.remove(ServiceContext.current());
        if (state != null) {
            state.daoMap.clear();
        }
    }

    /**
     * 判断当前服务上下文的 DAOManager 是否已初始化
     * Check whether DAOManager is initialized for the current service context
     *
     * @return 已初始化返回 true / True if initialized
     */
    public static boolean isInitialized() {
        return states.containsKey(ServiceContext.current());
    }

    /**
 * 根据 DAO 类获取其实现
     * Returns DAO implementation by DAO class
     *
 * DAO class
 * DAO type
 * DAO implementation
 *
 * @param clazz
 * @throws DAONotFoundException 如果未找到 DAO 实现 / If DAO implementation not found
     */
    @SuppressWarnings("unchecked")
    public static <T extends DAO> T getDAO(Class<T> clazz) throws DAONotFoundException {
        DAO result = state().daoMap.get(clazz.getName());

        if (result == null) {
            String s = I18n.get("log.2f5e195e6e70", clazz.getSimpleName());
            log.error(s);
            throw new DAONotFoundException(s);
        }

        return (T) result;
    }

    /**
 * 注册 DAO 实现类
     * Registers DAO implementation
     *
 * DAO implementation class
 * If DAO is already registered。
 * If error during DAO instantiation。
 * If error during DAO instantiation。
     */
    public static void registerDAO(Class<? extends DAO> daoClass) throws DAOAlreadyRegisteredException, ReflectiveOperationException {
        DAO dao = daoClass.getDeclaredConstructor().newInstance();

        if (!dao.supports(getDatabaseName(), getDatabaseMajorVersion(), getDatabaseMinorVersion())) {
            return;
        }

        DaoState state = state();
        synchronized (DAOManager.class) {
            DAO oldDao = state.daoMap.get(dao.getClassName());
            if (oldDao != null) {
                String s = I18n.get("log.cf94f1177f80", dao.getClassName(), oldDao.getClass().getName(), daoClass.getName());
                log.error(s);
                throw new DAOAlreadyRegisteredException(s);
            }
            state.daoMap.put(dao.getClassName(), dao);
        }

        if (log.isDebugEnabled()) {
            log.debug("DAO " + dao.getClassName() + " was successfully registered.");
        }
    }

    /**
 * 注销 DAO 实现类
     * Unregisters DAO implementation
     *
 * @param daoClass 要注销的 DAO 实现类 / DAO implementation class to unregister
     */
    public static void unregisterDAO(Class<? extends DAO> daoClass) {
        DaoState state = state();
        synchronized (DAOManager.class) {
            for (DAO dao : state.daoMap.values()) {
                if (dao.getClass() == daoClass) {
                    state.daoMap.remove(dao.getClassName());

                    if (log.isDebugEnabled()) {
                        log.debug("DAO " + dao.getClassName() + " was successfully unregistered.");
                    }

                    break;
                }
            }
        }
    }

    /**
     * 私有构造函数，防止实例化
     * Private constructor to prevent instantiation
     */
    private DAOManager() {
        // 空构造函数 / Empty constructor
    }

    /**
     * 通过 ServiceLoader 加载当前上下文的编译期 DAO 类
     * Load compiled DAO classes for the current context via ServiceLoader
     *
     * @param context 服务上下文名称 / Service context name
     */
    private static void loadCompiledDaos(String context) {
        DAOLoader loader = new DAOLoader();
        boolean foundProvider = false;
        for (DAOClassProvider provider : ServiceLoader.load(DAOClassProvider.class)) {
            if (!context.equals(provider.contextName())) {
                continue;
            }
            foundProvider = true;
            loader.postLoad(provider.daoClasses());
        }
        if (!foundProvider) {
            throw new IllegalStateException("No DAO class provider registered for " + context + " service context");
        }
    }

    /**
     * 获取当前服务上下文的 DAO 状态
     * Get the DAO state for the current service context
     *
     * DAO state
     */
    private static DaoState state() {
        DaoState state = states.get(ServiceContext.current());
        if (state == null) {
            throw new IllegalStateException("DAOManager is not initialized for " + ServiceContext.current() + " service context");
        }
        return state;
    }

    /**
     * 单个服务上下文下的 DAO 注册表
     * DAO registry for a single service context
     */
    private static final class DaoState {
        private final Map<String, DAO> daoMap = new HashMap<String, DAO>();
    }
}
