import transaction.TransactionCoordinator;
import transaction.TransactionParticipant;
import transaction.WriteLogFile;
import transaction.states_participant;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;

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
    public static void main(String[] args) throws UnknownHostException {
        ArrayList<CoordinatorRef> coordinatorRefs = new ArrayList<>();

        coordinatorRefs.add(new CoordinatorRef(InetAddress.getByName("localhost"),Config.Coordinator1Port));
        coordinatorRefs.add(new CoordinatorRef(InetAddress.getByName("localhost"),Config.Coordinator2Port));

        ArrayList<ParticipantRef> participantRefs = new ArrayList<>();

        participantRefs.add(new ParticipantRef(InetAddress.getByName("localhost"),Config.Participant2Port));

        ParticipantReceive participantReceive1 = new ParticipantReceive(coordinatorRefs,participantRefs,"Participant1LogFile.txt");

        DatagramSocket socket = null;
        try{
            socket = new DatagramSocket(Config.Participant1Port);

            Scanner scanner = new Scanner(new File("Participant1LogFile.txt"));
            System.out.println("ParticipantReceive Sanner wurde erzeugt");
            if(scanner.hasNext()) {
                String[] line = scanner.nextLine().split(" ");
                if(!line[0].equals("")) {
                    if (participantReceive1.monitorDataPaPaThread.getTransaction(UUID.fromString(line[0])) == null) {
                        TransactionParticipant transactionParticipant = new TransactionParticipant();
                        participantReceive1.monitorDataPaPaThread.addTransaction(UUID.fromString(line[0]), transactionParticipant);
                    } else {
                        participantReceive1.monitorDataPaPaThread.getTransaction(UUID.fromString(line[0])).setStateP(states_participant.valueOf(line[1]));
                    }
                }
            }
            System.out.println("ParticipantReceive holt sich als nächstes map");

            for (Map.Entry<UUID, TransactionParticipant> entry : participantReceive1.monitorDataPaPaThread.getUuidTransactionParticipantMap().entrySet()) {
                UUID key = entry.getKey();
                Thread participantThread = new ParticipantThread(key,participantReceive1.monitorDataPaPaThread,participantReceive1.writeLogFileMonitor,socket, participantReceive1.participantRefs );

                participantThread.start();
            }

            Thread participantHelperThread = new ParticipantHelperThread(participantReceive1.monitorDataPaPaHeThread, socket,participantReceive1.writeLogFileMonitor);
            participantHelperThread.start();
            System.out.println("ParticipantReceive startet ParticipantHelperThread");
            while(true) {
                byte buffer[] = new byte[65507];
                DatagramPacket receiveDP = new DatagramPacket(buffer,buffer.length);
                socket.receive(receiveDP);
                System.out.println("ParticipantReceive hatb paket empfangen");

                String msg = new String(receiveDP.getData(),0,receiveDP.getLength()); //Aufbau von Koordinator: UUID MSG ... /Aufbau von anderem Partizipanten:
                //if -> from coordinator else if -> from other participant -> from a Client also ignore
                System.out.println("ParticipantReceive "+msg);
                if (participantReceive1.coordinatorRefs.stream().anyMatch(coordinatorRef -> coordinatorRef.getAddress().equals(receiveDP.getAddress()) && coordinatorRef.getPort() == receiveDP.getPort())){
                    String[] splitMSG = msg.split(" ");//Aufbau von Koordinator: UUID MSG ...
                    if(participantReceive1.monitorDataPaPaThread.getTransaction(UUID.fromString(splitMSG[0])) == null){//nicht in liste bereits enthalten?
                        TransactionParticipant transactionParticipant = new TransactionParticipant();
                        participantReceive1.monitorDataPaPaThread.addTransaction(UUID.fromString(splitMSG[0]), transactionParticipant);
                        Thread participantThread = new ParticipantThread(UUID.fromString(splitMSG[0]),participantReceive1.monitorDataPaPaThread,participantReceive1.writeLogFileMonitor,socket, participantReceive1.participantRefs );
                        participantThread.start();
                        System.out.println("ParticipantReceive startet einen neuen Thread");
                    }else{
                        //lege neues Datagrampacket packet hinein
                        while(participantReceive1.monitorDataPaPaThread.getTransaction(UUID.fromString(splitMSG[0])).getDatagramPacket() != null){
                            //warte bis Paket von Thread entnommen wurde
                        }
                        participantReceive1.monitorDataPaPaThread.getTransaction(UUID.fromString(splitMSG[0])).setDatagramPacket(receiveDP);
                        System.out.println("ParticipantReceive hat neues Datagrampacket abgelegt");
                    }
                }else if(participantReceive1.participantRefs.stream().anyMatch(participantRef -> participantRef.getAddress().equals(receiveDP.getAddress()) && participantRef.getPort() == receiveDP.getPort())){
                    //gib dies dem participanthelper thread dieser schickt nachricht an den nachfrager thread
                    String[] splitMSG = msg.split(" ");//Aufbau von Partizipanten: UUID MSG ...
                    if(splitMSG.equals("DESICION_REQUEST")){
                        while(participantReceive1.monitorDataPaPaHeThread.getDatagramPacketRequestingParticipant() != null){
                            //warte bis Paket von Thread entnommen wurde
                        }
                        participantReceive1.monitorDataPaPaHeThread.setDatagramPacketRequestingParticipant(receiveDP);
                    }else{
                        participantReceive1.monitorDataPaPaThread.getTransaction(UUID.fromString(splitMSG[0])).setDatagramPacket(receiveDP);
                    }
                }else{
                    //ignorieren irgendein anderer client
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
