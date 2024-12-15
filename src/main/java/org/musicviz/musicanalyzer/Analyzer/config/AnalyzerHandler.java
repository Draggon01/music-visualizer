package org.musicviz.musicanalyzer.Analyzer.config;

import org.musicviz.musicanalyzer.Analyzer.AnalyzerController;
import org.springframework.web.socket.*;

public class AnalyzerHandler implements WebSocketHandler {
    private final AnalyzerController controller;

    public AnalyzerHandler(AnalyzerController controller) {
        this.controller = controller;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Connected");
        controller.startStreaming(session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        System.out.println("Received: " + message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.out.println("TransportError");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        System.out.println("Disconnected");
        controller.endStreaming(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
