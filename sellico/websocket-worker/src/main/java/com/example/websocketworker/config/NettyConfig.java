package com.example.websocketworker.config;

import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.springframework.boot.reactor.netty.NettyReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class NettyConfig {

    @Bean
    public NettyReactiveWebServerFactory nettyFactory() {
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();

        factory.addServerCustomizers(httpServer ->
                httpServer
                        .wiretap(true)
                        .httpRequestDecoder(spec ->
                                spec.maxInitialLineLength(65536)
                                        .maxHeaderSize(65536)
                                        .maxChunkSize(65536)
                        )
        );

        return factory;
    }
}
