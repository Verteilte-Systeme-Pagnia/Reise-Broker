package fehler.fehlerfall1.java.logic.transaction;

public class TransactionParticipant extends Transaction {
    private states_participant stateP; //Speicherung des Status für dne Partizipanten
    public int rooms; //Speicherung der Anzahl der Räume für die Transaktion
    public int autos; //Speicherung der Anzahl der Autos für die Transaktion
    public String fromDate; //speicherung von welchem tag an die jeweiligen räume reserviert werden sollen
    public String toDate; //speicherung bis zu welchem Tag die Räume reserviert werden sollen

    public TransactionParticipant(SenderReference senderReference, int rooms, int autos, String fromDate, String toDate){
        super();
        this.stateP = states_participant.INIT; //INIT status setzung zu Beginn der Transaktion
        this.senderReference = senderReference;
        this.rooms = rooms;
        this.autos = autos;
        this.fromDate = fromDate;
        this.toDate = toDate;

    }

    public void setStateP(states_participant stateP){
        this.stateP = stateP;
    }

    public states_participant getStateP(){
        return this.stateP;
    }
}

