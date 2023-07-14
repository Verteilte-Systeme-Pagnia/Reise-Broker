package fehler.fehlerfall3.java.logic;

import fehler.fehlerfall3.java.logic.transaction.TransactionParticipant;
import fehler.fehlerfall3.java.logic.transaction.states_participant;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class MonitorDataPaPaThread { //verwaltung gemeinsamer Datenstrukturen zwischen zwischen partizipantreceive und partizipantThread
    private Map<UUID, TransactionParticipant> uuidTransactionParticipantMap;
    private Semaphore semaphore = new Semaphore(1,true);
    
    public MonitorDataPaPaThread(){
        try {
            semaphore.acquire();
            uuidTransactionParticipantMap = new HashMap<>();
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void addTransaction(UUID uuid, TransactionParticipant transactionParticipant){
        try {
            semaphore.acquire();
            this.uuidTransactionParticipantMap.put(uuid, transactionParticipant);
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public TransactionParticipant getTransaction(UUID uuid){
        try {
            semaphore.acquire();
            TransactionParticipant tempTransaction = this.uuidTransactionParticipantMap.get(uuid);
            semaphore.release();
            return tempTransaction;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void setTransactionStatus(UUID uuid, states_participant stateP){
        try {
            semaphore.acquire();
            this.uuidTransactionParticipantMap.get(uuid).setStateP(stateP);
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<UUID, TransactionParticipant> getUuidTransactionParticipantMap(){
        try {
            semaphore.acquire();
            Map<UUID, TransactionParticipant> tempMap = this.uuidTransactionParticipantMap;
            semaphore.release();
            return tempMap;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
