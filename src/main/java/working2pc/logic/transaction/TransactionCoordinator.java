package working2pc.logic.transaction;

public class TransactionCoordinator extends Transaction {//erbt von Transaction
    private states_coordinator stateC; //Speicherung des Status der transaktion für den Koordinator
    public SenderReference senderReference; //Speicherung der Sender Reference
    public String finalResult; //Speicherung der Endentscheidung falls der Koordinator ausfällt
    public int rooms; //speicherung der Anzahl der Räume
    public int autos; //Speicherung der anzahl der Autos die gebucht werden für die transaktion
    public String fromDate; //Speicherung von welchem Tag diese gespeichert werden
    public String toDate; //Speicherung bis zu welchem Tag die Transaktion durchgrführt werden soll

    public TransactionCoordinator(SenderReference senderReference, int rooms, int autos, String fromDate, String toDate){
        super();
        this.stateC = states_coordinator.INIT;
        this.senderReference = senderReference;
        this.rooms = rooms;
        this.autos = autos;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public void setStateC(states_coordinator stateC){
        this.stateC = stateC;
    }

    public states_coordinator getStateC(){
        return this.stateC;
    }



}
