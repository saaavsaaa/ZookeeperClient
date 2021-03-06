package com.saaavsaaa.client.zookeeper;

import com.saaavsaaa.client.action.IClient;
import com.saaavsaaa.client.retry.AsyncRetryCenter;
import com.saaavsaaa.client.retry.DelayRetryPolicy;
import com.saaavsaaa.client.utility.constant.StrategyType;
import com.saaavsaaa.client.zookeeper.section.ZookeeperListener;
import org.apache.zookeeper.ZooDefs;
import org.junit.Before;

import java.io.IOException;

/**
 * Created by aaa
 */
public class AsyncRetryStrategyTest extends UsualClientTest{
    @Before
    public void start() throws IOException, InterruptedException {
        super.start();
        AsyncRetryCenter.INSTANCE.init(new DelayRetryPolicy(3, 3, 10));
        AsyncRetryCenter.INSTANCE.start();
    }
    
    @Override
    protected IClient createClient(final ClientFactory creator) throws IOException, InterruptedException {
        ZookeeperListener listener = TestSupport.buildListener();
        IClient client = creator.setNamespace(TestSupport.ROOT).authorization(TestSupport.AUTH, TestSupport.AUTH.getBytes(), ZooDefs.Ids.CREATOR_ALL_ACL).newClient(TestSupport.SERVERS, TestSupport.SESSION_TIMEOUT).watch(listener).start();
        client.useExecStrategy(StrategyType.ASYNC_RETRY);
        return client;
    }
}
