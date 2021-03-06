package com.saaavsaaa.client.zookeeper.core;

import com.saaavsaaa.client.action.IProvider;
import com.saaavsaaa.client.election.LeaderElection;
import com.saaavsaaa.client.utility.PathUtil;
import com.saaavsaaa.client.utility.constant.Constants;
import com.saaavsaaa.client.zookeeper.transaction.BaseTransaction;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.ACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Created by aaa
 */
public class BaseProvider implements IProvider {
    private static final Logger logger = LoggerFactory.getLogger(BaseProvider.class);
    protected final Holder holder;
    protected final boolean watched;
    protected final List<ACL> authorities;
    protected final String rootNode;
    
    public BaseProvider(final String rootNode, final Holder holder, final boolean watched, final List<ACL> authorities) {
        this.rootNode = rootNode;
        this.holder = holder;
        this.watched = watched;
        this.authorities = authorities;
    }
    
    public String getDataString(final String key) throws KeeperException, InterruptedException {
        return new String(getData(key));
    }
    
    @Override
    public byte[] getData(final String key) throws KeeperException, InterruptedException {
        return holder.getZooKeeper().getData(key, watched, null);
    }
    
    @Override
    public void getData(final String key, final AsyncCallback.DataCallback callback, final Object ctx) throws KeeperException, InterruptedException {
        holder.getZooKeeper().getData(key, watched, callback, ctx);
    }
    
    @Override
    public boolean exists(final String key) throws KeeperException, InterruptedException {
        return null != holder.getZooKeeper().exists(key, watched);
    }
    
    @Override
    public boolean exists(final String key, final Watcher watcher) throws KeeperException, InterruptedException {
        return null != holder.getZooKeeper().exists(key, watcher);
    }
    
    @Override
    public List<String> getChildren(final String key) throws KeeperException, InterruptedException {
        return holder.getZooKeeper().getChildren(key, watched);
    }
    
    @Override
    public void create(final String key, final String value, final CreateMode createMode) throws KeeperException, InterruptedException {
        if (exists(key)) {
            return;
        }
        holder.getZooKeeper().create(key, value.getBytes(Constants.UTF_8), authorities, createMode);
        logger.debug("BaseProvider createCurrentOnly:{}", key);
//        create(key, value, createMode, new AtomicInteger());
    }
    
    @Deprecated
    private void create(final String key, final String value, final CreateMode createMode, final AtomicInteger count) throws KeeperException, InterruptedException {
        try {
            holder.getZooKeeper().create(key, value.getBytes(Constants.UTF_8), authorities, createMode);
            logger.debug("BaseProvider createCurrentOnly:{}", key);
        } catch (KeeperException.NoNodeException e) {
            logger.error("BaseProvider createCurrentOnly:{}", e.getMessage(), e);
            // I don't know whether it will happen or not, if root watcher don't update rootExist timely
            if (!exists(rootNode)) {
                logger.info("BaseProvider createCurrentOnly root not exist:{}", count.get());
                Thread.sleep(50);
                if (count.incrementAndGet() < 3) {
                    this.create(key, value, createMode, count);
                }
            }
        }
    }

    @Override
    public boolean update(final String key, final String value) throws KeeperException, InterruptedException {
//        holder.getZooKeeper().setData(key, value.getBytes(Constants.UTF_8), Constants.VERSION);
        if (exists(key)) {
            holder.getZooKeeper().setData(key, value.getBytes(Constants.UTF_8), Constants.VERSION);
            return true;
        }
        return false;
    }
    
    @Override
    public void delete(final String key) throws KeeperException, InterruptedException {
        holder.getZooKeeper().delete(key, Constants.VERSION);
        logger.debug("BaseProvider deleteOnlyCurrent:{}", key);
    }
    
    @Override
    public void delete(final String key, final AsyncCallback.VoidCallback callback, final Object ctx) throws KeeperException, InterruptedException {
        holder.getZooKeeper().delete(key, Constants.VERSION, callback, ctx);
        logger.debug("BaseProvider deleteOnlyCurrent:{},ctx:{}", key, ctx);
    }
    
    
    @Override
    public String getRealPath(final String path) {
        return PathUtil.getRealPath(rootNode, path);
    }
    
    @Override
    public List<String> getNecessaryPaths(final String key) {
        List<String> nodes = PathUtil.getPathOrderNodes(rootNode, key);
        nodes.remove(rootNode);
        return nodes;
    }
    
    @Override
    public Stack<String> getDeletingPaths(String key) {
        return PathUtil.getPathReverseNodes(rootNode, key);
    }
    
    @Override
    public void executeContention(final LeaderElection election) throws KeeperException, InterruptedException {
        this.executeContention(rootNode, election);
    }
    
    public void executeContention(final String nodeBeCompete, final LeaderElection election) throws KeeperException, InterruptedException {
        election.executeContention(rootNode, this);
    }
    
    @Override
    public void resetConnection() {
        try {
            holder.reset();
        } catch (Exception ee) {
            logger.error("resetConnection Exception:{}", ee.getMessage(), ee);
        }
    }
    
    @Override
    public BaseTransaction transaction() {
        return new BaseTransaction();
    }
    
    public String getRootNode(){
        return rootNode;
    }
    
    public Holder getHolder(){
        return holder;
    }
}
