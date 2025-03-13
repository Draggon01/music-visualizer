package org.musicviz.musicanalyzer.Analyzer.config;

import org.musicviz.musicanalyzer.Analyzer.AnalyzerController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AnalyzerController controller;

    public WebSocketConfig(AnalyzerController controller) {
        this.controller = controller;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler(), "/ws")
                .setAllowedOrigins("http://localhost:5173");
//                .withSockJS();
    }

    @Bean
    WebSocketHandler webSocketHandler() {
        return new AnalyzerHandler(controller);
    }
}
