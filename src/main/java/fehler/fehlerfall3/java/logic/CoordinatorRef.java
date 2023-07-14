package fehler.fehlerfall3.java.logic;

import fehler.fehlerfall3.java.logic.transaction.states_coordinator;

import java.net.InetAddress;

public class CoordinatorRef {//Coordinator Referenz da die Objekte direkt nicht vorliegen da unterschiedliche Systeme deshalb nur Referenzspeicherung
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
