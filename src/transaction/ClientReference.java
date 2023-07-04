package transaction;
import java.net.*;

public class ClientReference {
    private int port;
    private InetAddress inetAddress;
    public ClientReference (int port, InetAddress inetAddress){
        this.port = port;
        this.inetAddress = inetAddress;
    }
    public int getClientPort() {
        return port;
    }

    public InetAddress getClientAddress() {
        return inetAddress;
    }
}
