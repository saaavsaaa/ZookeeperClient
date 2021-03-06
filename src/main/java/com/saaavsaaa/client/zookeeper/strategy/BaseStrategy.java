package com.saaavsaaa.client.zookeeper.strategy;

import com.saaavsaaa.client.action.IExecStrategy;
import com.saaavsaaa.client.action.IProvider;
import com.saaavsaaa.client.zookeeper.transaction.BaseTransaction;
import org.apache.zookeeper.KeeperException;

/*
 * Created by aaa
 */
public abstract class BaseStrategy implements IExecStrategy {
    protected final IProvider provider;
    public BaseStrategy(final IProvider provider){
        this.provider = provider;
    }
    
    @Override
    public String getDataString(final String key) throws KeeperException, InterruptedException {
        return new String(getData(key));
    }
    
    public IProvider getProvider() {
        return provider;
    }
    
    @Override
    public BaseTransaction transaction() {
        return provider.transaction();
    }
}
