package com.saaavsaaa.client.zookeeper;

import com.saaavsaaa.client.untils.*;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.apache.zookeeper.Watcher.Event.EventType.NodeDeleted;

/**
 * Created by aaa on 18-4-19.
 */
public abstract class Client implements IClient{
    private static final CountDownLatch CONNECTED = new CountDownLatch(1);
    protected static final Map<String, Watcher> watchers = new ConcurrentHashMap<>();
    private boolean globalListenerRegistered = false;
    
    private final String servers;
    private final int sessionTimeOut;
    protected ZooKeeper zooKeeper;
    
    protected String rootNode = "/InitValue";
    protected boolean rootExist = false;
    protected List<ACL> authorities;
    
    protected Client(String servers, int sessionTimeoutMilliseconds) {
        this.servers = servers;
        this.sessionTimeOut = sessionTimeoutMilliseconds;
    }
    
    public void start() throws IOException, InterruptedException {
        zooKeeper = new ZooKeeper(servers, sessionTimeOut, startWatcher());
        CONNECTED.await();
    }
    
    ZooKeeper getZooKeeper(){
        return zooKeeper;
    }
    
    private Watcher startWatcher() {
        return new Watcher(){
            public void process(WatchedEvent event) {
                if(Event.KeeperState.SyncConnected == event.getState()){
                    if(Event.EventType.None == event.getType()){
                        CONNECTED.countDown();
                    }
                }
                if (globalListenerRegistered){
                    watchers.get(Constants.GLOBAL_LISTENER_KEY).process(event);
                }
                if (Properties.WATCH_ON && watchers.containsKey(event.getPath())){
                     watchers.get(event.getPath()).process(event);
                }
            }
        };
    }
    
    void registerWatch(final Listener globalListener){
        if (globalListenerRegistered){
            return;
        }
        globalListenerRegistered = true;
        watchers.put(Constants.GLOBAL_LISTENER_KEY, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                globalListener.process(event);
            }
        });
    }
    
    public Watcher registerWatch(final String key, final Listener listener){
        String path = PathUtil.getRealPath(rootNode, key);
//        listener.setKey(path);
        Watcher watcher = new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                listener.process(event);
            }
        };
        watchers.put(path, watcher);
        return watcher;
    }
    
    public void unregisterWatch(final String key){
        if (StringUtil.isNullOrBlank(key)){
            throw new IllegalArgumentException("key should not be blank");
        }
        String path = PathUtil.getRealPath(rootNode, key);
        if (watchers.containsKey(path)){
            watchers.remove(path);
        }
    }
    
    public void close() throws InterruptedException {
        zooKeeper.close();
    }
    
    void createNamespace() throws KeeperException, InterruptedException {
        if (rootExist){
            return;
        }
        zooKeeper.create(rootNode, Constants.NOTHING_DATA, authorities, CreateMode.PERSISTENT);
        rootExist = true;
        zooKeeper.exists(rootNode, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (rootNode.equals(event.getPath()) && NodeDeleted.equals(event.getType())){
                    rootExist = false;
                    System.out.println("----------------------------------------------delete root");
                    System.out.println(event.getPath());
                    System.out.println(event.getState());
                    System.out.println(event.getType());
                    System.out.println("----------------------------------------------delete root");
                }
            }
        });
        System.out.println("----------------------------------------------create root");
    }
    
    void deleteNamespace() throws KeeperException, InterruptedException {
        zooKeeper.delete(rootNode, Constants.VERSION);
    }
    
    void setRootNode(String rootNode) {
        this.rootNode = rootNode;
    }
    
    void setAuthorities(String scheme, byte[] auth) {
        zooKeeper.addAuthInfo(scheme , auth);
        this.authorities = ZooDefs.Ids.CREATOR_ALL_ACL;
    }
}