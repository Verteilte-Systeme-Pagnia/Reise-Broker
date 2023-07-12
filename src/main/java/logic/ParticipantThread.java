package logic;

import logic.transaction.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ParticipantThread extends Thread {
    private static final String LOCAL_LOG_FILE = "protokolldatei.txt";
    private WriteLogFile writeLogFileMonitor;
    private String participantName; // Name des Teilnehmers
    private BlockingQueue<String> decisionRequests; // Warteschlange für DECISION_REQUESTs
    private DatagramSocket socket;
    private states_participant stateP;
    private MonitorDataPaPaHeThread monitorDataPaPaHeThread;
    private UUID uuid;
    private MonitorDataPaPaThread monitorDataPaPaThread;
    private DatagramPacket tempDP;
    private ArrayList<ParticipantRef> participantRefs;
    private String type;
    private DatabaseHotel databaseHotel;
    private DatabaseAutoverleih databaseAutoverleih;

    //2 Konstruktoren jeweils für Hotel und Autoverleih

    public ParticipantThread(UUID uuid, MonitorDataPaPaThread monitorDataPaPaThread, WriteLogFile writeLogFileMonitor, DatagramSocket socket,ArrayList<ParticipantRef> participantRefs, String type, DatabaseHotel databaseHotel){
        this.decisionRequests = new LinkedBlockingQueue<>();
        this.uuid = uuid;
        this.monitorDataPaPaThread = monitorDataPaPaThread;
        this.writeLogFileMonitor = writeLogFileMonitor;
        this.socket = socket;
        this.participantRefs = participantRefs;
        this.type = type;
        this.databaseHotel = databaseHotel;
    }

    public ParticipantThread(UUID uuid, MonitorDataPaPaThread monitorDataPaPaThread, WriteLogFile writeLogFileMonitor, DatagramSocket socket,ArrayList<ParticipantRef> participantRefs, String type, DatabaseAutoverleih databaseAutoverleih){
        this.decisionRequests = new LinkedBlockingQueue<>();
        this.uuid = uuid;
        this.monitorDataPaPaThread = monitorDataPaPaThread;
        this.writeLogFileMonitor = writeLogFileMonitor;
        this.socket = socket;
        this.participantRefs = participantRefs;
        this.type = type;
        this.databaseAutoverleih = databaseAutoverleih;
    }

    public void run() {
        initialize();
    }//die initialize Methode wird bei jedem Thread aufgerufen

    private void initialize() { //In der Methode wird geschaut, welchen Zustand der ParticipantThread zurzeit hat und dementsprechend verschiedene Methoden ausgeführt
        while (this.monitorDataPaPaThread.getTransaction(uuid).getStateP() != states_participant.Finish) { //wenn der Thread noch nicht fertig ist läuft die Abfrage, welchen Zustand der ParticipantThread zurzeit hat
            switch (this.monitorDataPaPaThread.getTransaction(uuid).getStateP()) { //Abfrage des Zustands des ParticipantThreads über den Monitor
                case INIT:
                    stateInit();
                    break;
                case READY:
                    stateReady();
                    break;
                case COMMIT:
                    bookRoomCar();
                    break;
                case ABORT:
                    doAbort();
                    break;
                case ACK:
                    ackGlobalMsg();
                    break;
                case Finish:    
            }
        }
    }

    private void stateInit() { //Methode für den initialen Zustand des ParticipantThreads (die ParticipantThreads werden schon im INIT Zustand initialisiert)
        writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid), monitorDataPaPaThread.getTransaction(this.uuid).getDatagramPacket()); //Der aktuelle Zustand und die anderen wichtigen Daten werden in die Log-File geschrieben

        long startTime = System.nanoTime();
        long endTime = startTime + (5 * 1000000000L);
        boolean vote_request = false;
        while (System.nanoTime() < endTime && !vote_request) { //solange die bestimmte Zeit noch nicht vergangen sind und noch kein VOTE_REQUEST vom Koordinator erhalten
            DatagramPacket tempDatagramPacket = this.monitorDataPaPaThread.getTransaction(uuid).getDatagramPacket(); //Das DatagramPacket wird geholt, falls etwas angekommen ist, wurde es dort abgelegt
            if (tempDatagramPacket == null) {
                //nichts Neues angekommen
            } else {
                tempDP = new DatagramPacket(tempDatagramPacket.getData(),tempDatagramPacket.getLength(),tempDatagramPacket.getAddress(),tempDatagramPacket.getPort());

                this.monitorDataPaPaThread.getTransaction(uuid).setDatagramPacket(null);
                System.out.print(tempDP.getAddress());
                String msg[] = new String(tempDatagramPacket.getData(), 0, tempDatagramPacket.getLength()).split(" "); //Die Nachricht aus dem DatagramPacket wird ausgelesen
                System.out.print("nachricht "+msg[1]);
                if ("VOTE_REQUEST".equals(msg[1])) { //falls die Nachricht ein VOTE_REQUEST, geht der Thread aus der Schleife raus
                    vote_request = true;
                    System.out.print(vote_request);
                }
            }
        }


        if(vote_request) { //falls ein VOTE_REQUEST geschickt wurde, wird mit der checkDatabase Methode überprüft, on man die Zimmer/Autos buchen könnte
            if (checkDatabase()) { //falls man Zimmer/Autos buchen kann
                monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.READY); //Zustand des Partizipanten wird auf READY gesetzt
                writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDP);
            } else { //falls man Zimmer/Autos nicht buchen kann
                monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.ABORT); //Zustand des Partizipanten wird auf ABORT gesetzt
                writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDP);
            }
        }
        else { //falls kein VOTE_REQUEST angekommen ist
            monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.ABORT); //Zustand des Partizipanten wird auf ABORT gesetzt
            writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDP);
        }
         System.out.print("amende"+tempDP.getAddress());

    }

    private void stateReady() { //Methode für den Zustand READY des ParticipantThreads
        sendMsgCoordinator(" VOTE_COMMIT"); //sendet VOTE_COMMIT an den Koordinator

        long startTime = System.nanoTime();
        long endTime = startTime + (15 * 1000000000L);
        boolean receivedMsg = false;
        boolean expectParticipantToo = false;

        while (!receivedMsg) { //solange noch keine Nachricht angekommen ist
            DatagramPacket tempDatagramPacket = this.monitorDataPaPaThread.getTransaction(uuid).getDatagramPacket();
            if(System.nanoTime() > endTime){ //falls die bestimmte Zeit zum Empfangen der Antwort des Koordinators abgelaufen ist
                this.participantRefs.stream().forEach(participantRef ->  {sendMsgParticipant(" DESICION_REQUEST",participantRef.getAddress(),participantRef.getPort());}); //sende DECISION_REQUEST an den anderen Partizipanten
                expectParticipantToo = true; //Nachricht vom anderen Partizipanten wird jetzt neben Nachricht vom Koordinator auch erwartet
                 startTime = System.nanoTime();
                 endTime = startTime + (15 * 1000000000L);
            }
            if (tempDatagramPacket == null) {
                //nichts Neues angekommen
            } else {
                if(!expectParticipantToo){ //falls DECISION_REQUEST noch nicht geschickt wurde und aktuelle Wartezeit noch unter der bestimmten Ablaufzeit ist
                    this.monitorDataPaPaThread.getTransaction(uuid).setDatagramPacket(null);
                    String msg[] = new String(tempDatagramPacket.getData(), 0, tempDatagramPacket.getLength()).split(" ");
                    if ("GLOBAL_COMMIT".equals(msg[1])) { //falls GLOBAL_COMMIT vom Koordinator empfangen
                        monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.COMMIT); //setze Status des Partizipanten auf COMMIT
                        writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDatagramPacket);
                        receivedMsg = true;
                    } else if ("GLOBAL_ABORT".equals(msg[1])) { //falls GLOBAL_ABORT vom Koordinator empfangen
                        monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.ACK); //setze Status des Partizipanten auf ACK (dass der thread nicht ins ABORT wechselt, sondern direkt ins ACK liegt an unserer Implementierung)
                        writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDatagramPacket);
                        if(type.equals("Hotel")){
                            databaseHotel.cancelReservation(String.valueOf(this.uuid)); //falls es ein Hotel ist, wird das reservierte Zimmer auf der Datenbank wieder von der Reservierung befreit
                        }else if(type.equals("Autoverleih")){
                            databaseAutoverleih.cancelReservation(String.valueOf(this.uuid)); //falls es ein Autoverleih ist, wird das reservierte Auto auf der Datenbank wieder von der Reservierung befreit
                        }
                        receivedMsg = true;
                    }
                }else{  //ab jetzt wird sowohl auf eine Antwort vom Koordinator, als auch vom anderen Partizipanten erwartet
                    this.monitorDataPaPaThread.getTransaction(uuid).setDatagramPacket(null);
                    String msg[] = new String(tempDatagramPacket.getData(), 0, tempDatagramPacket.getLength()).split(" ");
                    if ("GLOBAL_COMMIT".equals(msg[1])) {
                        monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.COMMIT);
                        writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDatagramPacket);
                        receivedMsg = true;
                    }else if ("GLOBAL_ABORT".equals(msg[1]) || "INIT".equals(msg[1])) { //neben dem GLOBAL_ABORT kann jetzt auch ein INIT ankommen, womit der Partizipant auch theoretisch ins ABORT geht (bei uns direkt ins ACK)
                         monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.ACK);
                         writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDatagramPacket);
                         receivedMsg = true;
                    }
                }
            }
        } //die while-Schleife endet nicht solange keine erwartete Nachricht ankommt, d.h. auch bei einem READY des anderen Partizipanten läuft die Schleife weiter und es wird auf eine gültige Nachricht gewartet

    }

    private void doAbort() { //Methode für den Zustand ABORT des ParticipantThreads (bei uns nur für den Fall, dass der Zustand READY nie betreten wird un der Partizipant nach dem INIT direkt ins ABORT wechselt)
        sendMsgCoordinator(" VOTE_ABORT"); //VOTE_ABORT wird an den Koordinator gesendet
        System.out.println("ABOOOOOOOOOOOOOOORT");
        boolean receivedMsg = false;

        while(!receivedMsg){ //solange noch keine Antwort vom Koordinator angekommen ist
            tempDP = this.monitorDataPaPaThread.getTransaction(uuid).getDatagramPacket();
            if(tempDP == null){

            }else{
                this.monitorDataPaPaThread.getTransaction(uuid).setDatagramPacket(null);
                String[] msg = new String(tempDP.getData(),0,tempDP.getLength()).split(" ");
                if(msg[1].equals("GLOBAL_ABORT")){ //sobald ein GLOBAL_ABORT empfangen wird, wird die Schleife beendet
                    receivedMsg = true;
                }
            }
        }
        monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.ACK); //Zustand des Partizipanten wird auf ACK gesetzt
        writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDP);

    }

    private void ackGlobalMsg() { //Methode für den Zustand ACK des ParticipantThreads
        System.out.println("Ich bin im ack");
        sendMsgCoordinator(" ACK"); //ACK wird an den Koordinator gesendet
        monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.Finish); //Zustand des Partizipanten wird auf FINISH gesetzt
        writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDP);
    }




    private boolean checkDatabase() { //Methode um in Datenbank zu checken ob man buchen könnte und ggfs zu reservieren
        TransactionParticipant participantTransaction = monitorDataPaPaThread.getTransaction(uuid);
        if(this.type.equals("Hotel")){
            //Datenbank hotel checken
            return databaseHotel.reserveRoom(participantTransaction.rooms,participantTransaction.fromDate,participantTransaction.toDate, String.valueOf(uuid)); //gibt true zurück wenn man Zimmer reservieren kann
        }else if(this.type.equals("Autoverleih")){
            //Datenbank autoverleih checken
            return databaseAutoverleih.reserveCar(participantTransaction.rooms,participantTransaction.fromDate,participantTransaction.toDate, String.valueOf(uuid)); //gibt true zurück wenn man Auto reservieren kann
        }

        return false;
    }

    private void bookRoomCar() { //Methode für den Zustand COMMIT des ParticipantThreads um Zimmer/Auto endgültig zu buchen
        TransactionParticipant participantTransaction = monitorDataPaPaThread.getTransaction(uuid);
        if(this.type.equals("Hotel")){
            databaseHotel.bookReservedRooms(participantTransaction.fromDate,participantTransaction.toDate, String.valueOf(uuid)); //Zimmer wird gebucht
        }else if(this.type.equals("Autoverleih")){
            databaseAutoverleih.bookReservedCars(participantTransaction.fromDate,participantTransaction.toDate, String.valueOf(uuid)); //Auto wird gebucht
        }

        System.out.println("Ich bin im Commit");
        monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.ACK); //Zustand des Partizipanten wird auf ACK gesetzt
        writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDP);
    }

    private void sendMsgCoordinator(String message){ //Methode um Nachrichten an den Koordinator zu senden
        try {
            byte[] tempSendData = (this.uuid + message).getBytes();
            DatagramPacket dp = new DatagramPacket(tempSendData, tempSendData.length, this.tempDP.getAddress(), this.tempDP.getPort());
            this.socket.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendMsgParticipant(String message, InetAddress address, int port){ //Methode um Nachrichten an den anderen Partizipanten zu senden
        try {
            byte[] tempSendData = (this.uuid + message).getBytes();
            DatagramPacket dp = new DatagramPacket(tempSendData, tempSendData.length, address, port);
            this.socket.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}




