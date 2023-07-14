package fehler.fehlerfall2.java.logic;

import java.net.DatagramPacket;
import java.util.concurrent.Semaphore;

public class MonitorDataPaPaHeThread {//Daten zwischen partizipant receive und partizipantHelperThread
    private DatagramPacket datagramPacketRequestingParticipant;
    private Semaphore semaphore = new Semaphore(1,true);

    public  DatagramPacket getDatagramPacketRequestingParticipant(){
        try {
            semaphore.acquire();
            DatagramPacket tempDP = this.datagramPacketRequestingParticipant;
            semaphore.release();
            return tempDP;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public  void setDatagramPacketRequestingParticipant(DatagramPacket datagramPacketRequestingParticipant){
        try {
            semaphore.acquire();
            this.datagramPacketRequestingParticipant = datagramPacketRequestingParticipant;
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
