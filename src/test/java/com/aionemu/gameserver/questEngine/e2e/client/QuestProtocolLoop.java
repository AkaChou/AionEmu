package com.aionemu.gameserver.questEngine.e2e.client;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT;
import com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_DIALOG;
import com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.runtime.QuestE2eRuntime;
import com.aionemu.gameserver.questEngine.runtime.QuestE2eWorldFixture;
import com.aionemu.gameserver.questEngine.runtime.QuestProductionDispatcher;
import com.aionemu.gameserver.questEngine.runtime.QuestRouteResult;
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeMetricsCollector;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 在隔离测试引擎上执行真实 CM_DIALOG_SELECT、CM_SHOW_DIALOG 和 CM_USE_ITEM，
 * 并在关闭时恢复全部静态 provider。
 * Executes real CM_DIALOG_SELECT, CM_SHOW_DIALOG, and CM_USE_ITEM packets against an isolated test engine and
 * restores every static provider on close.
 *
 * <p>该夹具持有进程级锁，生命周期内必须使用 try-with-resources；它不启动网络线程，也不访问数据库。
 * The fixture holds a process-wide lock and must be scoped with try-with-resources; it starts no network thread and
 * accesses no database.</p>
 */
public final class QuestProtocolLoop implements AutoCloseable {
	private static final ReentrantLock INSTALLATION_LOCK = new ReentrantLock();
	private final QuestE2eRuntime runtime;
	private final QuestE2eWorldFixture world;
	private final QuestEngine engine = new QuestEngine();
	private final ObjectProvider<QuestEngine> previousEngineProvider;
	private final ObjectProvider<QuestEngine> previousServiceProvider;
	private final Field serviceProviderField;
	private final Field dispatcherField;
	private boolean closed;

	/** 安装隔离 QuestEngine 和正式 dispatcher。 / Installs the isolated QuestEngine and formal dispatcher. */
	@SuppressWarnings("unchecked")
	public QuestProtocolLoop(QuestE2eRuntime runtime) throws ReflectiveOperationException {
		INSTALLATION_LOCK.lock();
		boolean installed = false;
		try {
			this.runtime = Objects.requireNonNull(runtime, "runtime");
			world = runtime.world();
			Field engineProviderField = QuestEngine.class.getDeclaredField("instanceProvider");
			engineProviderField.setAccessible(true);
			previousEngineProvider = (ObjectProvider<QuestEngine>) engineProviderField.get(null);

			serviceProviderField = GameEngineServices.class.getDeclaredField("questEngineProvider");
			serviceProviderField.setAccessible(true);
			previousServiceProvider = (ObjectProvider<QuestEngine>) serviceProviderField.get(null);

			dispatcherField = QuestEngine.class.getDeclaredField("productionDispatcher");
			dispatcherField.setAccessible(true);
			dispatcherField.set(engine, runtime.dispatcher());

			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			beanFactory.registerSingleton(QuestProtocolLoop.class.getName() + ".engine", engine);
			ObjectProvider<QuestEngine> provider = beanFactory.getBeanProvider(QuestEngine.class);
			QuestEngine.setInstanceProvider(provider);
			serviceProviderField.set(null, provider);
			installed = true;
		} finally {
			if (!installed) {
				INSTALLATION_LOCK.unlock();
			}
		}
	}

