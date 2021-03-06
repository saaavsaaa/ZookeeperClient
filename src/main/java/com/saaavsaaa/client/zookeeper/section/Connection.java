package com.saaavsaaa.client.zookeeper.section;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Created by aaa
 */
public class Connection {
    private static final Logger logger = LoggerFactory.getLogger(Connection.class);
    //is need reset
    private static final Map<Integer, Boolean> exceptionResets = new ConcurrentHashMap<>();
    private ClientContext context;
    
    static {
        exceptionResets.put(KeeperException.Code.SESSIONEXPIRED.intValue(), true);
        exceptionResets.put(KeeperException.Code.SESSIONMOVED.intValue(), true);
        exceptionResets.put(KeeperException.Code.CONNECTIONLOSS.intValue(), false);
        exceptionResets.put(KeeperException.Code.OPERATIONTIMEOUT.intValue(), false);
    }
    
    @Deprecated
    public Connection(final ClientContext context){
        this.context = context;
    }
    
    public static boolean needReset(final KeeperException e) throws KeeperException {
        int code = e.code().intValue();
        if (!exceptionResets.containsKey(code)) {
            throw e;
        }
        return exceptionResets.get(code);
    }
    
    /**
     * need retry.
     *
     * @param keeperException keeperException
     * @return need retry
     * @throws KeeperException Zookeeper Exception
     */
    public static boolean needRetry(final KeeperException keeperException) throws KeeperException {
        return exceptionResets.containsKey(keeperException.code().intValue());
    }
    
    @Deprecated
    public void check(KeeperException e) throws KeeperException {
        int code = e.code().intValue();
        if (!exceptionResets.containsKey(code)) {
            throw e;
        }
        boolean reset = exceptionResets.get(code);
        try {
            if (reset) {
                resetConnection();
            } else {
                // block
//                block();
            }
        } catch (Exception ee) {
            logger.error("check reconnect:{}", ee.getMessage(), ee);
        }
    }
    
    private void resetConnection() throws IOException, InterruptedException {
        logger.debug("resetConnection......................................................");
//        IClient client = context.getClientFactory().newClientByOriginal(true).start();
//        this.context.updateContext(((BaseClient)client).getContext());
        logger.debug("......................................................connection reset");
    }
    
    private void block() throws InterruptedException {
        logger.debug("block auto reconnection");
        final CountDownLatch autoReconnect = new CountDownLatch(1);
        ZookeeperListener listener = new ZookeeperListener() {
            @Override
            public void process(WatchedEvent event) {
                autoReconnect.countDown();
                logger.debug("block reconnected");
            }
        };
        context.getWatchers().put(listener.getKey(), listener);
        autoReconnect.await();
    }
}
