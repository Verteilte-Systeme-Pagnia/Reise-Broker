import java.io.*;
import java.net.*;
import java.util.*;
import transaction.*;

public class CoordinatorReceive {
    private MonitorDataCoCoThread monitorDataCoCoThread;

    private ArrayList<ParticipantRef> participants; // Liste der Teilnehmer
    private WriteLogFile writeLogFileMonitor;
    private String logFileName;

    public CoordinatorReceive(ArrayList<ParticipantRef> participants, String logFileName) {
        this.writeLogFileMonitor = new WriteLogFile(logFileName);
        this.participants = participants;
        this.monitorDataCoCoThread = new MonitorDataCoCoThread();
        this.logFileName = logFileName;
    }
    public void initialize(int socketPort){

        DatagramSocket socket = null;
        System.out.println("Participant erstelle socket");
        try{
            socket = new DatagramSocket(socketPort);
            //lese log datei aus
            //LogDateinlesen
            //Threads mit transaktionen neu erzeugen und auf Stand setzen
            Scanner scanner = new Scanner(new File(logFileName));
            while(scanner.hasNext()) {
                String[] line = scanner.nextLine().split(" ");
                if(this.monitorDataCoCoThread.getTransaction(UUID.fromString(line[0])) == null){
                    TransactionCoordinator transactionCoordinator = new TransactionCoordinator(new SenderReference(Integer.parseInt(line[3]),InetAddress.getByName(line[2].replace("/",""))));
                    transactionCoordinator.setUUID(UUID.fromString(line[0]));
                    if(line[1].equals("INIT") || line[1].equals("WAIT")){
                        String dpData = line[4] +" "+ line[5];
                        DatagramPacket datagrampacket = new DatagramPacket(dpData.getBytes(),0,dpData.length());
                        transactionCoordinator.setDatagramPacket(datagrampacket);
                    }else if(line[1].equals("ABORT") || line[1].equals("SENDCLIENT")){
                        String dpData = line[3] +" "+ line[4];
                        DatagramPacket datagrampacket = new DatagramPacket(dpData.getBytes(),0,dpData.length());
                        transactionCoordinator.setDatagramPacket(datagrampacket);
                    }
                    this.monitorDataCoCoThread.addTransaction(transactionCoordinator);
                }else{
                    System.out.println("setze im else zweig state c:");
                    this.monitorDataCoCoThread.getTransaction(UUID.fromString(line[0])).setStateC(states_coordinator.valueOf(line[1]));
                    if(line[1].equals("WAIT")){
                        String dpData = line[4] +" "+ line[5];
                        DatagramPacket datagrampacket = new DatagramPacket(dpData.getBytes(),0,dpData.length());
                        this.monitorDataCoCoThread.getTransaction(UUID.fromString(line[0])).setDatagramPacket(datagrampacket);

                    }else if(line[1].equals("ABORT") || line[1].equals("SENDCLIENT")){
                        String dpData = line[3] +" "+ line[4];
                        DatagramPacket datagrampacket = new DatagramPacket(dpData.getBytes(),0,dpData.length());
                        this.monitorDataCoCoThread.getTransaction(UUID.fromString(line[0])).setDatagramPacket(datagrampacket);

                    }
                    if(states_coordinator.SENDCLIENT.equals(states_coordinator.valueOf(line[1]))){
                        this.monitorDataCoCoThread.getTransaction(UUID.fromString(line[0])).finalResult = line[4].split(":")[1];
                    }
                }
            }
            System.out.println("Hole die map");
            for (Map.Entry<UUID, TransactionCoordinator> entry : this.monitorDataCoCoThread.getUuidTransactionCoordinatorMap().entrySet()) {
                UUID key = entry.getKey();
                Thread coordinatorThread = new CoordinatorThread(key,this.monitorDataCoCoThread,this.writeLogFileMonitor,this.participants,socket);
                coordinatorThread.start();
            }

            while(true){
                 byte[] buffer = new byte[65507];
                 DatagramPacket receiveDP = new DatagramPacket(buffer, buffer.length);
                 socket.receive(receiveDP);
                 System.out.println("DatagramPacket bekommen");

                 if(participants.stream().anyMatch(participantRef -> participantRef.getAddress().equals(receiveDP.getAddress()) && participantRef.getPort() == receiveDP.getPort())){//Nachricht von Partizipanten
                     //ändere von uuid in coordinator list auf den zustand des partizipanten das thread ausführen kann
                     System.out.println(new String(receiveDP.getData(), 0, receiveDP.getLength()));
                     UUID tempUUID = UUID.fromString(new String(receiveDP.getData(), 0, receiveDP.getLength()).split(" ")[0]); // get UUID from message Structure -> UUID Command Content etc.
                     while(this.monitorDataCoCoThread.getTransaction(tempUUID).getDatagramPacket() != null){
                         //warte bis Paket von Thread entnommen wurde
                     }
                     this.monitorDataCoCoThread.getTransaction(tempUUID).setDatagramPacket(receiveDP);
                 }else{//Client
                     TransactionCoordinator transaction = new TransactionCoordinator(new SenderReference(receiveDP.getPort(), receiveDP.getAddress()));
                     System.out.println("Nachricht von Client empfangen");
                     UUID tempUUID = transaction.getUUID();
                     this.monitorDataCoCoThread.addTransaction(transaction);// müssen wir über monitor noch synchronizen
                     System.out.println("Inhalt" + new String(receiveDP.getData(), 0, receiveDP.getLength()));
                     this.monitorDataCoCoThread.getTransaction(tempUUID).setDatagramPacket(receiveDP);
                     Thread coordinatorThread = new CoordinatorThread(tempUUID, this.monitorDataCoCoThread, this.writeLogFileMonitor, this.participants, socket);
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
