package logic.transaction;

public class TransactionCoordinator extends Transaction {
    private states_coordinator stateC;
    public SenderReference senderReference;
    public String finalResult;
    public int rooms;
    public int autos;
    public String fromDate;
    public String toDate;

    public TransactionCoordinator(SenderReference senderReference, int rooms, int autos, String fromDate,String toDate){
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
