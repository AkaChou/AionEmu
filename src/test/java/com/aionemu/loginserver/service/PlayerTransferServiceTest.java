package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.loginserver.persistence.dao.PlayerTransfersDao;
import com.aionemu.loginserver.service.ptransfer.PlayerTransferTask;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class PlayerTransferServiceTest {

    @Test
    void taskStopRemovesTaskAndMarksItAsError() throws Exception {
        AtomicReference<PlayerTransferTask> updated = new AtomicReference<>();
        PlayerTransferTask task = new PlayerTransferTask();
        task.id = 42;
        Map<Integer, PlayerTransferTask> tasks = new HashMap<>();
        tasks.put(task.id, task);
        PlayerTransferService service = service(fakeDao(updated), tasks);

        service.onTaskStop(task.id, "refused");

        assertEquals(PlayerTransferTask.STATUS_ERROR, task.status);
        assertEquals("refused", task.comment);
        assertSame(task, updated.get());
        assertEquals(Map.of(), tasks);
    }

    private static PlayerTransferService service(PlayerTransfersDao dao, Map<Integer, PlayerTransferTask> tasks) throws Exception {
        PlayerTransferService service = new ObjenesisStd().newInstance(PlayerTransferService.class);
        setField(service, "dao", dao);
        setField(service, "tasks", tasks);
        return service;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * 仅实现用例所需方法的动态代理 DAO 桩。
     * Dynamic-proxy DAO stub implementing only the methods this case needs.
     */
    private static PlayerTransfersDao fakeDao(AtomicReference<PlayerTransferTask> updated) {
        return (PlayerTransfersDao) Proxy.newProxyInstance(PlayerTransfersDao.class.getClassLoader(),
            new Class<?>[] { PlayerTransfersDao.class }, (proxy, method, args) -> switch (method.getName()) {
                case "getNew" -> new ArrayList<PlayerTransferTask>();
                case "update" -> {
                    updated.set((PlayerTransferTask) args[0]);
                    yield true;
                }
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }
}
