import transaction.TransactionParticipant;

import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MonitorDataPaPaHeThread {//Daten zwischen partizipant receive und partizipantHelperThread
    private DatagramPacket datagramPacketRequestingParticipant;

    public synchronized DatagramPacket getDatagramPacketRequestingParticipant(){
        return this.datagramPacketRequestingParticipant;
    }

    public synchronized void setDatagramPacketRequestingParticipant(DatagramPacket datagramPacketRequestingParticipant){
        this.datagramPacketRequestingParticipant = datagramPacketRequestingParticipant;
    }
}
