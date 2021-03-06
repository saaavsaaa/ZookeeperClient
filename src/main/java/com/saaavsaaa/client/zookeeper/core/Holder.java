package com.saaavsaaa.client.zookeeper.core;

import com.saaavsaaa.client.utility.StringUtil;
import com.saaavsaaa.client.zookeeper.section.ZookeeperListener;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * zookeeper connection holder
 *
 * Created by aaa
 */
public class Holder {
    private static final Logger logger = LoggerFactory.getLogger(Holder.class);
    private final CountDownLatch connectLatch = new CountDownLatch(1);
    
    protected ZooKeeper zooKeeper;
    protected final BaseContext context;
    
    private volatile AtomicBoolean connected = new AtomicBoolean();// false
    
    Holder(final BaseContext context){
        this.context = context;
    }
    
    protected void start() throws IOException, InterruptedException {
        initZookeeper();
        connectLatch.await();
    }
    
    protected void start(final int wait, final TimeUnit units) throws IOException, InterruptedException {
        initZookeeper();
        connectLatch.await(wait, units);
    }
    
    protected void initZookeeper() throws IOException {
        logger.debug("Holder servers:{},sessionTimeOut:{}", context.servers, context.sessionTimeOut);
        zooKeeper = new ZooKeeper(context.servers, context.sessionTimeOut, startWatcher());
        if (!StringUtil.isNullOrBlank(context.scheme)) {
            zooKeeper.addAuthInfo(context.scheme, context.auth);
            logger.debug("Holder scheme:{},auth:{}", context.scheme, context.auth);
        }
    }

    private Watcher startWatcher() {
        return new Watcher() {

            @Override
            public void process(final WatchedEvent event) {
                processConnection(event);
                if (!isConnected()) {
                    return;
                }
                processGlobalListener(event);
                // TODO filter event type or path
                if (event.getType() == Event.EventType.None) {
                    return;
                }
                if (Event.EventType.NodeDeleted == event.getType() || checkPath(event.getPath())) {
                    processUsualListener(event);
                }
            }
        };
    }
    
    protected void processConnection(final WatchedEvent event) {
        logger.debug("BaseClient process event:{}", event.toString());
        if (Watcher.Event.EventType.None == event.getType()) {
            if (Watcher.Event.KeeperState.SyncConnected == event.getState()) {
                connectLatch.countDown();
                connected.set(true);
                logger.debug("BaseClient startWatcher SyncConnected");
                return;
            } else if (Watcher.Event.KeeperState.Expired == event.getState()) {
                connected.set(false);
                try {
                    logger.warn("startWatcher Event.KeeperState.Expired");
                    reset();
                    // CHECKSTYLE:OFF
                } catch (Exception e) {
                    // CHECKSTYLE:ON
                    logger.error("event state Expired:{}", e.getMessage(), e);
                }
            }
        }
    }

    private void processGlobalListener(final WatchedEvent event) {
        if (null != context.getGlobalListener()) {
            context.getGlobalListener().process(event);
        }
    }

    private void processUsualListener(final WatchedEvent event) {
        if (!context.getWatchers().isEmpty()) {
            for (ZookeeperListener zookeeperListener : context.getWatchers().values()) {
                if (null == zookeeperListener.getPath() || event.getPath().startsWith(zookeeperListener.getPath())) {
                    logger.debug("listener process:{}, listener:{}", zookeeperListener.getPath(), zookeeperListener.getKey());
                    zookeeperListener.process(event);
                }
            }
        }
    }

    private boolean checkPath(final String path) {
        try {
            return null != zooKeeper.exists(path, true);
        } catch (final KeeperException | InterruptedException ignore) {
            return false;
        }
    }
    
    public void reset() throws IOException, InterruptedException {
        logger.debug("zk reset....................................");
        close();
        start();
        logger.debug("....................................zk reset");
    }
    
    public void close() {
        try {
            zooKeeper.register(new Watcher() {

                @Override
                public void process(final WatchedEvent watchedEvent) {

                }
            });
            zooKeeper.close();
            connected.set(false);
            logger.debug("zk closed");
            context.close();
        } catch (final InterruptedException ex) {
            logger.warn("Holder close:{}", ex.getMessage());
        }
    }
    
    public ZooKeeper getZooKeeper() {
        return zooKeeper;
    }
    
    public boolean isConnected() {
        return connected.get();
    }
    
    protected void setConnected(boolean connected) {
        this.connected.set(connected);
    }
}
