package logic;

import logic.transaction.*;

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
    private String filename;
    public ParticipantReceive(ArrayList<CoordinatorRef> coordinatorRefs,ArrayList<ParticipantRef> participantRefs, String filename){
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
        if(type.equals("Hotel")){
            databaseHotel = new DatabaseHotel();
        }else if(type.equals("Autoverleih")){
            databaseAutoverleih = new DatabaseAutoverleih();
        }
        DatagramSocket socket = null;
        try{
            socket = new DatagramSocket(socketPort);

            Scanner scanner = new Scanner(new File(this.filename));
            System.out.println("logic.ParticipantReceive Sanner wurde erzeugt");
            while(scanner.hasNext()) {
                String[] line = scanner.nextLine().split(" ");
                if(this.monitorDataPaPaThread.getTransaction(UUID.fromString(line[0])) == null){
                    TransactionParticipant transactionParticipant = new TransactionParticipant(new SenderReference(Integer.parseInt(line[3]),InetAddress.getByName(line[2].replace("/",""))),Integer.parseInt(line[5]),Integer.parseInt(line[6]),line[7],line[8]);
                    transactionParticipant.setUUID(UUID.fromString(line[0]));
                    if(line[1].equals("INIT") || line[1].equals("READY") || line[1].equals("COMMIT") || line[1].equals("ABORT") || line[1].equals("ACK")){
                        String dpData = line[4] +" "+ line[5];
                        DatagramPacket datagrampacket = new DatagramPacket(dpData.getBytes(),0,dpData.length());
                        transactionParticipant.setDatagramPacket(datagrampacket);
                    }

                    this.monitorDataPaPaThread.addTransaction(UUID.fromString(line[0]),transactionParticipant);
                }else{
                    System.out.println("setze im else zweig state c:");
                    this.monitorDataPaPaThread.getTransaction(UUID.fromString(line[0])).setStateP(states_participant.valueOf(line[1]));
                    if(line[1].equals("INIT") || line[1].equals("READY") || line[1].equals("COMMIT") || line[1].equals("ABORT") || line[1].equals("ACK")) {
                        String dpData = line[4] + " " + line[5];
                        DatagramPacket datagrampacket = new DatagramPacket(dpData.getBytes(), 0, dpData.length());
                        this.monitorDataPaPaThread.getTransaction(UUID.fromString(line[0])).setDatagramPacket(datagrampacket);
                    }

                }            }
            System.out.println("logic.ParticipantReceive holt sich als nächstes map");

            for (Map.Entry<UUID, TransactionParticipant> entry : this.monitorDataPaPaThread.getUuidTransactionParticipantMap().entrySet()) {
                UUID key = entry.getKey();
                if(type.equals("Hotel")){
                    Thread participantThread = new ParticipantThread(key, this.monitorDataPaPaThread, this.writeLogFileMonitor, socket, this.participantRefs, type, databaseHotel);
                    participantThread.start();
                }else if(type.equals("Autoverleih")){
                    Thread participantThread = new ParticipantThread(key, this.monitorDataPaPaThread, this.writeLogFileMonitor, socket, this.participantRefs, type, databaseAutoverleih);
                    participantThread.start();
                }
            }

            Thread participantHelperThread = new ParticipantHelperThread(this.monitorDataPaPaHeThread, socket,this.writeLogFileMonitor);
            participantHelperThread.start();
            System.out.println("logic.ParticipantReceive startet logic.ParticipantHelperThread");
            while(true) {
                byte buffer[] = new byte[65507];
                DatagramPacket receiveDP = new DatagramPacket(buffer,buffer.length);
                socket.receive(receiveDP);
                System.out.println("logic.ParticipantReceive hat paket empfangen");

                String msg = new String(receiveDP.getData(),0,receiveDP.getLength()); //Aufbau von Koordinator: UUID MSG ... /Aufbau von anderem Partizipanten:
                //if -> from coordinator else if -> from other participant -> from a Client also ignore
                System.out.println("logic.ParticipantReceive "+msg);
                String[] clientmsg = msg.split(" ");
                if(clientmsg[0].equals("InitializeClient")){
                    if(type.equals("Hotel")) {
                        Thread sendClientInformation = new ParticipantInitClientThread(type,databaseAutoverleih, receiveDP,socket);
                        sendClientInformation.start();
                    }else if(type.equals("Autoverleih")){
                        Thread sendClientInformation = new ParticipantInitClientThread(type,databaseHotel,receiveDP,socket);
                        sendClientInformation.start();
                    }
                }else {
                    if (this.coordinatorRefs.stream().anyMatch(coordinatorRef -> coordinatorRef.getAddress().equals(receiveDP.getAddress()) && coordinatorRef.getPort() == receiveDP.getPort())) {
                        String[] splitMSG = msg.split(" ");//Aufbau von Koordinator: UUID MSG ...
                        if (this.monitorDataPaPaThread.getTransaction(UUID.fromString(splitMSG[0])) == null) {//nicht in liste bereits enthalten?
                            TransactionParticipant transactionParticipant = new TransactionParticipant(new SenderReference(receiveDP.getPort(), receiveDP.getAddress()), Integer.parseInt(splitMSG[2]), Integer.parseInt(splitMSG[3]), splitMSG[4], splitMSG[5]);

                            transactionParticipant.setDatagramPacket(receiveDP);

                            this.monitorDataPaPaThread.addTransaction(UUID.fromString(splitMSG[0]), transactionParticipant);
                            if (type.equals("Hotel")) {
                                Thread participantThread = new ParticipantThread(UUID.fromString(splitMSG[0]), this.monitorDataPaPaThread, this.writeLogFileMonitor, socket, this.participantRefs, type, databaseHotel);
                                participantThread.start();
                            } else if (type.equals("Autoverleih")) {
                                Thread participantThread = new ParticipantThread(UUID.fromString(splitMSG[0]), this.monitorDataPaPaThread, this.writeLogFileMonitor, socket, this.participantRefs, type, databaseAutoverleih);
                                participantThread.start();
                            }
                            System.out.println("logic.ParticipantReceive startet einen neuen Thread");
                        } else {
                            //lege neues Datagrampacket packet hinein
                            while (this.monitorDataPaPaThread.getTransaction(UUID.fromString(splitMSG[0])).getDatagramPacket() != null) {
                                //warte bis Paket von Thread entnommen wurde
                            }
                            this.monitorDataPaPaThread.getTransaction(UUID.fromString(splitMSG[0])).setDatagramPacket(receiveDP);
                            System.out.println("logic.ParticipantReceive hat neues Datagrampacket abgelegt");
                        }
                    } else if (this.participantRefs.stream().anyMatch(participantRef -> participantRef.getAddress().equals(receiveDP.getAddress()) && participantRef.getPort() == receiveDP.getPort())) {
                        //gib dies dem participanthelper thread dieser schickt nachricht an den nachfrager thread
                        String[] splitMSG = msg.split(" ");//Aufbau von Partizipanten: UUID MSG ...
                        if (splitMSG.equals("DESICION_REQUEST")) {
                            while (this.monitorDataPaPaHeThread.getDatagramPacketRequestingParticipant() != null) {
                                //warte bis Paket von Thread entnommen wurde
                            }
                            this.monitorDataPaPaHeThread.setDatagramPacketRequestingParticipant(receiveDP);
                        } else {
                            this.monitorDataPaPaThread.getTransaction(UUID.fromString(splitMSG[0])).setDatagramPacket(receiveDP);
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
