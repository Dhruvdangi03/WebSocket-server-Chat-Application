package com.websocket_chat.client;

import com.websocket_chat.Message;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.web.socket.CloseStatus;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class MyStompSessionHandler extends StompSessionHandlerAdapter {
    private String username;
    private MessageListener messageListener;

    public MyStompSessionHandler(MessageListener messageListener, String username) {
        this.username = username;
        this.messageListener = messageListener;
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        System.out.println("Connected to STOMP server as " + username);

        session.subscribe("/topic/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Message.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                try {
                    if (payload instanceof Message) {
                        Message message = (Message) payload;
                        messageListener.onMessageRecieve(message);
                        System.out.println("Received Message: " + message.getUser() + ": " + message.getMessage());
                    } else {
                        System.out.println("Received unexpected payload type: " + payload.getClass());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        System.out.println("Subscribed to /topic/messages");

        session.subscribe("/topic/users", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return new ArrayList<String>().getClass();
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                try{
                    if(payload instanceof ArrayList){
                        ArrayList<String> activeUsers = (ArrayList<String>) payload;
                        messageListener.onActiveUsersUpdated(activeUsers);
                        System.out.println("Received active Users: " + activeUsers);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        System.out.println("Subscribed to /topic/users");

        session.send("/app/connect", username);
        session.send("/app/request-users", "");
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        System.err.println("Transport error: " + exception.getMessage());
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        System.err.println("Unhandled frame: " + payload);
    }

    public void afterConnectionClosed(StompSession session, CloseStatus closeStatus) {
        System.out.println("Connection closed: " + closeStatus);
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        System.err.println("STOMP error: " + exception.getMessage());
    }
}
