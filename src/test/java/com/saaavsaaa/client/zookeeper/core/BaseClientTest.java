package com.saaavsaaa.client.zookeeper.core;

import com.saaavsaaa.client.TestServer;
import com.saaavsaaa.client.action.IClient;
import com.saaavsaaa.client.utility.PathUtil;
import com.saaavsaaa.client.utility.constant.Constants;
import com.saaavsaaa.client.zookeeper.ClientFactory;
import com.saaavsaaa.client.zookeeper.TestSupport;
import com.saaavsaaa.client.zookeeper.section.ZookeeperListener;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by aaa
 */
public abstract class BaseClientTest extends BaseTest {
    protected IClient testClient = null;
    protected ZooKeeper zooKeeper;
    
    @Before
    public void start() throws IOException, InterruptedException {
        TestServer.start();
        ClientFactory creator = new ClientFactory();
        testClient = createClient(creator);
        getZooKeeper(testClient);
    }

    protected ZooKeeper getZooKeeper(IClient client){
        zooKeeper = ((BaseClient)client).holder.getZooKeeper();
        return zooKeeper;
    }
    
    protected abstract IClient createClient(ClientFactory creator) throws IOException, InterruptedException;
    
    @After
    public void stop() throws InterruptedException {
        testClient.close();
        testClient = null;
    }
    
    @Test
    public void deleteRoot() throws KeeperException, InterruptedException {
        ((BaseClient)testClient).createNamespace();
        deleteRoot(testClient);
        assertNull(getZooKeeper(testClient).exists(Constants.PATH_SEPARATOR + TestSupport.ROOT, false));
    }
    
    protected void createRoot(IClient client) throws KeeperException, InterruptedException {
        ((BaseClient) client).createNamespace();
        assertNotNull(getZooKeeper(client).exists(Constants.PATH_SEPARATOR + TestSupport.ROOT, false));
        ((BaseClient) client).deleteNamespace();
        assertNull(getZooKeeper(client).exists(Constants.PATH_SEPARATOR + TestSupport.ROOT, false));
    }
    
    protected void createChild(IClient client) throws KeeperException, InterruptedException {
        String key = "a/b/bb";
        client.createAllNeedPath(key, "bbb11", CreateMode.PERSISTENT);
        assertNotNull(getZooKeeper(client).exists(PathUtil.getRealPath(TestSupport.ROOT, key), false));
        client.deleteCurrentBranch(key);
        assertNull(getZooKeeper(client).exists(PathUtil.getRealPath(TestSupport.ROOT, key), false));
    }
    
    protected void deleteBranch(IClient client) throws KeeperException, InterruptedException {
        String keyB = "a/b/bb";
        String valueB = "bbb11";
        client.createAllNeedPath(keyB, valueB, CreateMode.PERSISTENT);
        assertNotNull(getZooKeeper(client).exists(PathUtil.getRealPath(TestSupport.ROOT, keyB), false));
        String keyC = "a/c/cc";
        client.createAllNeedPath(keyC, "ccc11", CreateMode.PERSISTENT);
        assertNotNull(getZooKeeper(client).exists(PathUtil.getRealPath(TestSupport.ROOT, keyC), false));
        client.deleteCurrentBranch(keyC);
        assertNull(getZooKeeper(client).exists(PathUtil.getRealPath(TestSupport.ROOT, keyC), false));
        assertNotNull(getZooKeeper(client).exists(PathUtil.getRealPath(TestSupport.ROOT, "a"), false));
        client.deleteCurrentBranch(keyB);
        checkChangeKey(client, keyB);
        client.createAllNeedPath(keyB, valueB, CreateMode.PERSISTENT);
        assertNotNull(getZooKeeper(client).exists(PathUtil.getRealPath(TestSupport.ROOT, keyB), false));
        assertThat(client.getDataString(keyB), is(valueB));
        client.deleteCurrentBranch(keyB);
        checkChangeKey(client, PathUtil.checkPath(TestSupport.ROOT));
    }

