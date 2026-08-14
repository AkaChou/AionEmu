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
import lombok.extern.slf4j.Slf4j;

/**
 * 从数据库加载并注册的全局定时任务管理器。
 * Global scheduled-task manager loaded and registered from the database.
 *
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
	private ArrayList<TaskFromDB> tasksList;

	/**
	 * 任务名到处理器模板的映射。
	 * Map of task name to handler template.
	 */
	private HashMap<String, TaskFromDBHandler> handlers;

	/**
	 * 加载 DB 任务、注册内置处理器并安排调度。
	 * Load DB tasks, register built-in handlers, and schedule them.
	 */
	public TaskManagerFromDB() {
		this.handlers = new HashMap<String, TaskFromDBHandler>();

		tasksList = getDAO().getAllTasks();
		log.info(I18n.get("log.8fed1ca907ff", tasksList.size(), (tasksList.size() > 1 ? "s" : "")));

		registerHandlers();
		registerTasks();
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
			if (handlers.get(task.getName()) != null) {
				Class<? extends TaskFromDBHandler> tmpClass = handlers.get(task.getName()).getClass();
				TaskFromDBHandler currentTask = null;

				try {
					// 创建任务新实例。 / Create new instance of the task
					currentTask = tmpClass.getDeclaredConstructor().newInstance();
				} catch (ReflectiveOperationException e) {
					log.error(e.getMessage(), e);
				}

				// 设置任务信息 / Set informations for the task
				currentTask.setId(task.getId());
				currentTask.setParam(task.getParams());

				if (!currentTask.isValid()) {
					log.error(I18n.get("log.290fb6fb05a9", task.getId()));
					continue;
				}

				if (task.getType().equals("FIXED_IN_TIME")) {
					runFixedInTimeTask(currentTask, task);
				} else
					log.error(I18n.get("log.a722bb89732c", task.getType()));
			} else
				log.error(I18n.get("log.a61c4a8afc0f", task.getName()));
		}
	}

	/**
	 * 按每天固定时刻（HH:MM:SS）循环执行任务。
	 * Run a task every day at a fixed time (HH:MM:SS).
	 *
	 * @param handler 处理器实例 / Handler instance
	 * @param dbTask 数据库任务配置 / DB task configuration
	 */
	private void runFixedInTimeTask(TaskFromDBHandler handler, TaskFromDB dbTask) {
		String time[] = dbTask.getStartTime().split(":");
		int hour = Integer.parseInt(time[0]);
		int minute = Integer.parseInt(time[1]);
		int second = Integer.parseInt(time[2]);

		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, second);

		long delay = calendar.getTimeInMillis() - System.currentTimeMillis();

		if (delay < 0) {
			delay += 1 * 24 * 60 * 60 * 1000;
		}
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(handler, delay, 1 * 24 * 60 * 60 * 1000);
	}

	/**
	 * 获取 {@link TaskFromDBDAO} 快捷方法。
	 * Shortcut to obtain {@link TaskFromDBDAO}.
	 *
	 * @return DAO 实例 / the DAO instance
	 */
	private static TaskFromDBDAO getDAO() {
		return DAOManager.getDAO(TaskFromDBDAO.class);
	}

	/**
	 * 获取单例：优先 Spring 提供者，否则回退静态 holder。
	 * Get the singleton: prefer Spring provider, otherwise the static holder.
	 *
	 * @return 管理器实例 / Manager instance
	 */
	public static final TaskManagerFromDB getInstance() {
		ObjectProvider<TaskManagerFromDB> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
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

	/**
	 * 静态单例持有者。
	 * Static singleton holder.
	 */
	private static class SingletonHolder {

		/**
		 * 默认单例实例。
		 * Default singleton instance.
		 */
		protected static final TaskManagerFromDB instance = new TaskManagerFromDB();
	}
}
