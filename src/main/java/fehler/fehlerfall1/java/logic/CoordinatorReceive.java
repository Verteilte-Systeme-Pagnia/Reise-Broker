package fehler.fehlerfall1.java.logic;

import fehler.fehlerfall1.java.logic.transaction.SenderReference;
import fehler.fehlerfall1.java.logic.transaction.TransactionCoordinator;
import fehler.fehlerfall1.java.logic.transaction.WriteLogFile;
import fehler.fehlerfall1.java.logic.transaction.states_coordinator;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;

public class CoordinatorReceive {
    private MonitorDataCoCoThread monitorDataCoCoThread;

    private ArrayList<ParticipantRef> participants; // Liste der Partizipanten die bekannt sind. dies sind nur referenzen und keine direkten objekte von Partizipant
    private WriteLogFile writeLogFileMonitor; //um in Log file zu schreiben
    private String logFileName; //name der logfile
    private Map<UUID, SenderReference> uuidTransactionParticipantClient; //darin wird eine uuid sowie eine senderreference des clients gespeichert bei verfügbarkeitsanfragen

    public CoordinatorReceive(ArrayList<ParticipantRef> participants, String logFileName) {
        this.writeLogFileMonitor = new WriteLogFile(logFileName);
        this.participants = participants;
        this.monitorDataCoCoThread = new MonitorDataCoCoThread();
        this.logFileName = logFileName;
        this.uuidTransactionParticipantClient = new HashMap<>();
    }
    public void initialize(int socketPort){

        DatagramSocket socket = null;
        try{
            socket = new DatagramSocket(socketPort);
            //lese log datei aus
            //LogDateinlesen
            //Threads mit transaktionen neu erzeugen und auf Stand setzen
            Scanner scanner = new Scanner(new File(logFileName));
            System.out.println("CoordinatorReceive liest log file aus");
            while(scanner.hasNext()) {//liest solange datei eine weitere zeile hat
                String[] line = scanner.nextLine().split(" "); //zwischenspeicherung der zeile und teilen dieser bei einem leerzeichen
                if(this.monitorDataCoCoThread.getTransaction(UUID.fromString(line[0])) == null){//überprüfen ob die uuid des strings in der transaktionsliste bereits existiert falls nein
                    TransactionCoordinator transactionCoordinator = new TransactionCoordinator(new SenderReference(Integer.parseInt(line[3]),InetAddress.getByName(line[2].replace("/",""))),Integer.parseInt(line[4]),Integer.parseInt(line[5]),line[6],line[7]); //erzeugen einer neuen Transaktion anhand der log file
                    transactionCoordinator.setUUID(UUID.fromString(line[0])); //setzen der uuid anhand der line aus der log file
                    if(line[1].equals("INIT") || line[1].equals("WAIT")){//abfrage ob init oder wait aufgrund der länge des geteilten strings
                        String dpData = line[4] +" "+ line[5];//für die erzeugung des dp anhand der logfile, sodass der koordinator wieder in das protokoll einsteigen kann
                        DatagramPacket datagrampacket = new DatagramPacket(dpData.getBytes(),0,dpData.length());
                        transactionCoordinator.setDatagramPacket(datagrampacket);
                    }else if(line[1].equals("ABORT") || line[1].equals("SENDCLIENT")){//abfrage ob init oder wait aufgrund der länge des geteilten strings
                        String dpData = line[3] +" "+ line[4];
                        DatagramPacket datagrampacket = new DatagramPacket(dpData.getBytes(),0,dpData.length());
                        transactionCoordinator.setDatagramPacket(datagrampacket);//ablegen des datagrampackets sodass der später erzuegte thread darauf zugreifen kann
                    }
                    this.monitorDataCoCoThread.addTransaction(transactionCoordinator);//hinzufügen der Transaktion
                }else{//wenn die Transaktions ID bereits vorhanden ist -> aktualisierung des Eintrags anhand der ausgelesenen Zeile
                    this.monitorDataCoCoThread.getTransaction(UUID.fromString(line[0])).setStateC(states_coordinator.valueOf(line[1])); //aktulaisierte Statussetzung des Koordinators
                    if(line[1].equals("WAIT")){//abfragen der zeile aufgrund der länge der zeile
                        String dpData = line[4] +" "+ line[5];
                        DatagramPacket datagrampacket = new DatagramPacket(dpData.getBytes(),0,dpData.length());
                        this.monitorDataCoCoThread.getTransaction(UUID.fromString(line[0])).setDatagramPacket(datagrampacket);

                    }
                    if(states_coordinator.SENDCLIENT.equals(states_coordinator.valueOf(line[1]))){
                        this.monitorDataCoCoThread.getTransaction(UUID.fromString(line[0])).finalResult = line[4].split(":")[1];
                    }
                }
            }
            System.out.println("CoordinatorReceive Auslesen und Initialiserung abgeshlossen");
            System.out.println("CoordinatorReceive Starten der Transaktionsthreads -> CoordinatorThread");
            for (Map.Entry<UUID, TransactionCoordinator> entry : this.monitorDataCoCoThread.getUuidTransactionCoordinatorMap().entrySet()) {//holen der map mit sämtlichen Transaktionen um Threads dafür zu erzeugen
                UUID key = entry.getKey();
                Thread coordinatorThread = new CoordinatorThread(key,this.monitorDataCoCoThread,this.writeLogFileMonitor,this.participants,socket); //erzeugung des Threads welcher das 2pC weiter ausführt
                coordinatorThread.start();
            }
            System.out.println("CoordinatorReceive Transaktionsthreads wurden gestartet -> CoordinatorThread");

            while(true){
                 byte[] buffer = new byte[65507];
                 DatagramPacket receiveDP = new DatagramPacket(buffer, buffer.length);
                 System.out.println("CoordinatorReceive Bereit um ein DP zu empfangen");
                 socket.receive(receiveDP);//erhalten eines DatagramPackets
                 System.out.println("DP empfangen");
                 String initializeMsg = new String(receiveDP.getData(),0,receiveDP.getLength());//umwnadeln des Datagrampackets in einen String
                 System.out.println("CoordinatorReceive Erhaltene Nachricht: "+initializeMsg);
                System.out.println("CoordinatorReceive Prüfen Nachrichten typ");
                 if(initializeMsg.contains("checkAvailability")) {//prüfen ob es sich um eine Verfügbarkeitsanfrage handelt wenn ja...
                     System.out.println("CoordinatorReceive Prüfen Nachrichten typ CheckAvailability");
                     if (participants.stream().anyMatch(participantRef -> participantRef.getAddress().equals(receiveDP.getAddress()) && participantRef.getPort() == receiveDP.getPort())) {//Prüfen ob Nachricht von einem Partizipanten stammt wenn ja...
                         System.out.println("CoordinatorReceive Prüfen Nachrichten typ CheckAvailability von Partizipant");
                         String[] splitInitializeMsg = initializeMsg.split(" ");//aufsplitten einer Nachricht
                         SenderReference senderReference = uuidTransactionParticipantClient.get(UUID.fromString(splitInitializeMsg[3])); //erhalten der Cleint Reference um die Verfügbarkeitsanfrage an den fehler.fehlerfall1.java.Client zurückzusenden mit den Informationen die vom Partizipanten erhalten wurden
                         socket.send(new DatagramPacket(initializeMsg.getBytes(), initializeMsg.length(), senderReference.getSenderAddress(), senderReference.getSenderPort()));
                     }else{//falls nachricht nicht von einem partipanten stammt muss es sich um eine nachricht eines Clients handeln -> neue Verfügbarkeitsanfrage
                         System.out.println("CoordinatorReceive Prüfen Nachrichten typ CheckAvailability kein Partizipant");
                         SenderReference senderReference = new SenderReference(receiveDP.getPort(),receiveDP.getAddress());//speichern ser senderreference -> die des clients
                         UUID tempUUID = UUID.randomUUID(); //erzeugen einer uuid um eine empfangene nachricht wieder zuordnen zu können
                         uuidTransactionParticipantClient.put(tempUUID,senderReference);
                         String msg = initializeMsg + " "+tempUUID; //mitsenden der UUID und der ursprünglichen Nachricht
                         for(ParticipantRef participant : participants){ //sendet verfügbarkeitsanfrage an jeden beteiligten Partizipanten
                             socket.send(new DatagramPacket(msg.getBytes(), msg.length(), participant.getAddress(), participant.getPort()));
                         }
                     }
                 }else{
                     System.out.println("CoordinatorReceive Prüfen Nachrichten typ kein CheckAvailability");
                     if (participants.stream().anyMatch(participantRef -> participantRef.getAddress().equals(receiveDP.getAddress()) && participantRef.getPort() == receiveDP.getPort())) {//Nachricht von Partizipanten
                         System.out.println("CoordinatorReceive Prüfen Nachrichten typ kein CheckAvailability von Partizipant");
                         //ändere von uuid in coordinator list auf den zustand des partizipanten das thread ausführen kann

                         UUID tempUUID = UUID.fromString(new String(receiveDP.getData(), 0, receiveDP.getLength()).split(" ")[0]); // get UUID from message Structure -> UUID Command Content etc.
                         System.out.println(tempUUID);
                         while (this.monitorDataCoCoThread.getTransaction(tempUUID).getDatagramPacket() != null) {
                             //warte bis Paket von Thread entnommen wurde
                         }
                         System.out.println("CoordinatorReceive Prüfen Nachrichten typ kein CheckAvailability von Partizipant Paket abgelegt");
                         this.monitorDataCoCoThread.getTransaction(tempUUID).setDatagramPacket(receiveDP);
                     } else {//Nachricht von fehler.fehlerfall1.java.Client
                         System.out.println("CoordinatorReceive Prüfen Nachrichten typ kein CheckAvailability von fehler.fehlerfall1.java.Client");
                         String[] receivedMSG = new String(receiveDP.getData(), 0, receiveDP.getLength()).split(" ");//nachricht wird geteilt
                         TransactionCoordinator transaction = new TransactionCoordinator(new SenderReference(receiveDP.getPort(), receiveDP.getAddress()), Integer.parseInt(receivedMSG[0]), Integer.parseInt(receivedMSG[1]), receivedMSG[2], receivedMSG[3]);//erstellung einer neuen Koordinator Transaktion
                         UUID tempUUID = transaction.getUUID();
                         this.monitorDataCoCoThread.addTransaction(transaction);//hinzufügen der transaktion in die transaktionsliste
                         this.monitorDataCoCoThread.getTransaction(tempUUID).setDatagramPacket(receiveDP);//festlegen des Datagrampackets
                         Thread coordinatorThread = new CoordinatorThread(tempUUID, this.monitorDataCoCoThread, this.writeLogFileMonitor, this.participants, socket);//erstellen eines koordinatorenthreads für die erstellte Transaktion
                         coordinatorThread.start(); //ausführen des 2PC Protokolls
                         System.out.println("CoordinatorReceive Prüfen Nachrichten typ kein CheckAvailability von fehler.fehlerfall1.java.Client starten einer neuen Transaktion");
                     }
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
