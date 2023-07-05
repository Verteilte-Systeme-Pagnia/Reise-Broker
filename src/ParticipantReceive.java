import transaction.TransactionParticipant;
import transaction.WriteLogFile;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.UUID;

public class ParticipantReceive {
    private MonitorDataPaPaThread monitorDataPaPaThread;
    private MonitorDataPaPaHeThread monitorDataPaPaHeThread;
    private ArrayList<CoordinatorRef> coordinatorRefs;
    private ArrayList<ParticipantRef> participantRefs;
    private WriteLogFile writeLogFileMonitor;
    public ParticipantReceive(ArrayList<CoordinatorRef> coordinatorRefs,ArrayList<ParticipantRef> participantRefs, String filename){
        this.monitorDataPaPaThread = new MonitorDataPaPaThread();
        this.monitorDataPaPaHeThread = new MonitorDataPaPaHeThread();
        this.coordinatorRefs = coordinatorRefs;
        this.participantRefs = participantRefs;
        this.writeLogFileMonitor = new WriteLogFile(filename);
    }
    public static void main(String[] args) {
        ArrayList<CoordinatorRef> coordinatorRefs = new ArrayList<>();

        coordinatorRefs.add(new CoordinatorRef(Config.Coordinator1Address,Config.Coordinator1Port));
        coordinatorRefs.add(new CoordinatorRef(Config.Participant2Address,Config.Coordinator2Port));

        ArrayList<ParticipantRef> participantRefs = new ArrayList<>();

        participantRefs.add(new ParticipantRef(Config.Participant2Address,Config.Participant2Port));

        ParticipantReceive participantReceive1 = new ParticipantReceive(coordinatorRefs,participantRefs,"Participant1LogFile.txt");

        DatagramSocket socket = null;
        try{
            socket = new DatagramSocket(Config.Participant1Port);

            Thread participantHelperThread = new ParticipantHelperThread(participantReceive1.monitorDataPaPaHeThread, socket,participantReceive1.writeLogFileMonitor);
            participantHelperThread.start();

            while(true) {
                byte buffer[] = new byte[65507];
                DatagramPacket receiveDP = new DatagramPacket(buffer,buffer.length);
                socket.receive(receiveDP);

                String msg = new String(receiveDP.getData(),0,receiveDP.getLength()); //Aufbau von Koordinator: UUID MSG ... /Aufbau von anderem Partizipanten:
                //if -> from coordinator else if -> from other participant -> from a Client also ignore
                if (participantReceive1.coordinatorRefs.stream().anyMatch(coordinatorRef -> coordinatorRef.getAddress().equals(receiveDP.getAddress()) && coordinatorRef.getPort() == receiveDP.getPort())){
                    String[] splitMSG = msg.split(" ");//Aufbau von Koordinator: UUID MSG ...
                    if(participantReceive1.monitorDataPaPaThread.getTransaction(UUID.fromString(splitMSG[0])) == null){//nicht in liste bereits enthalten?
                        TransactionParticipant transactionParticipant = new TransactionParticipant();
                        participantReceive1.monitorDataPaPaThread.addTransaction(UUID.fromString(splitMSG[0]), transactionParticipant);
                        Thread participantThread = new ParticipantThread(UUID.fromString(splitMSG[0]),participantReceive1.monitorDataPaPaThread,participantReceive1.writeLogFileMonitor,socket );
                        participantThread.start();
                    }else{
                        //lege neues Datagrampacket packet hinein
                        while(participantReceive1.monitorDataPaPaThread.getTransaction(UUID.fromString(splitMSG[0])).getDatagramPacket() != null){
                            //warte bis Paket von Thread entnommen wurde
                        }
                        participantReceive1.monitorDataPaPaThread.getTransaction(UUID.fromString(splitMSG[0])).setDatagramPacket(receiveDP);
                    }
                }else if(participantReceive1.participantRefs.stream().anyMatch(participantRef -> participantRef.getAddress().equals(receiveDP.getAddress()) && participantRef.getPort() == receiveDP.getPort())){
                    //gib dies dem participanthelper thread dieser schickt nachricht an den nachfrager thread
                    String[] splitMSG = msg.split(" ");//Aufbau von Partizipanten: UUID MSG ...
                    while(participantReceive1.monitorDataPaPaHeThread.getDatagramPacketRequestingParticipant() != null){
                        //warte bis Paket von Thread entnommen wurde
                    }
                    participantReceive1.monitorDataPaPaHeThread.setDatagramPacketRequestingParticipant(receiveDP);
                }else{
                    //ignorieren irgenein anderer client
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            if(socket != null){
                socket.close();
            }
        }
    }
}
