package logic;

import logic.transaction.*;

import java.util.*;
public class MonitorDataCoCoThread{
    private Map<UUID, TransactionCoordinator> uuidTransactionCoordinatorMap;
    private int ctrAdd = 0;
    private int ctrRM = 0;
    private int ctrRead = 0;

    public MonitorDataCoCoThread(){
        this.uuidTransactionCoordinatorMap = new HashMap<UUID, TransactionCoordinator>();
    }

    public synchronized void addTransaction(TransactionCoordinator transaction){
        this.uuidTransactionCoordinatorMap.put(transaction.getUUID(),transaction);
    }

    public synchronized TransactionCoordinator getTransaction(UUID uuid){
        return this.uuidTransactionCoordinatorMap.get(uuid);
    }

    public synchronized void setTransactionStatus(UUID uuid,states_coordinator stateC){
            this.uuidTransactionCoordinatorMap.get(uuid).setStateC(stateC);
    }

    public synchronized void rmTranscaction(UUID uuid){
        this.uuidTransactionCoordinatorMap.remove(uuid);
    }
    public synchronized Map<UUID, TransactionCoordinator> getUuidTransactionCoordinatorMap(){
        return this.uuidTransactionCoordinatorMap;
    }
    


}