package com.aionemu.gameserver.utils.collections;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;


/**
 * 乐观无锁 FIFO 队列，基于 E. Ladan-Mozes 与 N. Shavit 算法。
 * Optimistic lock-free FIFO queue based on the E. Ladan-Mozes and N. Shavit algorithm.
 * <p>
 * 与 Michael-Scott 的 {@code ConcurrentLinkedQueue} 相比，入队时 CAS 失败更少。
 * Compared with Michael and Scott Nonblocking Queue in ConcurrentLinkedQueue, fewer CAS failures when enqueueing.
 *
 * @param <E> 元素类型 / Element type
 */
public class OptimisticLinkedQueue<E> extends AbstractQueue<E> implements Queue<E>, java.io.Serializable {

	/**
	 * 序列化版本。
	 * Serialization version.
	 */
	private static final long serialVersionUID = -3445502502831420722L;

	/**
	 * 双向链表节点。
	 * Doubly linked list node.
	 *
	 * @param <E> 元素类型 / Element type
	 */
	private static class Node<E> {

		/**
		 * 节点元素。
		 * Node item.
		 */
		private volatile E item;

		/**
		 * 后继（向尾）。
		 * Next toward the tail side of the logical list.
		 */
		private volatile Node<E> next;

		/**
		 * 前驱（向头）。
		 * Previous toward the head side of the logical list.
		 */
		private volatile Node<E> prev;

		/**
		 * 使用元素构造空链节点。
		 * Construct a node with the given item and null links.
		 *
		 * @param x 元素 / Item
		 */
		@SuppressWarnings("unused")
		Node(E x) {
			item = x;
			next = null;
			prev = null;
		}

		/**
		 * 使用元素与 next 构造。
		 * Construct with item and next link.
		 *
		 * @param x 元素 / Item
		 * Next node
		 */
		Node(E x, Node<E> n) {
			item = x;
			next = n;
			prev = null;
		}

		/**
		 * 返回节点元素。
		 * Return the node item.
		 *
		 * Item
		 */
		E getItem() {
			return item;
		}

		/**
		 * 设置节点元素。
		 * Set the node item.
		 *
		 * New value
		 */
		@SuppressWarnings("unused")
		void setItem(E val) {
			this.item = val;
		}

		/**
		 * 返回 next 指针。
		 * Return the next pointer.
		 *
		 * Next node
		 */
		Node<E> getNext() {
			return next;
		}

		/**
		 * 设置 next 指针。
		 * Set the next pointer.
		 *
		 * New next
		 */
		void setNext(Node<E> val) {
			next = val;
		}

		/**
		 * 返回 prev 指针。
		 * Return the previous pointer.
		 *
		 * Previous node
		 */
		Node<E> getPrev() {
			return prev;
		}

		/**
		 * 设置 prev 指针。
		 * Set the previous pointer.
		 *
		 * New previous
		 */
		void setPrev(Node<E> val) {
			prev = val;
		}
	}

	/**
	 * tail 字段原子更新器。
	 * Atomic updater for the tail field.
	 */
	@SuppressWarnings("rawtypes")
	private static final AtomicReferenceFieldUpdater<OptimisticLinkedQueue, Node> tailUpdater = AtomicReferenceFieldUpdater
			.newUpdater(OptimisticLinkedQueue.class, Node.class, "tail");

	/**
	 * head 字段原子更新器。
	 * Atomic updater for the head field.
	 */
	@SuppressWarnings("rawtypes")
	private static final AtomicReferenceFieldUpdater<OptimisticLinkedQueue, Node> headUpdater = AtomicReferenceFieldUpdater
			.newUpdater(OptimisticLinkedQueue.class, Node.class, "head");

	/**
	 * CAS 更新 tail。
	 * CAS update of the tail.
	 *
	 * Expected value
	 * New value
	 *
	 * @return 若 successful 则为 true / True if successful
	 */
	private boolean casTail(Node<E> cmp, Node<E> val) {
		return tailUpdater.compareAndSet(this, cmp, val);
	}

