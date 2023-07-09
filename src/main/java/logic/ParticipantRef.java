package logic;// Package not detected, please report project structure on CodeTogether's GitHub Issues
import logic.transaction.states_participant;

import java.net.*;

public class ParticipantRef {
    private InetAddress address;
    private int port;
    private states_participant stateP;

    public ParticipantRef(InetAddress address, int port){
        this.address = address;
        this.port = port;
        this.stateP = states_participant.INIT;
    }

    public InetAddress getAddress() {
            return address;
        }

    public int getPort() {
        return port;
    }

    public states_participant getStateP() {
        return stateP;
    }

    public void setStateP(states_participant stateP) {
        this.stateP = stateP;
    }
}