	/**
	 * 通过真实客户端包读取和运行路径执行一个请求。
	 * Executes one request through real client-packet decoding and run paths.
	 *
	 * @param request 无头客户端请求 / headless-client request
	 * @return 状态、失败和有序出站包观察 / state, failure, and ordered outbound-packet observations
	 */
	public QuestHeadlessClient.DispatchOutcome dispatch(ClientActionRequest request) {
		ensureOpen();
		Objects.requireNonNull(request, "request");
		if (request.questId() != runtime.state().questId()) {
			throw new IllegalArgumentException("request quest does not match protocol runtime");
		}
		if (request.kind() == ClientActionRequest.Kind.WORLD_EVENT) {
			return runtime.dispatch(request);
		}
		runtime.beginRequest(request);
		QuestStatusSnapshot before = snapshot();
		QuestRuntimeMetricsCollector.Snapshot metricsBefore = runtime.metricsSnapshot();
		int traceStart = runtime.trace().entries().size();
		RuntimeException failure = null;
		boolean handled = false;
		try {
			handled = switch (request.kind()) {
				case DIALOG_SELECT -> dialog(request);
				case USE_OBJECT -> useObject(request);
				case USE_ITEM -> useItem(request);
				case WORLD_EVENT -> throw new IllegalStateException("world event was not delegated");
			};
		} catch (RuntimeException exception) {
			failure = exception;
		}
		List<ServerPacketObservation> packets = world.drainPackets();
		QuestStatusSnapshot after = snapshot();
		boolean changed = !before.equals(after);
		QuestRouteResult routeResult = runtime.conclusiveResultSince(metricsBefore);
		handled |= routeResult == QuestRouteResult.HANDLED || routeResult == QuestRouteResult.BLOCKED;
		if (failure == null && rolledBackAfter(traceStart)) {
			failure = new IllegalStateException("protocol quest transaction rolled back");
		}
		if (request.kind() == ClientActionRequest.Kind.USE_ITEM && !handled) {
			handled = changed || !packets.isEmpty();
		}
		return new QuestHeadlessClient.DispatchOutcome(handled, failure != null, changed, failure, packets);
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		try {
			dispatcherField.set(engine, QuestProductionDispatcher.disabled());
			serviceProviderField.set(null, previousServiceProvider);
			QuestEngine.setInstanceProvider(previousEngineProvider);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("cannot restore quest protocol providers", exception);
		} finally {
			INSTALLATION_LOCK.unlock();
		}
	}

	private boolean dialog(ClientActionRequest request) {
		ByteBuffer buffer = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN)
			.putInt(request.objectId())
			.putShort((short) request.actionId())
			.putShort((short) 0)
			.putShort((short) 0)
			.putShort((short) runtime.state().currentPage())
			.putInt(request.questId())
			.putShort((short) 0);
		buffer.flip();
		runPacket(new CM_DIALOG_SELECT(0, AionConnection.State.IN_GAME), buffer);
		return world.protocolDialogHandled(request.objectId());
	}

	private boolean useItem(ClientActionRequest request) {
		world.seedQuestItem(request.itemId(), request.itemObjectId());
		ByteBuffer buffer = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
			.putInt(request.itemObjectId()).put((byte) 1);
		buffer.flip();
		runPacket(new CM_USE_ITEM(0, AionConnection.State.IN_GAME), buffer);
		return false;
	}

	private boolean useObject(ClientActionRequest request) {
		ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
			.putInt(request.objectId());
		buffer.flip();
		runPacket(new CM_SHOW_DIALOG(0, AionConnection.State.IN_GAME), buffer);
		return world.protocolDialogHandled(request.objectId());
	}

	private void runPacket(AionClientPacket packet, ByteBuffer buffer) {
		packet.setConnection(world.connection());
		packet.setBuffer(buffer);
		if (!packet.read()) {
			throw new IllegalStateException("client packet could not be decoded");
		}
		packet.run();
	}

	private boolean rolledBackAfter(int traceStart) {
		return runtime.trace().entries().stream().skip(traceStart)
			.anyMatch(entry -> "TRANSACTION".equals(entry.phase()) && "rollback".equals(entry.detail()));
	}

	private QuestStatusSnapshot snapshot() {
		return new QuestStatusSnapshot(runtime.state().status(), runtime.state().packedVariables(),
			runtime.inventorySnapshot());
	}

	private void ensureOpen() {
		if (closed) {
			throw new IllegalStateException("quest protocol loop is closed");
		}
	}

	/** 协议执行前后的最小任务状态签名。 / Minimal quest-state signature before and after protocol execution. */
	private record QuestStatusSnapshot(com.aionemu.gameserver.questEngine.model.QuestStatus status,
			int packedVariables, java.util.Map<Integer, Integer> inventory) {
	}
}
