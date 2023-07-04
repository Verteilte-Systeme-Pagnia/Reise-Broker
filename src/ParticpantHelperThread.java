import transaction.WriteLogFile;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.UUID;

public class ParticpantHelperThread extends Thread{
    private MonitorDataPaPaHeThread monitorDataPaPaHeThread;
    private DatagramSocket socket;
    private WriteLogFile writeLogFile;

    public ParticpantHelperThread(MonitorDataPaPaHeThread monitorDataPaPaHeThread, DatagramSocket socket,WriteLogFile writeLogFile){
        this.monitorDataPaPaHeThread = monitorDataPaPaHeThread;
        this.socket = socket;
        this.writeLogFile = writeLogFile;
    }

    public void run() {
        while(true){
            if(monitorDataPaPaHeThread.getDatagramPacketRequestingParticipant() != null){
                try {
                    DatagramPacket tempDatagrampacket = monitorDataPaPaHeThread.getDatagramPacketRequestingParticipant();
                    monitorDataPaPaHeThread.setDatagramPacketRequestingParticipant(null);
                    String lastRecordedState = this.writeLogFile.readLastRecordedState(UUID.fromString(new String(tempDatagrampacket.getData(), 0, tempDatagrampacket.getLength()).split(" ")[0]));
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
