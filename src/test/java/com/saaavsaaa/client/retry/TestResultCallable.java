package com.saaavsaaa.client.retry;

import com.saaavsaaa.client.action.IProvider;
import com.saaavsaaa.client.zookeeper.core.BaseProvider;
import org.apache.zookeeper.KeeperException;

/**
 * Created by aaa
 */
public abstract class TestResultCallable<T> extends RetryResultCallable<T> {
    private int count = 0;

    public TestResultCallable(final IProvider provider, final DelayRetryPolicy delayRetryPolicy) {
        super(provider, delayRetryPolicy);
    }

    @Override
    public void call() throws KeeperException, InterruptedException {
        if (count < 2){
            count++;
//            throw new KeeperException.SessionExpiredException();
//            throw new KeeperException.ConnectionLossException();

            System.out.println("TestCallable injectSessionExpiration==================================================" + count);
            ((BaseProvider)provider).getHolder().getZooKeeper().getTestable().injectSessionExpiration();
//            ((BaseProvider)provider).getHolder().close();
        }
        System.out.println("TestCallable ================================================" + count);
        test();
    }

    public abstract void test() throws KeeperException, InterruptedException;
}