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
package org.jboss.weld.logging;

import static org.jboss.weld.logging.WeldLogger.WELD_PROJECT_CODE;

import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.Message.Format;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.weld.context.BusyConversationException;
import org.jboss.weld.context.NonexistentConversationException;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.jboss.weld.exceptions.IllegalStateException;

/**
 * Log messages for conversations.
 *
 * Message Ids: 000300 - 000399
 */
@MessageLogger(projectCode = WELD_PROJECT_CODE)
public interface ConversationLogger extends WeldLogger {

    ConversationLogger LOG = Logger.getMessageLogger(ConversationLogger.class, Category.CONVERSATION.getName());

    /**
     * @deprecated Not in use
     */
    @Deprecated
    @Message(id = 300, value = "No conversation id to restore")
    String noConversationToRestore();

    /**
     * @deprecated Not in use
     */
    @Deprecated
    @Message(id = 301, value = "Could not restore long-running conversation {0} because {1}", format = Format.MESSAGE_FORMAT)
    String unableToRestoreConversation(Object param1, Object param2);

    /**
     * @deprecated Not in use
     */
    @Deprecated
    @Message(id = 302, value = "Interrupted while trying to acquire lock")
    String conversationLockUnavailable();

    /**
     * @deprecated Not in use
     */
    @Deprecated
    @Message(id = 303, value = "Conversation switched from {0} to {1}", format = Format.MESSAGE_FORMAT)
    String conversationSwitched(Object param1, Object param2);

    @LogMessage(level = Level.TRACE)
    @Message(id = 304, value = "Cleaning up conversation {0}", format = Format.MESSAGE_FORMAT)
    void cleaningUpConversation(Object param1);

    /**
     * @deprecated Not in use
     */
    @Deprecated
    @Message(id = 305, value = "Scheduled {0} for termination", format = Format.MESSAGE_FORMAT)
    String conversationTerminationScheduled(Object param1);

    /**
     * @deprecated Not in use
     */
    @Deprecated
    @Message(id = 306, value = "Termination of conversation {0} cancelled", format = Format.MESSAGE_FORMAT)
    String conversationTerminationCancelled(Object param1);

    /**
     * @deprecated Not in use
     */
    @Deprecated
    @Message(id = 307, value = "Failed to cancel termination of conversation {0}", format = Format.MESSAGE_FORMAT)
    String conversationTerminationCancellationFailed(Object param1);

    /**
     * @deprecated Not in use
     */
    @Deprecated
    @Message(id = 308, value = "Destroyed long-running conversation {0} because {1}", format = Format.MESSAGE_FORMAT)
    String destroyLrcComplete(Object param1, Object param2);

    /**
     * @deprecated Not in use
     */
    @Deprecated
    @Message(id = 309, value = "{0} long-running conversations", format = Format.MESSAGE_FORMAT)
    String lrcCount(Object param1);

    /**
     * @deprecated Not in use
     */
    @Deprecated
    @Message(id = 310, value = "Destroying transient conversation {0}", format = Format.MESSAGE_FORMAT)
    String destroyTransientConversation(Object param1);

    /**
     * @deprecated Not in use
     */
    @Deprecated
    @Message(id = 311, value = "Destroying long-running conversation {0} because {1}", format = Format.MESSAGE_FORMAT)
    String destroyLrc(Object param1, Object param2);

    /**
     * @deprecated Not in use
     */
    @Deprecated
    @Message(id = 312, value = "Destroying all long-running conversations because {0}", format = Format.MESSAGE_FORMAT)
    String destroyAllLrc(Object param1);

    @LogMessage(level = Level.TRACE)
    @Message(id = 313, value = "Lock acquired on conversation {0}", format = Format.MESSAGE_FORMAT)
    void conversationLocked(Object param1);

    @LogMessage(level = Level.TRACE)
    @Message(id = 314, value = "Lock released on conversation {0}", format = Format.MESSAGE_FORMAT)
    void conversationUnlocked(Object param1);

    @LogMessage(level = Level.WARN)
    @Message(id = 315, value = "Failed to acquire conversation lock in {0} ms for {1}", format = Format.MESSAGE_FORMAT)
    void conversationUnavailable(Object param1, Object param2);

    @LogMessage(level = Level.WARN)
    @Message(id = 316, value = "Attempt to release lock on conversation {0} failed because {1}", format = Format.MESSAGE_FORMAT)
    void illegalConversationUnlockAttempt(Object param1, Object param2);

    @LogMessage(level = Level.DEBUG)
    @Message(id = 317, value = "Promoted conversation {0} to long-running", format = Format.MESSAGE_FORMAT)
    void promotedTransient(Object param1);

    @LogMessage(level = Level.DEBUG)
    @Message(id = 318, value = "Returned long-running conversation {0} to transient", format = Format.MESSAGE_FORMAT)
    void demotedLrc(Object param1);

