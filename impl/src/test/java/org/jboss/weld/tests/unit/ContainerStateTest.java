package org.jboss.weld.tests.unit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.jboss.weld.ContainerState.*;

import org.jboss.weld.ContainerState;
import org.junit.Test;

/**
 *
 * @author Martin Kouba
 */
public class ContainerStateTest {

    @Test
    public void testOrdering() {
        assertBefore(STOPPED, STARTING, BEAN_DISCOVERY_FINISHED, DEPLOYED, VALIDATED, INITIALIZED, SHUTDOWN);
        assertBefore(STARTING, BEAN_DISCOVERY_FINISHED, DEPLOYED, VALIDATED, INITIALIZED, SHUTDOWN);
        assertAfter(STARTING, STOPPED);
        assertBefore(BEAN_DISCOVERY_FINISHED, DEPLOYED, VALIDATED, INITIALIZED, SHUTDOWN);
        assertAfter(BEAN_DISCOVERY_FINISHED, STARTING, STOPPED);
        assertBefore(DEPLOYED, VALIDATED, INITIALIZED, SHUTDOWN);
        assertAfter(DEPLOYED, BEAN_DISCOVERY_FINISHED, STARTING, STOPPED);
        assertBefore(VALIDATED, INITIALIZED, SHUTDOWN);
        assertAfter(VALIDATED, DEPLOYED, BEAN_DISCOVERY_FINISHED, STARTING, STOPPED);
        assertBefore(INITIALIZED, SHUTDOWN);
        assertAfter(INITIALIZED, VALIDATED, DEPLOYED, BEAN_DISCOVERY_FINISHED, STARTING, STOPPED);
        assertAfter(SHUTDOWN, INITIALIZED, VALIDATED, DEPLOYED, BEAN_DISCOVERY_FINISHED, STARTING, STOPPED);
    }

    private void assertBefore(ContainerState state, ContainerState... afterStates) {
        for (ContainerState containerState : afterStates) {
            assertTrue(state.comesBefore(containerState));
        }
    }

    private void assertAfter(ContainerState state, ContainerState... beforeStates) {
        for (ContainerState containerState : beforeStates) {
            assertTrue(state.comesAfter(containerState));
        }
    }

}
