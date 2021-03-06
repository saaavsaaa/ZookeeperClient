package com.saaavsaaa.client.zookeeper.operation;

import com.saaavsaaa.client.action.IProvider;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

/**
 * Created by aaa
 */
public class CreateCurrentOperation extends BaseOperation {
    private final String key;
    private final String value;
    private final CreateMode createMode;
    
    public CreateCurrentOperation(final IProvider provider, final String key, final String value, final CreateMode createMode) {
        super(provider);
        this.key = key;
        this.value = value;
        this.createMode = createMode;
    }
    
    @Override
    public void execute() throws KeeperException, InterruptedException {
        provider.create(provider.getRealPath(key), value, createMode);
    }
    
    @Override
    public String toString(){
        return String.format("CreateCurrentOperation key:%s,value:%s,createMode:%s", key, value, createMode.name());
    }
}
