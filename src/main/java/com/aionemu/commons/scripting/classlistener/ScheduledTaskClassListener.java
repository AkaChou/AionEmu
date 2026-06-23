/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 * Aion-Lightning is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Aion-Lightning is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. *
 *
 * You should have received a copy of the GNU General Public License along with Aion-Lightning. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Credits goes to all Open Source Core Developer Groups listed below Please do not change here something, ragarding the developer credits, except the
 * "developed by XXXX". Even if you edit a lot of files in this source, you still have no rights to call it as "your Core". Everybody knows that this
 * Emulator Core was developed by Aion Lightning
 * 
 * @-Aion-Unique-
 * @-Aion-Lightning
 * @Aion-Engine
 * @Aion-Extreme
 * @Aion-NextGen
 * @Aion-Core Dev.
 */
package com.aionemu.commons.scripting.classlistener;

import java.lang.reflect.Modifier;
import java.util.Map;
import org.quartz.JobDetail;
import com.aionemu.commons.scripting.metadata.Scheduled;
import com.aionemu.commons.services.CronService;
import com.aionemu.commons.utils.ClassUtils;

/**
 * 定时任务类监听器，负责管理带有@Scheduled注解的类的生命周期
 * Scheduled task class listener that manages lifecycle of classes with @Scheduled annotation
 *
 * 该类实现了以下功能：
 * This class implements the following features:
 * - 检测和验证定时任务类 (Detect and validate scheduled task classes)
 * - 管理定时任务的调度 (Manage scheduling of tasks)
 * - 处理任务的加载和卸载 (Handle task loading and unloading)
 */
public class ScheduledTaskClassListener implements ClassListener {
	
    /**
     * 处理类加载后的定时任务注册
     * Process scheduled task registration after class loading
     *
     * @param classes 要处理的类数组 / Array of classes to process
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public void postLoad(Class<?>[] classes) {
		for (Class<?> clazz : classes) {
			if (isValidClass(clazz)) {
				scheduleClass((Class<? extends Runnable>) clazz);
			}
		}
	}
	
    /**
     * 处理类卸载前的定时任务注销
     * Process scheduled task deregistration before class unloading
     *
     * @param classes 要处理的类数组 / Array of classes to process
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public void preUnload(Class<?>[] classes) {
		for (Class<?> clazz : classes) {
			if (isValidClass(clazz)) {
				unScheduleClass((Class<? extends Runnable>) clazz);
			}
		}
	}
	
    /**
     * 验证类是否为有效的定时任务类
     * Validate if a class is a valid scheduled task class
     *
     * @param clazz 要验证的类 / Class to validate
     * @return 是否为有效的定时任务类 / Whether it's a valid scheduled task class
     */
    public boolean isValidClass(Class<?> clazz) {
		
		if (!ClassUtils.isSubclass(clazz, Runnable.class)) {
			return false;
		}
		
		final int modifiers = clazz.getModifiers();
		
		if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
			return false;
		}
		
		if (!Modifier.isPublic(modifiers)) {
			return false;
		}
		
		if (!clazz.isAnnotationPresent(Scheduled.class)) {
			return false;
		}
		
		Scheduled scheduled = clazz.getAnnotation(Scheduled.class);
		if (scheduled.disabled()) {
			return false;
		}
		
		if (scheduled.value().length == 0) {
			return false;
		}
		
		return true;
	}
	
    /**
     * 调度定时任务类
     * Schedule a task class
     *
     * @param clazz 要调度的类 / Class to schedule
     */
    protected void scheduleClass(Class<? extends Runnable> clazz) {
		Scheduled metadata = clazz.getAnnotation(Scheduled.class);
		
		try {
			if (metadata.instancePerCronExpression()) {
				for (String s : metadata.value()) {
					getCronService().schedule(clazz.newInstance(), s, metadata.longRunningTask());
				}
			} else {
				Runnable r = clazz.newInstance();
				for (String s : metadata.value()) {
					getCronService().schedule(r, s, metadata.longRunningTask());
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to schedule runnable " + clazz.getName(), e);
		}
	}
	
    /**
     * 取消定时任务类的调度
     * Unschedule a task class
     *
     * @param clazz 要取消调度的类 / Class to unschedule
     */
    protected void unScheduleClass(Class<? extends Runnable> clazz) {
		Map<Runnable, JobDetail> map = getCronService().getRunnables();
		for (Map.Entry<Runnable, JobDetail> entry : map.entrySet()) {
			if (entry.getKey().getClass() == clazz) {
				getCronService().cancel(entry.getValue());
			}
		}
	}
	
    /**
     * 获取CronService实例
     * Get CronService instance
     *
     * @return CronService实例 / CronService instance
     */
    protected CronService getCronService() {
		if (CronService.getInstance() == null) {
			throw new RuntimeException("CronService is not initialized");
		}
		
		return CronService.getInstance();
	}
}
