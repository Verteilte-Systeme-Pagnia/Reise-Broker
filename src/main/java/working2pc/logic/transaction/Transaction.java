// Package not detected, please report project structure on CodeTogether's GitHub Issues
package working2pc.logic.transaction;

import java.net.DatagramPacket;
import java.util.UUID;
public class Transaction { //Transaktion klasse darin werden Informationen für die jeweilige Transaktion gespeichert bildet Oberklasse für Transaction Participant und Transaction coordinator
    public SenderReference senderReference;//speicherung einer sender reference
    private UUID uuid;//speicherung einer UUID für die Transaktion
    private DatagramPacket datagramPacket; //speicherung von datagrampackets das zu behandeln ist für die Transaktion

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


