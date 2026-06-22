package com.aionemu.commons.database.dao;

import static com.aionemu.commons.database.DatabaseFactory.getDatabaseMajorVersion;
import static com.aionemu.commons.database.DatabaseFactory.getDatabaseMinorVersion;
import static com.aionemu.commons.database.DatabaseFactory.getDatabaseName;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.configs.DatabaseConfig;
import com.aionemu.commons.scripting.classlistener.AggregatedClassListener;
import com.aionemu.commons.scripting.classlistener.OnClassLoadUnloadListener;
import com.aionemu.commons.scripting.classlistener.ScheduledTaskClassListener;
import com.aionemu.commons.scripting.scriptmanager.ScriptManager;

/**
 * DAO管理器类
 * DAO Manager Class
 *
 * 这个类负责管理所有DAO实现类的注册和获取。它维护了一个DAO实现类的注册表，
 * 并提供了注册、注销和获取DAO实现的方法。
 * This class manages the registration and retrieval of all DAO implementations.
 * It maintains a registry of DAO implementations and provides methods for
 * registering, unregistering and retrieving DAO implementations.
 *
 * @author SoulKeeper
 * @author Saelya
 */
public class DAOManager {

    /**
     * DAOManager类的日志记录器
     * Logger for DAOManager class
     */
    private static final Logger log = LoggerFactory.getLogger(DAOManager.class);

    /**
     * 已注册的DAO集合
     * Collection of registered DAOs
     */
    private static final Map<String, DAO> daoMap = new HashMap<String, DAO>();

    /**
     * 负责加载DAO实现的脚本管理器
     * Script manager responsible for loading DAO implementations
     */
    private static ScriptManager scriptManager;

    /**
     * 初始化DAOManager
     * Initializes DAOManager
     */
    public static void init() {
        try {
            scriptManager = new ScriptManager();

            // 初始化默认的类监听器 / Initialize default class listeners
            AggregatedClassListener acl = new AggregatedClassListener();
            acl.addClassListener(new OnClassLoadUnloadListener());
            acl.addClassListener(new ScheduledTaskClassListener());
            acl.addClassListener(new DAOLoader());
            scriptManager.setGlobalClassListener(acl);

            scriptManager.load(DatabaseConfig.DATABASE_SCRIPTCONTEXT_DESCRIPTOR);
        } catch (RuntimeException e) {
            throw new Error(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            throw new Error("Can't load database script context: " + DatabaseConfig.DATABASE_SCRIPTCONTEXT_DESCRIPTOR, e);
        } catch (JAXBException e) {
            throw new Error("Can't compile database handlers - check your MySQL5 implementations", e);
        } catch (Exception e) {
            throw new Error("A fatal error occurred during loading or compiling the database handlers", e);
        }

        log.info("Loaded " + daoMap.size() + " DAO implementations.");
    }

    /**
     * 关闭DAOManager
     * Shuts down DAOManager
     */
    public static void shutdown() {
        scriptManager.shutdown();
        daoMap.clear();
        scriptManager = null;
    }

    /**
     * 根据DAO类获取其实现
     * Returns DAO implementation by DAO class
     *
     * @param clazz DAO类 / DAO class
     * @param <T> DAO类型 / DAO type
     * @return DAO实现 / DAO implementation
     * @throws DAONotFoundException 如果未找到DAO实现 / If DAO implementation not found
     */
    @SuppressWarnings("unchecked")
    public static <T extends DAO> T getDAO(Class<T> clazz) throws DAONotFoundException {
        DAO result = daoMap.get(clazz.getName());

        if (result == null) {
            String s = "DAO for class " + clazz.getSimpleName() + " not implemented";
            log.error(s);
            throw new DAONotFoundException(s);
        }

        return (T) result;
    }

    /**
     * 注册DAO实现类
     * Registers DAO implementation
     *
     * @param daoClass DAO实现类 / DAO implementation class
     * @throws DAOAlreadyRegisteredException 如果DAO已注册 / If DAO is already registered
     * @throws IllegalAccessException 如果实例化DAO时出错 / If error during DAO instantiation
     * @throws InstantiationException 如果实例化DAO时出错 / If error during DAO instantiation
     */
    public static void registerDAO(Class<? extends DAO> daoClass) throws DAOAlreadyRegisteredException, IllegalAccessException, InstantiationException {
        DAO dao = daoClass.newInstance();

        if (!dao.supports(getDatabaseName(), getDatabaseMajorVersion(), getDatabaseMinorVersion())) {
            return;
        }

        synchronized (DAOManager.class) {
            DAO oldDao = daoMap.get(dao.getClassName());
            if (oldDao != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("DAO with className ").append(dao.getClassName()).append(" is used by ");
                sb.append(oldDao.getClass().getName()).append(". Can't override with ");
                sb.append(daoClass.getName()).append(".");
                String s = sb.toString();
                log.error(s);
                throw new DAOAlreadyRegisteredException(s);
            }
            daoMap.put(dao.getClassName(), dao);
        }

        if (log.isDebugEnabled()) {
            log.debug("DAO " + dao.getClassName() + " was successfully registered.");
        }
    }

    /**
     * 注销DAO实现类
     * Unregisters DAO implementation
     *
     * @param daoClass 要注销的DAO实现类 / DAO implementation class to unregister
     */
    public static void unregisterDAO(Class<? extends DAO> daoClass) {
        synchronized (DAOManager.class) {
            for (DAO dao : daoMap.values()) {
                if (dao.getClass() == daoClass) {
                    daoMap.remove(dao.getClassName());

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
}
