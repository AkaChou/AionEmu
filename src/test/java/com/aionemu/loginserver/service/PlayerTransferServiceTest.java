package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.loginserver.dao.PlayerTransferDAO;
import com.aionemu.loginserver.service.ptransfer.PlayerTransferTask;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class PlayerTransferServiceTest {

    @Test
    void taskStopRemovesTaskAndMarksItAsError() throws Exception {
        FakePlayerTransferDAO dao = new FakePlayerTransferDAO();
        PlayerTransferTask task = new PlayerTransferTask();
        task.id = 42;
        Map<Integer, PlayerTransferTask> tasks = new HashMap<>();
        tasks.put(task.id, task);
        PlayerTransferService service = service(dao, tasks);

        service.onTaskStop(task.id, "refused");

        assertEquals(PlayerTransferTask.STATUS_ERROR, task.status);
        assertEquals("refused", task.comment);
        assertSame(task, dao.updatedTask);
        assertEquals(Map.of(), tasks);
    }

    private static PlayerTransferService service(PlayerTransferDAO dao, Map<Integer, PlayerTransferTask> tasks) throws Exception {
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

    private static final class FakePlayerTransferDAO extends PlayerTransferDAO {

        private PlayerTransferTask updatedTask;

        @Override
        public List<PlayerTransferTask> getNew() {
            return new ArrayList<>();
        }

        @Override
        public boolean update(PlayerTransferTask task) {
            updatedTask = task;
            return true;
        }

        @Override
        public boolean supports(String databaseName, int majorVersion, int minorVersion) {
            return true;
        }
    }
}