    protected void checkChangeKey(final IClient client, final String key) throws KeeperException, InterruptedException {
        if (getZooKeeper(client).exists(PathUtil.getRealPath(TestSupport.ROOT, key), false) != null) {
            List<String> children = client.getChildren(TestSupport.ROOT);
            // LeaderElection.executeContention delete CHANGING_KEY when action done
            if (children != null && !children.isEmpty()) {
                assertThat(children, hasItems(Constants.CHANGING_KEY));
            }
        }
    }
    
    protected void isExisted(IClient client) throws KeeperException, InterruptedException {
        String key = "a/b/bb";
        client.createAllNeedPath(key, "", CreateMode.PERSISTENT);
        assertTrue(isExisted(key, client));
        client.deleteCurrentBranch(key);
    }
    
    protected void get(IClient client) throws KeeperException, InterruptedException {
        String value = "bbb11";
        client.createAllNeedPath("a/b", value, CreateMode.PERSISTENT);
        String key = "a";
        assertThat(getDirectly(key, client), is(""));
        key = "a/b";
        assertThat(getDirectly(key, client), is(value));
        client.deleteCurrentBranch("a/b");
    }
    
    protected void asyncGet(IClient client) throws KeeperException, InterruptedException {
        final CountDownLatch ready = new CountDownLatch(1);
        String key = "a/b";
        String value = "bbb11";
        client.createAllNeedPath(key, value, CreateMode.PERSISTENT);
        AsyncCallback.DataCallback callback = new AsyncCallback.DataCallback() {
            @Override
            public void processResult(final int rc, final String path, final Object ctx, final byte[] data, final Stat stat) {
                assertThat(new String(data), is(ctx));
                ready.countDown();
            }
        };
        client.getData(key, callback, value);
        ready.await();
        client.deleteCurrentBranch("a/b");
    }

    protected String getDirectly(String key, IClient client) throws KeeperException, InterruptedException {
        return new String(client.getData(key));
    }

    protected boolean isExisted(String key, IClient client) throws KeeperException, InterruptedException {
        return client.checkExists(key);
    }
    
    protected void getChildrenKeys(IClient client) throws KeeperException, InterruptedException {
        String key = "a/b";
        String current = "a";
        client.createAllNeedPath(key, "", CreateMode.PERSISTENT);
        List<String> result = client.getChildren(current);
        Collections.sort(result, new Comparator<String>() {

            public int compare(final String o1, final String o2) {
                return o2.compareTo(o1);
            }
        });
        assertThat(result.get(0), is("b"));
        client.deleteCurrentBranch(key);
    }
    
    protected void persist(IClient client) throws KeeperException, InterruptedException {
        String key = "a";
        String value = "aa";
        String newValue = "aaa";
        if (!isExisted(key, client)) {
            client.createAllNeedPath(key, value, CreateMode.PERSISTENT);
        } else {
            updateWithCheck(key, value, client);
        }

        assertThat(getDirectly(key, client), is(value));

        updateWithCheck(key, newValue, client);
        assertThat(getDirectly(key, client), is(newValue));
        client.deleteCurrentBranch(key);
    }

    protected void updateWithCheck(String key, String value, IClient client) throws KeeperException, InterruptedException {
        client.update(key, value);
//        client.transaction().check(key, Constants.VERSION).setData(key, value.getBytes(Constants.UTF_8), Constants.VERSION).commit();
    }
    
    protected void persistEphemeral(IClient client) throws KeeperException, InterruptedException {
        String key = "a/b/bb";
        String value = "b1b";
        client.createAllNeedPath(key, value, CreateMode.PERSISTENT);
        Stat stat = new Stat();
        getZooKeeper(client).getData(PathUtil.getRealPath(TestSupport.ROOT, key), false, stat);
        assertThat(stat.getEphemeralOwner(), is(0L));

        client.deleteAllChildren(key);
        assertFalse(isExisted(key, client));
        client.createAllNeedPath(key, value, CreateMode.EPHEMERAL);

        assertThat(getZooKeeper(client).exists(PathUtil.getRealPath(TestSupport.ROOT, key), null).getEphemeralOwner(), is(getZooKeeper(client).getSessionId()));
        client.deleteCurrentBranch(key);
    }
    
