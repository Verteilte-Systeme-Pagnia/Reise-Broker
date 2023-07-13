package logic;

import logic.MonitorDataPaPaHeThread;
import logic.transaction.WriteLogFile;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.UUID;

public class ParticipantHelperThread extends Thread{//wird von Participant Receive erzeugt und existiert einmalig
    private MonitorDataPaPaHeThread monitorDataPaPaHeThread;
    private DatagramSocket socket;
    private WriteLogFile writeLogFile;

    public ParticipantHelperThread(MonitorDataPaPaHeThread monitorDataPaPaHeThread, DatagramSocket socket, WriteLogFile writeLogFile){
        this.monitorDataPaPaHeThread = monitorDataPaPaHeThread;
        this.socket = socket;
        this.writeLogFile = writeLogFile;
    }

    public void run() {
        System.out.println("PartizipantHelperThread wurde gestartet");
        while(true){
            if(monitorDataPaPaHeThread.getDatagramPacketRequestingParticipant() != null){//schaut ob ein datagrampacket für ihn abgelegt wurde wenn nicht == null
                System.out.println("PartizipantHelperThread hat Paket erhalten");
                try {
                    DatagramPacket tempDatagrampacket = monitorDataPaPaHeThread.getDatagramPacketRequestingParticipant(); //holen des Datagrampackets
                    monitorDataPaPaHeThread.setDatagramPacketRequestingParticipant(null);//setzen des Datagrampackets auf null sodass der koordinator receive ein neues hereinlegen kann
                    String lastRecordedState = this.writeLogFile.readLastRecordedState(UUID.fromString(new String(tempDatagrampacket.getData(), 0, tempDatagrampacket.getLength()).split(" ")[0])); //holt den letzten String/line in der logfile von der uuid alle haben die gleiche uuid
                    if (lastRecordedState.equals("ABORT")) {//entsprechend zum 2PC von Tannenbaum wird ein Global abort bei einem abort versendet
                        lastRecordedState = "GLOBAL_ABORT";
                    }
                    else if (lastRecordedState.equals("COMMIT")) {//sendet ein global commit, da bereits vom koordinator bereits ein global commit erhalten wurde
                        lastRecordedState = "GLOBAL_COMMIT";
                    }
                    else if (lastRecordedState.equals("INIT")) {
                        lastRecordedState = "INIT";
                    }
                    DatagramPacket datagramPacketSend = new DatagramPacket(lastRecordedState.getBytes(), 0, lastRecordedState.getBytes().length, tempDatagrampacket.getAddress(), tempDatagrampacket.getPort());
                    socket.send(datagramPacketSend);
                }catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }else{
                //nichts im datagrampacket
               
            }
        }
    }

    private void readLogFileForUUIDLine(){

    }
}
