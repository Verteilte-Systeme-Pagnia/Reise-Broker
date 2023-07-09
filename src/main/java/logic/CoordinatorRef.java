package logic;

import java.net.InetAddress;
import logic.transaction.states_coordinator;

public class CoordinatorRef {
    private InetAddress address;
    private int port;
    private states_coordinator stateC;
    
    public CoordinatorRef(InetAddress address, int port){
        this.address = address;
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
