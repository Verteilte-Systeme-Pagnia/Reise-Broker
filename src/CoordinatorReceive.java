import java.io.*;
import java.net.*;
import java.util.*;
import transaction.*;

public class CoordinatorReceive {
    private MonitorDataCoCoThread monitorDataCoCoThread;

    private ArrayList<ParticipantRef> participants; // Liste der Teilnehmer
    private WriteLogFile writeLogFileMonitor;

    public CoordinatorReceive(ArrayList<ParticipantRef> participants, String logFileName) {
        this.writeLogFileMonitor = new WriteLogFile(logFileName);
        this.participants = participants;
        this.monitorDataCoCoThread = new MonitorDataCoCoThread();
    }

    public static void main(String[] args) throws UnknownHostException {
        // Beispielverwendung der Klasse TwoPhaseCommitProtocol
        ArrayList<ParticipantRef> participants = new ArrayList<ParticipantRef>();

        ParticipantRef participantRef1 = new ParticipantRef(InetAddress.getByName("localhost"), Config.Participant1Port);
        //ParticipantRef participantRef2 = new ParticipantRef(InetAddress.getByName("localhost"),Config.Participant2Port);

        participants.add(participantRef1);
        //participants.add(participantRef2);


        CoordinatorReceive coordinatorReceive1 = new CoordinatorReceive(participants,"LogFileCoordinator1.txt");
        //LogDateinlesen
        //Threads mit transaktionen neu erzeugen und auf Stand setzen
        DatagramSocket socket = null;
        System.out.println("Participant erstelle socket");
        try{
            socket = new DatagramSocket(Config.Coordinator1Port);
            //lese log datei aus
            Scanner scanner = new Scanner(new File("LogFileCoordinator1.txt"));
            if(scanner.hasNext()) {
                String[] line = scanner.nextLine().split(" ");
                if(coordinatorReceive1.monitorDataCoCoThread.getTransaction(UUID.fromString(line[0])) == null){
                    TransactionCoordinator transactionCoordinator = new TransactionCoordinator(new ClientReference(Integer.parseInt(line[3]),InetAddress.getByName(line[3])));
                    coordinatorReceive1.monitorDataCoCoThread.addTransaction(transactionCoordinator);
                }else{
                    coordinatorReceive1.monitorDataCoCoThread.getTransaction(UUID.fromString(line[0])).setStateC(states_coordinator.valueOf(line[1]));
                    if(states_coordinator.SENDCLIENT.equals(states_coordinator.valueOf(line[1]))){
                        coordinatorReceive1.monitorDataCoCoThread.getTransaction(UUID.fromString(line[0])).finalResult = line[4].split(":")[1];
                    }
                }
            }
            System.out.println("Hole die map");
            for (Map.Entry<UUID, TransactionCoordinator> entry : coordinatorReceive1.monitorDataCoCoThread.getUuidTransactionCoordinatorMap().entrySet()) {
                UUID key = entry.getKey();
                Thread coordinatorThread = new CoordinatorThread(key,coordinatorReceive1.monitorDataCoCoThread,coordinatorReceive1.writeLogFileMonitor,coordinatorReceive1.participants,socket);
                coordinatorThread.start();
            }

            while(true){
                 byte[] buffer = new byte[65507];
                 DatagramPacket receiveDP = new DatagramPacket(buffer, buffer.length);
                 socket.receive(receiveDP);
                 System.out.println("DatagramPacket bekommen");

                 if(participants.stream().anyMatch(participantRef -> participantRef.getAddress().equals(receiveDP.getAddress()) && participantRef.getPort() == receiveDP.getPort())){//Nachricht von Partizipanten
                     //ändere von uuid in coordinator list auf den zustand des partizipanten das thread ausführen kann
                     UUID tempUUID = UUID.fromString(new String(receiveDP.getData(), 0, receiveDP.getLength()).split(" ")[0]); // get UUID from message Structure -> UUID Command Content etc.
                     while(coordinatorReceive1.monitorDataCoCoThread.getTransaction(tempUUID).getDatagramPacket() != null){
                         //warte bis Paket von Thread entnommen wurde
                     }
                     coordinatorReceive1.monitorDataCoCoThread.getTransaction(tempUUID).setDatagramPacket(receiveDP);
                 }else{//Client
                     TransactionCoordinator transaction = new TransactionCoordinator(new ClientReference(receiveDP.getPort(), receiveDP.getAddress()));
                     System.out.println("Nachricht von Client empfangen");
                     UUID tempUUID = transaction.getUUID();
                     coordinatorReceive1.monitorDataCoCoThread.addTransaction(transaction);// müssen wir über monitor noch synchronizen
                     Thread coordinatorThread = new CoordinatorThread(tempUUID, coordinatorReceive1.monitorDataCoCoThread, coordinatorReceive1.writeLogFileMonitor, coordinatorReceive1.participants, socket);
                     coordinatorThread.start();
                     System.out.println("Thread gestartet");
                 }
            }

        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (socket != null) {
                socket.close();
            }
        }

    }
}
