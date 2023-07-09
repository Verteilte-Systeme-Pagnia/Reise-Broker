package logic.transaction;

public class TransactionParticipant extends Transaction {
    private states_participant stateP;
    public int rooms;
    public int autos;
    public String fromDate;
    public String toDate;

    public TransactionParticipant(SenderReference senderReference, int rooms, int autos, String fromDate,String toDate){
        super();
        this.stateP = states_participant.INIT;
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

