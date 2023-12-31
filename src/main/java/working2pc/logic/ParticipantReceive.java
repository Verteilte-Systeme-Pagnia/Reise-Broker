package working2pc.logic;

import working2pc.logic.transaction.SenderReference;
import working2pc.logic.transaction.TransactionParticipant;
import working2pc.logic.transaction.WriteLogFile;
import working2pc.logic.transaction.states_participant;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class ParticipantReceive {
    private MonitorDataPaPaThread monitorDataPaPaThread;//monitor für gemeinsam genutze Datenstrukturen zwischen ParticipantReceive und ParticipantThread
    private MonitorDataPaPaHeThread monitorDataPaPaHeThread; //monitor für gemeinsam genutzte Daten zwischen PartizipantReceive und PartizipantHelperThread
    private ArrayList<CoordinatorRef> coordinatorRefs; //Referenzen in welchen die KoorinatorenRefernzen gespeichert wurden
    private ArrayList<ParticipantRef> participantRefs; //Referenzen in welchen Partizipantenreferenzen gespeichert wurden
    private WriteLogFile writeLogFileMonitor; // um in log file zu schreiben und diese auszulesen
    private String filename;
    public ParticipantReceive(ArrayList<CoordinatorRef> coordinatorRefs, ArrayList<ParticipantRef> participantRefs, String filename){
        this.monitorDataPaPaThread = new MonitorDataPaPaThread();
        this.monitorDataPaPaHeThread = new MonitorDataPaPaHeThread();
        this.coordinatorRefs = coordinatorRefs;
        this.participantRefs = participantRefs;
        this.writeLogFileMonitor = new WriteLogFile(filename);
        this.filename = filename;
    }
    public void initialize(int socketPort, String type){
        DatabaseHotel databaseHotel = null;
        DatabaseAutoverleih databaseAutoverleih = null;
        //Erzeugung eines Datenbankobjektes je nach type
        if(type.equals("Hotel")){
            databaseHotel = new DatabaseHotel();
        }else if(type.equals("Autoverleih")){
            databaseAutoverleih = new DatabaseAutoverleih();
        }
        DatagramSocket socket = null;
        try{
            socket = new DatagramSocket(socketPort);
            System.out.println("ParticipantReceive lesen der Log Datei");
            //Auslesen der Log Datei und festsetzen auf den Stand
            Scanner scanner = new Scanner(new File(this.filename));
            while(scanner.hasNext()) {
                String[] line = scanner.nextLine().split(" ");//splitten der line
                if(this.monitorDataPaPaThread.getTransaction(UUID.fromString(line[0])) == null){//wenn uuid noch nicht in der liste existiert dass
                    TransactionParticipant transactionParticipant = new TransactionParticipant(new SenderReference(Integer.parseInt(line[3]),InetAddress.getByName(line[2].replace("/",""))),Integer.parseInt(line[6]),Integer.parseInt(line[7]),line[8],line[9]); //erzeugen einer neuen Transaktion mit Daten aus der line
                    transactionParticipant.setUUID(UUID.fromString(line[0]));//festlegen der uuid in der transaktion
                    if(line[1].equals("INIT")){
                        String dpData = line[4] +" "+ line[5];
                        DatagramPacket datagrampacket = new DatagramPacket(dpData.getBytes(),0,dpData.length());
                        datagrampacket.setAddress(InetAddress.getByName(line[2].replace("/","")));
                        datagrampacket.setPort(Integer.parseInt(line[3]));
                        transactionParticipant.setDatagramPacket(datagrampacket);
                    }else if(line[1].equals("READY") || line[1].equals("COMMIT") || line[1].equals("ABORT") || line[1].equals("ACK")){
                    }
                    this.monitorDataPaPaThread.addTransaction(UUID.fromString(line[0]),transactionParticipant);//hinzufügen der transaktion zu liste
                }else{//wenn uuid bereits in der liste enthalten, aktualisieren der Daten mittels der ausgelesenen Zeile
                    this.monitorDataPaPaThread.getTransaction(UUID.fromString(line[0])).setStateP(states_participant.valueOf(line[1])); //Aktualisierung des PartizipantenStatus
                    if(line[1].equals("INIT")) {
                        String dpData = line[4] + " " + line[5];//erzeugen des Strings des letzten geloggten Datenbankinhalts
                        DatagramPacket datagrampacket = new DatagramPacket(dpData.getBytes(), 0, dpData.length());
                        this.monitorDataPaPaThread.getTransaction(UUID.fromString(line[0])).setDatagramPacket(datagrampacket);//hinzufügen der Transaktion
                        this.monitorDataPaPaThread.getTransaction(UUID.fromString(line[0])).getDatagramPacket().setAddress(InetAddress.getByName(line[2].replace("/","")));
                        this.monitorDataPaPaThread.getTransaction(UUID.fromString(line[0])).getDatagramPacket().setPort(Integer.parseInt(line[3]));
                    }

                }
            }
            System.out.println("ParticipantReceive Auslesen abgeschlossen");
            System.out.println("ParticipantReceive erzeugen von ParticipantThreads für offene Transaktionen");
            //erzeugen der ParticipantThreads für die Transaktionen
            for (Map.Entry<UUID, TransactionParticipant> entry : this.monitorDataPaPaThread.getUuidTransactionParticipantMap().entrySet()) {
                UUID key = entry.getKey();
                //erzeugen des entsprechenden Threads je nach type
                if(type.equals("Hotel")){
                    //erzeugen des Threads für den type Hotel
                    Thread participantThread = new ParticipantThread(key, this.monitorDataPaPaThread, this.writeLogFileMonitor, socket, this.participantRefs, type, databaseHotel);
                    participantThread.start();
                }else if(type.equals("Autoverleih")){
                    //erzeugen des Threads für den type autoverleih
                    Thread participantThread = new ParticipantThread(key, this.monitorDataPaPaThread, this.writeLogFileMonitor, socket, this.participantRefs, type, databaseAutoverleih);
                    participantThread.start();
                }
            }
            System.out.println("ParticipantReceive ParticipantThreads erzeugt und gestartet");
            //erzeugen des ParticipantHelperThread
            Thread participantHelperThread = new ParticipantHelperThread(this.monitorDataPaPaHeThread, socket,this.writeLogFileMonitor);
            participantHelperThread.start();
            while(true) {
                byte buffer[] = new byte[65507];
                DatagramPacket receiveDP = new DatagramPacket(buffer,buffer.length);
                System.out.println("ParticipantReceive bereit um DP zu empfangen");
                socket.receive(receiveDP);
                System.out.println("ParticipantReceive DP empfangen");
                String msg = new String(receiveDP.getData(),0,receiveDP.getLength()); //Aufbau von Koordinator: UUID MSG ... /Aufbau von anderem Partizipanten:
                System.out.println("ParticipantReceive empfangene Nachricht: "+msg);
                //if -> from coordinator else if -> from other participant -> from a working2pc.Client also ignore
                String[] clientmsg = msg.split(" ");
                if(clientmsg[0].equals("checkAvailability")){//prüfen auf Verfügbarkeitsanfrage
                    if(type.equals("Hotel")) {//wenn von Hotel
                        System.out.println("ParticipantReceive checkAvailability Hotel");
                        //erzeugen eines ParicipantInitClientThread um Verfügbarkeitsinformationen an den absender zurückzusenden(Koordinator)
                        Thread sendClientInformation = new ParticipantInitClientThread(type,databaseHotel, receiveDP,socket); //mit databaseHotel
                        sendClientInformation.start();
                    }else if(type.equals("Autoverleih")){
                        System.out.println("ParticipantReceive checkAvailability Autoverleih");
                        //erzeugen eines ParicipantInitClientThread um Verfügbarkeitsinformationen an den absender zurückzusenden(Koordinator)
                        Thread sendClientInformation = new ParticipantInitClientThread(type,databaseAutoverleih,receiveDP,socket);//mit databaseautoverleih
                        sendClientInformation.start();
                    }
                }else {//wenn keine Verfügbarkeitsanfrage
                    if (this.coordinatorRefs.stream().anyMatch(coordinatorRef -> coordinatorRef.getAddress().equals(receiveDP.getAddress()) && coordinatorRef.getPort() == receiveDP.getPort())) {//überpfung ob nachricht von Koordinator stammt
                        System.out.println("ParticipantReceive Nachricht vom Koordinator empfangen");
                        String[] splitMSG = msg.split(" ");//Aufbau von Koordinator: UUID MSG ...
                        if(((splitMSG[1].equals("GLOBAL_ABORT") || splitMSG[1].equals("GLOBAL_COMMIT"))&& this.monitorDataPaPaThread.getTransaction(UUID.fromString(splitMSG[0])).getStateP().equals(states_participant.FINISH))){
                            String message = splitMSG[0] + " ACK";
                            socket.send(new DatagramPacket(message.getBytes(),message.getBytes().length,receiveDP.getAddress(),receiveDP.getPort()));
                        }else{
                            if (this.monitorDataPaPaThread.getTransaction(UUID.fromString(splitMSG[0])) == null) {//nicht in liste bereits enthalten?
                                TransactionParticipant transactionParticipant = new TransactionParticipant(new SenderReference(receiveDP.getPort(), receiveDP.getAddress()), Integer.parseInt(splitMSG[2]), Integer.parseInt(splitMSG[3]), splitMSG[4], splitMSG[5]);//erzeugung einer Transaktion
                                transactionParticipant.setUUID(UUID.fromString(splitMSG[0]));
                                transactionParticipant.setDatagramPacket(receiveDP);//festlegung datagrampaket
                                this.monitorDataPaPaThread.addTransaction(UUID.fromString(splitMSG[0]), transactionParticipant);//hinzufügen einer Transaktion
                                if (type.equals("Hotel")) {//wenn typ gleich Hotel
                                    //erzeugung des ParticipantThread für 2PC
                                    System.out.println("ParticipantReceive Erzeugung ParticipantThread Auto");
                                    Thread participantThread = new ParticipantThread(UUID.fromString(splitMSG[0]), this.monitorDataPaPaThread, this.writeLogFileMonitor, socket, this.participantRefs, type, databaseHotel);
                                    participantThread.start();
                                } else if (type.equals("Autoverleih")) { //wenn typ autoverleih
                                    //erzeugung des ParticipantThread für 2PC
                                    System.out.println("ParticipantReceive Erzeugung ParticipantThread Autoverleih");
                                    Thread participantThread = new ParticipantThread(UUID.fromString(splitMSG[0]), this.monitorDataPaPaThread, this.writeLogFileMonitor, socket, this.participantRefs, type, databaseAutoverleih);
                                    participantThread.start();
                                }
                            } else {// uuid in liste enthalten
                                //lege neues Datagrampacket packet hinein
                                while (this.monitorDataPaPaThread.getTransaction(UUID.fromString(splitMSG[0])).getDatagramPacket() != null) {
                                    //warte bis Paket von Thread entnommen wurde
                                }
                                this.monitorDataPaPaThread.getTransaction(UUID.fromString(splitMSG[0])).setDatagramPacket(receiveDP);
                                System.out.println("ParticipantReceive hat neues Datagrampacket abgelegt");
                            }
                        }
                    } else if (this.participantRefs.stream().anyMatch(participantRef -> participantRef.getAddress().equals(receiveDP.getAddress()) && participantRef.getPort() == receiveDP.getPort())) {//nachricht von Partizipant
                        System.out.println("ParticipantReceive Nachricht vom Partizipant empfangen");
                        //gib dies dem participanthelper thread dieser schickt nachricht an den nachfrager thread
                        String[] splitMSG = msg.split(" ");//Aufbau von Partizipanten: UUID MSG ...
                        System.out.println(splitMSG[0]);
                        if (splitMSG[1].equals("DECISION_REQUEST")) {//Nachfrage eines anderen Partizipanten
                            System.out.println("ParticipantReceive DECISION_REQUEST erhalten");
                            while (this.monitorDataPaPaHeThread.getDatagramPacketRequestingParticipant() != null) {
                                //warte bis Paket von Thread entnommen wurde
                            }
                            this.monitorDataPaPaHeThread.setDatagramPacketRequestingParticipant(receiveDP);
                        } else {//antwort auf selbst gesendete Nachricht
                            this.monitorDataPaPaThread.getTransaction(UUID.fromString(splitMSG[0])).setDatagramPacket(receiveDP);
                            System.out.println("ParticipantReceive Antwort vom DECISION_REQUEST erhalten");
                        }
                    } else {
                        //ignorieren irgendein anderer client
                    }
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
