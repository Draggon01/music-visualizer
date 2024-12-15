package org.musicviz.musicanalyzer.Analyzer;


import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class WebSocketEventHandler extends TextWebSocketHandler {



    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket connection established: " + session.getId());
        //webSocketStreamer.activateStreaming();
        //webSocketStreamer.startStreaming(); // Start streaming data
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        System.out.println("WebSocket connection closed: " + session.getId());
        //webSocketStreamer.endStreaming();
    }
}
