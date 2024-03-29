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
package cn.jpush.socketio.store.pubsub;


public interface PubSubStore {

    // TODO refactor to enum
    String CONNECT = "connect";

    String DISCONNECT = "disconnect";

    String JOIN = "join";

    String LEAVE = "leave";

    String DISPATCH = "dispatch";


    void publish(String name, PubSubMessage msg);

    <T extends PubSubMessage> void subscribe(String name, PubSubListener<T> listener, Class<T> clazz);

    void unsubscribe(String name);

    void shutdown();

}
