package com.project.Chat_Application.Observability;

import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class StompCorrelationInterceptor implements ExecutorChannelInterceptor {

    // Runs on the thread that executes the handler (the controller's thread),
    // so the MDC is visible to the controller and everything it calls.
    @Override
    public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        String raw = accessor != null ? accessor.getFirstNativeHeader(CorrelationId.HEADER) : null;
        MDC.put(CorrelationId.MDC_KEY, CorrelationId.sanitize(raw));
        return message;
    }

    @Override
    public void afterMessageHandled(Message<?> message, MessageChannel channel,
                                    MessageHandler handler, Exception ex) {
        MDC.remove(CorrelationId.MDC_KEY);
    }
}