package logic.transaction;

public class TransactionCoordinator extends Transaction {
    private states_coordinator stateC;
    public SenderReference senderReference;
    public String finalResult;

    public TransactionCoordinator(SenderReference senderReference){
        super();
        this.stateC = states_coordinator.INIT;
        this.senderReference = senderReference;
    }

    public void setStateC(states_coordinator stateC){
        this.stateC = stateC;
    }

    public states_coordinator getStateC(){
        return this.stateC;
    }



}
