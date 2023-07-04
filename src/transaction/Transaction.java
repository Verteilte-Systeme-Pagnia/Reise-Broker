// Package not detected, please report project structure on CodeTogether's GitHub Issues
package transaction;
import java.net.DatagramPacket;
import java.util.*;
public class Transaction {
    private UUID uuid;
    private DatagramPacket datagramPacket;

    public Transaction(){
        this.uuid = uuid.randomUUID();
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public DatagramPacket getDatagramPacket() {
        return datagramPacket;
    }

    public void setDatagramPacket(DatagramPacket datagramPacket) {
        this.datagramPacket = datagramPacket;
    }
}


