package transaction;

import transaction.Transaction;

public class TransactionParticipant extends Transaction {
    private states_participant stateP;

    public TransactionParticipant(){
        super();
        this.stateP = states_participant.INIT;

    }

    public void setStateP(states_participant stateP){
        this.stateP = stateP;
    }

    public states_participant getStateP(){
        return this.stateP;
    }
}

