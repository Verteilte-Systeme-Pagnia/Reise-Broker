package logic.transaction;
import java.net.*;

public class SenderReference {//Referenz die Informationen für den Absender einer Nachricht gespeichter werden Bsp. Client.
    private int port;
    private InetAddress inetAddress;
    public SenderReference(int port, InetAddress inetAddress){
        this.port = port;
        this.inetAddress = inetAddress;
    }
    public int getSenderPort() {
        return port;
    }

    public InetAddress getSenderAddress() {
        return inetAddress;
    }
}