    protected void delAllChildren(IClient client) throws KeeperException, InterruptedException {
        String key = "a/b/bb";
        client.createAllNeedPath(key, "bb", CreateMode.PERSISTENT);
        key = "a/c/cc";
        client.createAllNeedPath(key, "cc", CreateMode.PERSISTENT);
        System.out.println(getZooKeeper(client).exists(PathUtil.getRealPath(TestSupport.ROOT, "a"), null).getNumChildren()); // nearest children count
        assertNotNull(getZooKeeper(client).exists(PathUtil.getRealPath(TestSupport.ROOT, key), false));
        client.deleteAllChildren("a");
        assertNull(getZooKeeper(client).exists(PathUtil.getRealPath(TestSupport.ROOT, key), false));
        assertNotNull(getZooKeeper(client).exists("/" + TestSupport.ROOT, false));
        ((BaseClient) client).deleteNamespace();
    }
    
    protected void watch(IClient client) throws KeeperException, InterruptedException {
        List<String> actual = new ArrayList<>();
        final ZookeeperListener zookeeperListener = buildListener(client, actual);

        String key = "a";
        client.registerWatch(key, zookeeperListener);
        client.createCurrentOnly(key, "aaa", CreateMode.EPHEMERAL);
        client.checkExists(key, new Watcher() {

            @Override
            public void process(final WatchedEvent event) {
                zookeeperListener.process(event);
            }
        });
        String value = "value0";
        client.update(key, value);
        assertThat(client.getDataString(key), is(value));
        sleep(200);

        String value1 = "value1";
        client.update(key, value1);
        assertThat(client.getDataString(key), is(value1));
        sleep(200);

        String value2 = "value2";
        client.update(key, value2);
        assertThat(client.getDataString(key), is(value2));
        sleep(200);

        client.deleteCurrentBranch(key);
        sleep(200);

        //The acquisition value is after the reception of the event,
        //so the value may be not equal.
        assertThat(actual, hasItems("update_/test/a_value0", "update_/test/a_value1", "update_/test/a_value2", "delete_/test/a_"));
        client.unregisterWatch(zookeeperListener.getKey());
    }

    protected final void watchRegister(final IClient client) throws KeeperException, InterruptedException {
        List<String> actual = new ArrayList<>();

        final ZookeeperListener zookeeperListener = buildListener(client, actual);

        String key = "a";
        client.registerWatch(key, zookeeperListener);
        client.createCurrentOnly(key, "aaa", CreateMode.EPHEMERAL);
        sleep(100);

        String value = "value0";
        client.update(key, value);
        sleep(100);

        String value1 = "value1";
        client.update(key, value1);
        sleep(100);

        String value2 = "value2";
        client.update(key, value2);
        sleep(100);

        client.deleteCurrentBranch(key);
        sleep(100);

        //The acquisition value is after the reception of the event,
        //so the value may be not equal.
        assertThat(actual, hasItems("update_/test/a_value0", "update_/test/a_value1", "update_/test/a_value2", "delete_/test/a_"));
        client.unregisterWatch(zookeeperListener.getKey());
    }
    
    protected ZookeeperListener buildListener(IClient client, List<String> actual){
        return new ZookeeperListener(null) {

            @Override
            public void process(final WatchedEvent event) {
                System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                System.out.println(event.toString());
                switch (event.getType()) {
                    case NodeDataChanged:
                    case NodeChildrenChanged:
                        try {
                            actual.add("update_" + event.getPath() + "_" + client.getDataString(event.getPath()));
                        } catch (final KeeperException | InterruptedException ignored) {
                        }
                        break;
                    case NodeDeleted:
                        actual.add("delete_" + event.getPath() + "_");
                        break;
                    default:
                }
                System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            }
        };
    }
    
    protected void close(IClient client) {
        client.close();
        assertThat(getZooKeeper(client).getState(), is(ZooKeeper.States.CLOSED));
    }
}
