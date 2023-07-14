package working2pc.logic;

import working2pc.logic.transaction.TransactionCoordinator;
import working2pc.logic.transaction.states_coordinator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class MonitorDataCoCoThread{//verwaltet den Zugriff für die gemeinsame Datenstruktur zwischen Coordinator Receive und CoordinatorThread
    private Map<UUID, TransactionCoordinator> uuidTransactionCoordinatorMap;
    private int ctrAdd = 0;
    private int ctrRM = 0;
    private int ctrRead = 0;
    private Semaphore semaphore = new Semaphore(1,true);

    public MonitorDataCoCoThread(){
        this.uuidTransactionCoordinatorMap = new HashMap<UUID, TransactionCoordinator>();
    }

    public void addTransaction(TransactionCoordinator transaction){
        try {
            semaphore.acquire();
            this.uuidTransactionCoordinatorMap.put(transaction.getUUID(), transaction);
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public TransactionCoordinator getTransaction(UUID uuid){
        try {
            semaphore.acquire();
            TransactionCoordinator tempTransaction = this.uuidTransactionCoordinatorMap.get(uuid);
            semaphore.release();
            return tempTransaction;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void setTransactionStatus(UUID uuid, states_coordinator stateC){
        try {
            semaphore.acquire();
            this.uuidTransactionCoordinatorMap.get(uuid).setStateC(stateC);
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void rmTranscaction(UUID uuid){
        try {
            semaphore.acquire();
            this.uuidTransactionCoordinatorMap.remove(uuid);
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public Map<UUID, TransactionCoordinator> getUuidTransactionCoordinatorMap(){
        try{
            semaphore.acquire();
            Map<UUID, TransactionCoordinator> temp = this.uuidTransactionCoordinatorMap;
            semaphore.release();
            return temp;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    


}