	/**
	 * CAS 更新 head。
	 * CAS update of the head.
	 *
	 * Expected value
	 * New value
	 *
	 * @return 若 successful 则为 true / True if successful
	 */
	private boolean casHead(Node<E> cmp, Node<E> val) {
		return headUpdater.compareAndSet(this, cmp, val);
	}

	/**
	 * 头指针，初始化为哑节点；首个实际节点在 {@code head.getPrev()}。
	 * Head pointer, initialized to a dummy node. The first actual node is at {@code head.getPrev()}.
	 */
	private transient volatile Node<E> head = new Node<E>(null, null);

	/**
	 * 尾指针，指向列表最后一个节点。
	 * Tail pointer to the last node on the list.
	 */
	private transient volatile Node<E> tail = head;

	/**
	 * 创建空的 {@code OptimisticLinkedQueue}。
	 * Create an initially empty {@code OptimisticLinkedQueue}.
	 */
	public OptimisticLinkedQueue() {
	}

	/**
	 * 近似元素计数。
	 * Approximate element count.
	 */
	AtomicInteger count = new AtomicInteger();

	/**
	 * 将指定元素入队到尾部。
	 * Enqueue the specified element at the tail of this queue.
	 *
	 * @param e 元素，不可为 null / Element, must not be null
	 * Always true
	 * When element is null
	 */
	public boolean offer(E e) {
		if (e == null)
			throw new NullPointerException();
		Node<E> n = new Node<E>(e, null);
		for (;;) {
			Node<E> t = tail;
			n.setNext(t);
			count.incrementAndGet();
			if (casTail(t, n)) {
				t.setPrev(n);
				return true;
			}
		}
	}

	/**
	 * 从队列头部出队。成功 CAS head 后会清空被摘节点的 prev/next 以便 GC。
	 * Dequeue an element from the queue. After a successful casHead, the prev and
	 * next pointers of the dequeued node are set to null to allow garbage collection.
	 *
	 * @return 队头元素，空队列则为 null / Head element, or null if empty
	 */
	public E poll() {
		for (;;) {
			Node<E> h = head;
			Node<E> t = tail;
			Node<E> first = h.getPrev();
			if (h == head) {
				if (h != t) {
					if (first == null) {
						fixList(t, h);
						continue;
					}
					E item = first.getItem();
					if (casHead(h, first)) {
						h.setNext(null);
						h.setPrev(null);
						count.decrementAndGet();
						return item;
					}
				} else
					return null;
			}
		}
	}

	/**
	 * 在需要时修复反向指针。
	 * Fix the backwards pointers when needed.
	 *
	 * @param t 尾节点 / Tail node
	 * @param h 头节点 / Head node
	 */
	private void fixList(Node<E> t, Node<E> h) {
		Node<E> curNodeNext;
		Node<E> curNode = t;
		while (h == this.head && curNode != h) {
			curNodeNext = curNode.getNext();
			curNodeNext.setPrev(curNode);
			curNode = curNode.getNext();
		}
	}

	/**
	 * 清空队列。
	 * Clear the queue.
	 */
	public void clear() {
		while (poll() != null)
			;
	}

	/**
	 * 弹出除最后一个以外的全部元素，再将最后一个重新入队。
	 * Poll all elements except the last, then re-offer the last one.
	 *
	 * @return 被移除的元素个数 / Number of removed elements
	 */
	public int leaveTail() {
		E elem = null;
		E elem1 = null;
		int removed = 0;
		while ((elem = poll()) != null) {
			elem1 = elem;
			removed++;
		}
		if (elem1 != null) {
			removed--;
			offer(elem1);
		}
		return removed;
	}

	/**
	 * 不支持窥视。
	 * Peek is not supported.
	 *
	 * Never returns
	 * Always thrown
	 */
	@Override
	public E peek() {
		throw new UnsupportedOperationException();
	}

	/**
	 * 不支持迭代。
	 * Iteration is not supported.
	 *
	 * Never returns
	 * Always thrown
	 */
	@Override
	public Iterator<E> iterator() {
		throw new UnsupportedOperationException();
	}

	/**
	 * 返回近似元素个数。
	 * Return the approximate element count.
	 *
	 * Count
	 */
	@Override
	public int size() {
		return count.get();
	}
}
