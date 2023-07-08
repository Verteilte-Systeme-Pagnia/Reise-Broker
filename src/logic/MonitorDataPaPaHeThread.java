package logic;

import java.net.DatagramPacket;

public class MonitorDataPaPaHeThread {//Daten zwischen partizipant receive und partizipantHelperThread
    private DatagramPacket datagramPacketRequestingParticipant;

    public synchronized DatagramPacket getDatagramPacketRequestingParticipant(){
        return this.datagramPacketRequestingParticipant;
    }

    public synchronized void setDatagramPacketRequestingParticipant(DatagramPacket datagramPacketRequestingParticipant){
        this.datagramPacketRequestingParticipant = datagramPacketRequestingParticipant;
    }
}
