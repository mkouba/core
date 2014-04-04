/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld;

/**
 * Application container instance state.
 *
 * Do not change the ordering of constants without reimplementing {@link #comesBefore(ContainerState)} and {@link #comesAfter(ContainerState)}.
 *
 * @author pmuir
 */
public enum ContainerState {
    /**
     * The container has not been started
     * TODO rename to BEFORE_USE
     */
    STOPPED(false),
    /**
     * The container is starting
     */
    STARTING(false),
    /**
     * The bean discovery has finished
     */
    BEAN_DISCOVERY_FINISHED(false),
    /**
     * The container has started and beans have been deployed
     */
    DEPLOYED(true),
    /**
     * The deployment has been validated
     */
    VALIDATED(true),
    /**
     * The container finished initialization and is serving requests
     */
    INITIALIZED(true),
    /**
     * The container has been shutdown
     */
    SHUTDOWN(false);

    private ContainerState(boolean available) {
        this.available = available;
    }

    final boolean available;

    /**
     * @return ture if the container is available for use, false otherwise
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * @param state
     * @return true if the given state comes before this state, false otherwise
     */
    public boolean comesAfter(ContainerState state) {
        return this.ordinal() > state.ordinal();
    }

    /**
     * @param state
     * @return true if the given state comes after this state, false otherwise
     */
    public boolean comesBefore(ContainerState state) {
        return this.ordinal() < state.ordinal();
    }
}
