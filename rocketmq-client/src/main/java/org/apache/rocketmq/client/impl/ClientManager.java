package org.apache.rocketmq.client.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClientManager {

    private static final ClientManager instance = new ClientManager();

    private final Map<String, ClientInstance> clientInstanceTable;
    private final Lock lock;

    private ClientManager() {
        this.clientInstanceTable = new HashMap<String, ClientInstance>();
        this.lock = new ReentrantLock();
    }

    public static ClientManager getInstance() {
        return instance;
    }

    public ClientInstance getClientInstance(final ClientConfig clientConfig) {
        final String arn = clientConfig.getArn();
        lock.lock();
        try {
            ClientInstance clientInstance = clientInstanceTable.get(arn);
            if (null == clientInstance) {
                clientInstance = new ClientInstance(arn);
                clientInstance.start();
                clientInstanceTable.put(arn, clientInstance);
            }
            return clientInstance;
        } finally {
            lock.unlock();
        }
    }

    public void removeClientInstance(final String id) {
        lock.lock();
        try {
            clientInstanceTable.remove(id);
        } finally {
            lock.unlock();
        }
    }
}
