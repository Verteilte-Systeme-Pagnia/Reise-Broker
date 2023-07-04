package transaction;

public class TransactionCoordinator extends Transaction {
    private states_coordinator stateC;
    public ClientReference clientReference;
    public String finalResult;

    public TransactionCoordinator(ClientReference clientReference){
        super();
        this.stateC = states_coordinator.INIT;
        this.clientReference = clientReference;
    }

    public void setStateC(states_coordinator stateC){
        this.stateC = stateC;
    }

    public states_coordinator getStateC(){
        return this.stateC;
    }



}
