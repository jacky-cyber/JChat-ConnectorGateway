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

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jpush.socketio.AckCallback;
import cn.jpush.socketio.HandshakeData;
import cn.jpush.socketio.SocketIOClient;
import cn.jpush.socketio.Transport;
import cn.jpush.socketio.handler.ClientHead;
import cn.jpush.socketio.namespace.Namespace;
import cn.jpush.socketio.protocol.Packet;
import cn.jpush.socketio.protocol.PacketType;

public class NamespaceClient implements SocketIOClient, Serializable {

	private static final long serialVersionUID = -4964426801030710654L;

	private final Logger log = LoggerFactory.getLogger(getClass());

    private final AtomicBoolean disconnected = new AtomicBoolean();
    private final ClientHead baseClient;
    private final Namespace namespace;

    public NamespaceClient(ClientHead baseClient, Namespace namespace) {
        this.baseClient = baseClient;
        this.namespace = namespace;
        namespace.addClient(this);
    }

    public ClientHead getBaseClient() {
        return baseClient;
    }

    @Override
    public Transport getTransport() {
        return baseClient.getCurrentTransport();
    }

    @Override
    public boolean isChannelOpen() {
        return baseClient.isChannelOpen();
    }

    @Override
    public Namespace getNamespace() {
        return namespace;
    }

    @Override
    public void sendEvent(String name, Object ... data) {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setSubType(PacketType.EVENT);
        packet.setName(name);
        packet.setData(Arrays.asList(data));
        send(packet);
    }

    @Override
    public void sendEvent(String name, AckCallback<?> ackCallback, Object ... data) {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setSubType(PacketType.EVENT);
        packet.setName(name);
        packet.setData(Arrays.asList(data));
        send(packet, ackCallback);
    }

    private boolean isConnected() {
        return !disconnected.get() && baseClient.isConnected();
    }

    @Override
    public void send(Packet packet, AckCallback<?> ackCallback) {
        if (!isConnected()) {
            ackCallback.onTimeout();
            return;
        }
        long index = baseClient.getAckManager().registerAck(getSessionId(), ackCallback);
        packet.setAckId(index);
        send(packet);
    }

    @Override
    public void send(Packet packet) {
        if (!isConnected()) {
            return;
        }
        packet.setNsp(namespace.getName());
        baseClient.send(packet);
    }

    public void onDisconnect() {
        disconnected.set(true);

        baseClient.removeNamespaceClient(this);
        namespace.onDisconnect(this);

        log.debug("Client {} for namespace {} has been disconnected", baseClient.getSessionId(), getNamespace().getName());
    }

    @Override
    public void disconnect() {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setSubType(PacketType.DISCONNECT);
        send(packet);
//        onDisconnect();
    }

    @Override
    public UUID getSessionId() {
        return baseClient.getSessionId();
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return baseClient.getRemoteAddress();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getSessionId() == null) ? 0 : getSessionId().hashCode());
        result = prime * result
                + ((getNamespace().getName() == null) ? 0 : getNamespace().getName().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NamespaceClient other = (NamespaceClient) obj;
        if (getSessionId() == null) {
            if (other.getSessionId() != null)
                return false;
        } else if (!getSessionId().equals(other.getSessionId()))
            return false;
        if (getNamespace().getName() == null) {
            if (other.getNamespace().getName() != null)
                return false;
        } else if (!getNamespace().getName().equals(other.getNamespace().getName()))
            return false;
        return true;
    }

    @Override
    public void joinRoom(String room) {
        namespace.joinRoom(room, getSessionId());
    }

    @Override
    public void leaveRoom(String room) {
        namespace.leaveRoom(room, getSessionId());
    }

    @Override
    public void set(String key, Object val) {
        baseClient.getStore().set(key, val);
    }

    @Override
    public <T> T get(String key) {
        return baseClient.getStore().get(key);
    }

    @Override
    public boolean has(String key) {
        return baseClient.getStore().has(key);
    }

    @Override
    public void del(String key) {
        baseClient.getStore().del(key);
    }

    @Override
    public Set<String> getAllRooms() {
        return namespace.getRooms(this);
    }

    @Override
    public HandshakeData getHandshakeData() {
        return baseClient.getHandshakeData();
    }

}
