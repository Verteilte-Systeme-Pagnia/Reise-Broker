package fehler.fehlerfall1.java.logic;

import fehler.fehlerfall1.java.logic.transaction.WriteLogFile;

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
                    String splitState = lastRecordedState.split(" ")[1];
                    if (splitState.equals("ABORT")) {//entsprechend zum 2PC von Tannenbaum wird ein Global abort bei einem abort versendet
                        lastRecordedState = new String(tempDatagrampacket.getData(),0,tempDatagrampacket.getLength()).split(" ")[0]+" GLOBAL_ABORT";
                    }
                    else if (splitState.equals("COMMIT")) {//sendet ein global commit, da bereits vom koordinator bereits ein global commit erhalten wurde
                        lastRecordedState = new String(tempDatagrampacket.getData(),0,tempDatagrampacket.getLength()).split(" ")[0]+" GLOBAL_COMMIT";
                    }
                    else if (splitState.equals("INIT")) {
                        lastRecordedState = new String(tempDatagrampacket.getData(),0,tempDatagrampacket.getLength()).split(" ")[0]+" INIT";
                    }
                    DatagramPacket datagramPacketSend = new DatagramPacket(lastRecordedState.getBytes(), 0, lastRecordedState.getBytes().length, tempDatagrampacket.getAddress(), tempDatagrampacket.getPort());
                    socket.send(datagramPacketSend);
                    System.out.println("PartizipantHelperThread Nachricht wurde gesendet: "+lastRecordedState);
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
