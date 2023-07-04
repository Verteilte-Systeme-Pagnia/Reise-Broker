import transaction.TransactionCoordinator;
import transaction.TransactionParticipant;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MonitorDataPaPaThread { //Daten zwischen partizipant receive und partizipantThread
    private Map<UUID, TransactionParticipant> uuidTransactionParticipantMap;
    
    public MonitorDataPaPaThread(){
        uuidTransactionParticipantMap = new HashMap<>();
    }
    
    public void addTransaction(UUID uuid, TransactionParticipant transactionParticipant){
        this.uuidTransactionParticipantMap.put(uuid,transactionParticipant);
    }
    
    public TransactionParticipant getTransaction(UUID uuid){
        return this.uuidTransactionParticipantMap.get(uuid);
    }
}