    /**
     * @deprecated Not in use
     */
    @Deprecated
    @Message(id = 319, value = "Switched conversation from {0} to {1}", format = Format.MESSAGE_FORMAT)
    String switchedConversation(Object param1, Object param2);

    @LogMessage(level = Level.TRACE)
    @Message(id = 320, value = "Cleaning up transient conversation")
    void cleaningUpTransientConversation();

    @Message(id = 321, value = "No conversation found to restore for id {0}", format = Format.MESSAGE_FORMAT)
    NonexistentConversationException noConversationFoundToRestore(Object param1);

    @Message(id = 322, value = "Conversation lock timed out: {0}", format = Format.MESSAGE_FORMAT)
    BusyConversationException conversationLockTimedout(Object param1);

    // Moved from JsfLogger, id 500
    /**
     * @deprecated Not in use
     */
    @Deprecated
    @Message(id = 323, value = "Cleaning up the conversation after the {0} phase because {1}", format = Format.MESSAGE_FORMAT)
    String cleaningUpConversation(Object param1, Object param2);

    // Moved from JsfLogger, id 501
    /**
     * @deprecated Not in use
     */
    @Deprecated
    @Message(id = 324, value = "Skipping conversation cleanup after the {0} phase because {1}", format = Format.MESSAGE_FORMAT)
    String skippingCleaningUpConversation(Object param1, Object param2);

    // Moved from JsfLogger, id 502
    /**
     * @deprecated Not in use
     */
    @Deprecated
    @Message(id = 325, value = "Initiating conversation before the {0} phase", format = Format.MESSAGE_FORMAT)
    String initiatingConversation(Object param1);

    // Moved from JsfLogger, id 503
    @LogMessage(level = Level.TRACE)
    @Message(id = 326, value = "Found conversation id {0} in request parameter", format = Format.MESSAGE_FORMAT)
    void foundConversationFromRequest(Object param1);

    // Moved from JsfLogger, id 504
    @LogMessage(level = Level.DEBUG)
    @Message(id = 327, value = "Resuming conversation with id {0}", format = Format.MESSAGE_FORMAT)
    void resumingConversation(Object param1);

    // Previously 214
    @Message(id = 328, value = "Attempt to call begin() on a long-running conversation")
    IllegalStateException beginCalledOnLongRunningConversation();

    // Previously 215
    @Message(id = 329, value = "Attempt to call end() on a transient conversation")
    IllegalStateException endCalledOnTransientConversation();

    /**
     * @deprecated Not in use
     */
    // Previously 216
    @Deprecated
    @Message(id = 330, value = "Cannot create ConversationBeanStore with a null HTTP session")
    String nullHttpSession();

    /**
     * @deprecated Not in use
     */
    // Previously 217
    @Deprecated
    @Message(id = 331, value = "Switching modes with non-transient conversations present resets the timeouts")
    String switchingModeResetsTimeouts();

    // Previously 218
    @Message(id = 332, value = "Conversation ID {0} is already in use", format = Format.MESSAGE_FORMAT)
    IllegalArgumentException conversationIdAlreadyInUse(Object param1);

    @Message(id = 333, value = "Must call associate() before calling activate()", format = Format.MESSAGE_FORMAT)
    IllegalStateException mustCallAssociateBeforeActivate();

    @Message(id = 334, value = "Must call associate() before calling deactivate()", format = Format.MESSAGE_FORMAT)
    IllegalStateException mustCallAssociateBeforeDeactivate();

    @Message(id = 335, value = "Context is already active", format = Format.MESSAGE_FORMAT)
    IllegalStateException contextAlreadyActive();

    @Message(id = 336, value = "Context is not active", format = Format.MESSAGE_FORMAT)
    IllegalStateException contextNotActive();

    @Message(id = 337, value = "Unable to find ConversationNamingScheme in the request, this conversation wasn't transient at the start of the request", format = Format.MESSAGE_FORMAT)
    IllegalStateException conversationNamingSchemeNotFound();

    @Message(id = 338, value = "Unable to locate ConversationIdGenerator", format = Format.MESSAGE_FORMAT)
    IllegalStateException conversationIdGeneratorNotFound();

    @Message(id = 339, value = "A request must be associated with the context in order to generate a conversation id", format = Format.MESSAGE_FORMAT)
    IllegalStateException mustCallAssociateBeforeGeneratingId();

    @Message(id = 340, value = "A request must be associated with the context in order to load the known conversations", format = Format.MESSAGE_FORMAT)
    IllegalStateException mustCallAssociateBeforeLoadingKnownConversations();

    @Message(id = 341, value = "Unable to load current conversations from the associated request, something went badly wrong when associate() was called", format = Format.MESSAGE_FORMAT)
    IllegalStateException unableToLoadCurrentConversations();
}
