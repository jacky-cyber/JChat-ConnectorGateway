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
package cn.jpush.socketio.store;

import cn.jpush.socketio.store.pubsub.PubSubListener;
import cn.jpush.socketio.store.pubsub.PubSubMessage;
import cn.jpush.socketio.store.pubsub.PubSubStore;

public class MemoryPubSubStore implements PubSubStore {

    @Override
    public void publish(String name, PubSubMessage msg) {
    }

    @Override
    public <T extends PubSubMessage> void subscribe(String name, PubSubListener<T> listener, Class<T> clazz) {
    }

    @Override
    public void unsubscribe(String name) {
    }

    @Override
    public void shutdown() {
    }

}
