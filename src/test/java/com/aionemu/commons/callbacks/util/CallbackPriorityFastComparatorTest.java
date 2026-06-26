package com.aionemu.commons.callbacks.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackPriority;
import com.aionemu.commons.callbacks.CallbackResult;
import org.junit.jupiter.api.Test;

class CallbackPriorityFastComparatorTest {

    @Test
    void comparesCallbacksByPriority() {
        CallbackPriorityFastComparator comparator = new CallbackPriorityFastComparator();
        TestCallback highPriority = new TestCallback(10);
        TestCallback lowPriority = new TestCallback(1);

        assertTrue(comparator.compare(highPriority, lowPriority) < 0);
        assertTrue(comparator.compare(lowPriority, highPriority) > 0);
    }

    @Test
    void treatsCallbacksWithEqualPriorityAsEqual() {
        CallbackPriorityFastComparator comparator = new CallbackPriorityFastComparator();

        assertTrue(comparator.areEqual(new TestCallback(5), new TestCallback(5)));
    }

    @Test
    void returnsCallbackHashCode() {
        CallbackPriorityFastComparator comparator = new CallbackPriorityFastComparator();
        TestCallback callback = new TestCallback(3);

        assertEquals(callback.hashCode(), comparator.hashCodeOf(callback));
    }

    private record TestCallback(int priority) implements Callback<Object>, CallbackPriority {

        @Override
        public CallbackResult beforeCall(Object object, Object[] args) {
            return CallbackResult.newContinue();
        }

        @Override
        public CallbackResult afterCall(Object object, Object[] args, Object result) {
            return CallbackResult.newContinue();
        }

        @Override
        public Class<? extends Callback> getBaseClass() {
            return TestCallback.class;
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }
}
