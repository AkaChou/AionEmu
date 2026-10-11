package com.aionemu.gameserver.taskmanager;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.TaskFromDBDAO;
import com.aionemu.gameserver.model.tasks.TaskFromDB;
import com.aionemu.gameserver.model.templates.tasks.TaskFromDBHandler;
import com.aionemu.gameserver.taskmanager.tasks.RestartTask;
import com.aionemu.gameserver.taskmanager.tasks.ShutdownTask;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * 从数据库加载并注册的全局定时任务管理器。
 * Global scheduled-task manager loaded and registered from the database.
 * <p>基于 L2J Emulator Global Tasks System。/ Based on L2J Emulator Global Tasks System.</p>
 *
 * @author Divinity
 * @author Layane
 */
@Slf4j
public class TaskManagerFromDB {

	/**
	 * Spring 可选实例提供者。
	 * Optional Spring instance provider.
	 */
	private static volatile ObjectProvider<TaskManagerFromDB> instanceProvider;

	/**
	 * 数据库任务列表。
	 * Tasks loaded from the database.
	 */
	private final ArrayList<TaskFromDB> tasksList;

	/**
	 * 任务名到处理器模板的映射。
	 * Map of task name to handler template.
	 */
	private final HashMap<String, TaskFromDBHandler> handlers;

	/**
	 * 使用指定的数据库任务 DAO 构造管理器。
	 * Constructs the manager with the given task-from-DB DAO.
	 *
	 * @param dao 数据库任务 DAO / Task-from-DB DAO
	 */
	public TaskManagerFromDB(@NonNull TaskFromDBDAO dao) {
		this.handlers = new HashMap<>();

		tasksList = dao.getAllTasks();
		log.info(I18n.get("log.8fed1ca907ff", tasksList.size(), (tasksList.size() > 1 ? "s" : "")));

		registerHandlers();
		registerTasks();
	}

	/**
	 * 构造管理器：DAOManager 已初始化时优先解析 Spring Bean，否则退回实现类无参构造。
	 * Constructs the manager: resolves the Spring bean when DAOManager is initialized, else falls back to the no-arg impl constructor.
	 */
	public TaskManagerFromDB() {
		this(safeTaskDao());
	}

	/**
	 * DAO 解析兜底：容器未就绪（离线/单测）时退回实现类无参构造。
	 * DAO resolution fallback: falls back to the no-arg impl constructor when the container is not ready (offline/unit tests).
	 */
	private static TaskFromDBDAO safeTaskDao() {
		if (DAOManager.isInitialized()) {
			try {
				return DAOManager.getDAO(TaskFromDBDAO.class);
			} catch (Exception e) {
				log.debug("TaskFromDBDAO bean resolution failed, falling back to the no-arg impl constructor", e);
			}
		}
		return new com.aionemu.gameserver.dao.impl.TaskFromDBDAO();
	}

	/**
	 * 注册全部内置任务处理器。
	 * Register all built-in task handlers.
	 */
	private void registerHandlers() {
		registerNewTask(new ShutdownTask());
		registerNewTask(new RestartTask());
	}

	/**
	 * 注册一个任务处理器；若同名已存在则打错误日志。
	 * Register one task handler; log an error if the name already exists.
	 *
	 * @param task 处理器模板 / Handler template
	 */
	private void registerNewTask(TaskFromDBHandler task) {
		if (handlers.get(task.getTaskName()) != null) {
			log.error(I18n.get("log.6fd925a0ac25", task.getTaskName()));
		}
		handlers.put(task.getTaskName(), task);
	}

	/**
	 * 遍历 DB 任务，实例化对应处理器并按类型调度。
	 * Walk DB tasks, instantiate matching handlers, and schedule by type.
	 */
	private void registerTasks() {
		// 用于所有来自 DB 的任务 / For all tasks from DB
		for (TaskFromDB task : tasksList) {
			// 若任务名存在 / If the task name exist
			if (handlers.get(task.name()) != null) {
				Class<? extends TaskFromDBHandler> tmpClass = handlers.get(task.name()).getClass();
				TaskFromDBHandler currentTask;

				try {
					// 创建任务新实例。 / Create new instance of the task
					currentTask = tmpClass.getDeclaredConstructor().newInstance();
				} catch (ReflectiveOperationException e) {
					log.error(e.getMessage(), e);
					continue;
				}

				// 设置任务信息 / Set informations for the task
				currentTask.setId(task.id());
				currentTask.setParam(task.params());

				if (!currentTask.isValid()) {
					log.error(I18n.get("log.290fb6fb05a9", task.id()));
					continue;
				}

				if (task.type().equals("FIXED_IN_TIME")) {
					runFixedInTimeTask(currentTask, task);
				} else
					log.error(I18n.get("log.a722bb89732c", task.type()));
			} else
				log.error(I18n.get("log.a61c4a8afc0f", task.name()));
		}
	}

	/**
	 * 按每天固定时刻（HH:MM:SS）循环执行任务。
	 * Run a task every day at a fixed time (HH:MM:SS).
	 *
	 * @param handler 处理器实例 / Handler instance
	 * @param dbTask  数据库任务配置 / DB task configuration
	 */
	private void runFixedInTimeTask(TaskFromDBHandler handler, TaskFromDB dbTask) {
		String[] time = dbTask.startTime().split(":");
		int hour = Integer.parseInt(time[0]);
		int minute = Integer.parseInt(time[1]);
		int second = Integer.parseInt(time[2]);

		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, second);

		long delay = calendar.getTimeInMillis() - System.currentTimeMillis();

		if (delay < 0) {
			delay += 24 * 60 * 60 * 1000;
		}
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(handler, delay, 24 * 60 * 60 * 1000);
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean / when no provider or bean is available
	 */
	public static TaskManagerFromDB getInstance() {
		ObjectProvider<TaskManagerFromDB> provider = instanceProvider;
		TaskManagerFromDB provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("TaskManagerFromDB 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * @param instanceProvider Spring 实例提供者 / the Spring instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<TaskManagerFromDB> instanceProvider) {
		TaskManagerFromDB.instanceProvider = instanceProvider;
	}
}
