/**
 * Copyright 2012 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.jpush.socketio.transport;

import io.netty.buffer.ByteBufHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.ReferenceCountUtil;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jpush.socketio.Configuration;
import cn.jpush.socketio.Transport;
import cn.jpush.socketio.handler.AuthorizeHandler;
import cn.jpush.socketio.handler.ClientHead;
import cn.jpush.socketio.handler.ClientsBox;
import cn.jpush.socketio.messages.PacketsMessage;
import cn.jpush.socketio.scheduler.CancelableScheduler;
import cn.jpush.socketio.scheduler.SchedulerKey;

@Sharable
public class WebSocketTransport extends ChannelInboundHandlerAdapter {

    public static final String NAME = "websocket";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AuthorizeHandler authorizeHandler;
    private final CancelableScheduler scheduler;
    private final Configuration configuration;
    private final ClientsBox clientsBox;

    private final boolean isSsl;

    public WebSocketTransport(boolean isSsl,
            AuthorizeHandler authorizeHandler, Configuration configuration,
            CancelableScheduler scheduler, ClientsBox clientsBox) {
        this.isSsl = isSsl;
        this.authorizeHandler = authorizeHandler;
        this.configuration = configuration;
        this.scheduler = scheduler;
        this.clientsBox = clientsBox;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof CloseWebSocketFrame) {
            ctx.channel().close();
            ReferenceCountUtil.release(msg);
        } else if (msg instanceof BinaryWebSocketFrame
                    || msg instanceof TextWebSocketFrame) {
            ByteBufHolder frame = (ByteBufHolder) msg;
            ClientHead client = clientsBox.get(ctx.channel());
            if (client == null) {
                log.debug("Client with was already disconnected. Channel closed!");
                ctx.channel().close();
                frame.release();
                return;
            }

            ctx.pipeline().fireChannelRead(new PacketsMessage(client, frame.content(), Transport.WEBSOCKET));
            frame.release();
        } else if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.getUri());
            String path = queryDecoder.path();
            List<String> transport = queryDecoder.parameters().get("transport");
            List<String> sid = queryDecoder.parameters().get("sid");

            if (transport != null && NAME.equals(transport.get(0))) {
                try {
                    if (!configuration.getTransports().contains(Transport.WEBSOCKET)) {
                        log.debug("{} transport not supported by configuration.", Transport.WEBSOCKET);
                        ctx.channel().close();
                        return;
                    }
                    if (sid != null && sid.get(0) != null) {
                        final UUID sessionId = UUID.fromString(sid.get(0));
                        handshake(ctx, sessionId, path, req);
                    } else {
                        ClientHead client = ctx.channel().attr(ClientHead.CLIENT).get();
                        // first connection
                        handshake(ctx, client.getSessionId(), path, req);
                    }
                } finally {
                    req.release();
                }
            } else {
                ctx.fireChannelRead(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ClientHead client = clientsBox.get(ctx.channel());
        if (client != null && client.isTransportChannel(ctx.channel(), Transport.WEBSOCKET)) {
            ctx.flush();
        } else {
            super.channelReadComplete(ctx);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ClientHead client = clientsBox.get(ctx.channel());
        if (client != null && client.isTransportChannel(ctx.channel(), Transport.WEBSOCKET)) {
            log.debug("channel inactive {}", client.getSessionId());
            client.onChannelDisconnect();
        }
        super.channelInactive(ctx);
    }

    private void handshake(ChannelHandlerContext ctx, final UUID sessionId, String path, FullHttpRequest req) {
        final Channel channel = ctx.channel();

        WebSocketServerHandshakerFactory factory =
                new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, false, configuration.getMaxFramePayloadLength());
        WebSocketServerHandshaker handshaker = factory.newHandshaker(req);
        if (handshaker != null) {
            ChannelFuture f = handshaker.handshake(channel, req);
            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!future.isSuccess()) {
                        log.error("Can't handshake " + sessionId, future.cause());
                        return;
                    }
                    connectClient(channel, sessionId);
                }
            });
        } else {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }
    }

    private void connectClient(final Channel channel, final UUID sessionId) {
        ClientHead client = clientsBox.get(sessionId);
        if (client == null) {
            log.warn("Unauthorized client with sessionId: {} with ip: {}. Channel closed!",
                        sessionId, channel.remoteAddress());
            channel.close();
            return;
        }

        client.bindChannel(channel, Transport.WEBSOCKET);

        authorizeHandler.connect(client);

        if (client.getCurrentTransport() == Transport.POLLING) {
            SchedulerKey key = new SchedulerKey(SchedulerKey.Type.UPGRADE_TIMEOUT, sessionId);
            scheduler.schedule(key, new Runnable() {
                @Override
                public void run() {
                    ClientHead clientHead = clientsBox.get(sessionId);
                    if (clientHead != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("client did not complete upgrade - closing transport");
                        }
                        clientHead.onChannelDisconnect();
                    }
                }
            }, configuration.getUpgradeTimeout(), TimeUnit.MILLISECONDS);
        }

        log.debug("сlient {} handshake completed", sessionId);
    }

    private String getWebSocketLocation(HttpRequest req) {
        String protocol = "ws://";
        if (isSsl) {
            protocol = "wss://";
        }
        return protocol + req.headers().get(HttpHeaders.Names.HOST) + req.getUri();
    }

}
