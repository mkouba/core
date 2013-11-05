/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionListener;

import org.jboss.weld.logging.ServletLogger;

/**
 * Holds the session associated with the current request.
 *
 * This utility class was added to work around an incompatibility problem with some Servlet containers (JBoss Web, Tomcat). In these containers,
 * {@link HttpServletRequest#getSession(boolean)} cannot be used within {@link HttpSessionListener#sessionCreated(HttpSession)} method invocation is the created
 * session is not made available. As a result either null is returned or a new session is created (possibly causing an endless loop).
 *
 * This utility class receives an {@link HttpSession} once it is created and holds it until the request is destroyed / session is invalidated.
 *
 * @see https://issues.jboss.org/browse/AS7-6428
 *
 * @author Jozef Hartinger
 *
 */
public class SessionHolder {

    private static final ThreadLocal<HttpSession> CURRENT_SESSION = new ThreadLocal<HttpSession>();

    private SessionHolder() {
    }

    public static void requestInitialized(HttpServletRequest request) {
        CURRENT_SESSION.set(request.getSession(false));
    }

    public static void sessionCreated(HttpSession session) {
        CURRENT_SESSION.set(session);
    }

    public static HttpSession getSessionIfExists() {
        HttpSession session = CURRENT_SESSION.get();
        if (session != null && isSessionValid(session)) {
            return session;
        }
        return null;
    }

    public static CurrentSession getSession(HttpServletRequest request, boolean create) {
        CurrentSession currentSession = new CurrentSession();
        HttpSession session = CURRENT_SESSION.get();

        if (session != null) {
            if (isSessionValid(session)) {
                // Valid session
                currentSession.set(session);
            } else {
                // Invalid session found - most probably invalidated in parallel request
                ServletLogger.LOG.invalidSessionFound(session.getId());
                currentSession.setInvalidSessionFound();
                CURRENT_SESSION.set(null);
            }
        }

        if (create && currentSession.get() == null) {
            ServletLogger.LOG.newSessionCreated(request.getSession(true).getId());
            currentSession.set(CURRENT_SESSION.get());
        }
        return currentSession;
    }

    public static void clear() {
        CURRENT_SESSION.remove();
    }

    public static class CurrentSession {

        private HttpSession value = null;

        private boolean invalidSessionFound = false;

        CurrentSession() {
        }

        public HttpSession get() {
            return value;
        }

        void set(HttpSession value) {
            this.value = value;
        }

        public boolean isInvalidSessionFound() {
            return invalidSessionFound;
        }

        void setInvalidSessionFound() {
            this.invalidSessionFound = true;
        }

    }

    private static boolean isSessionValid(HttpSession session) {
        try {
            session.getLastAccessedTime();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

}
