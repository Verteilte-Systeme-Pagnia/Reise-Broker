package fehler.fehlerfall1.java.logic;// Package not detected, please report project structure on CodeTogether's GitHub Issues

import fehler.fehlerfall1.java.logic.transaction.states_participant;

import java.net.InetAddress;

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
