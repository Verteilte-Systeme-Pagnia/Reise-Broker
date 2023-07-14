package fehler.fehlerfall2.java.logic;

import fehler.fehlerfall2.java.logic.transaction.TransactionCoordinator;
import fehler.fehlerfall2.java.logic.transaction.WriteLogFile;
import fehler.fehlerfall2.java.logic.transaction.states_coordinator;
import fehler.fehlerfall2.java.logic.transaction.states_participant;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.UUID;

import static fehler.fehlerfall2.java.logic.transaction.states_coordinator.Finish;

public class CoordinatorThread extends Thread{
    private MonitorDataCoCoThread monitorDataCoCoThread;
    private WriteLogFile writeLogFileMonitor;
    private ArrayList<ParticipantRef> participantsAllThreadUse; // Liste der Teilnehmer
    private ArrayList<ParticipantRef> participantsSingleThread; //
    private final DatagramSocket socket;
    private final UUID uuid;
    private DatagramPacket tempDP;

    public CoordinatorThread(UUID uuid, MonitorDataCoCoThread monitorDataCoCoThread, WriteLogFile writeLogFileMonitor, ArrayList<ParticipantRef> participantsAllThreadUse, DatagramSocket socket){
        this.monitorDataCoCoThread = monitorDataCoCoThread;
        this.uuid = uuid;
        this.writeLogFileMonitor = writeLogFileMonitor;
        this.participantsAllThreadUse = participantsAllThreadUse;
        this.socket = socket;

        this.participantsSingleThread = new ArrayList<>();
        for(ParticipantRef participant : participantsAllThreadUse){
            this.participantsSingleThread.add(new ParticipantRef(participant.getAddress(),participant.getPort()));
        }
    }
    public void run() {
        initialize();
    } //die initialize Methode wird bei jedem Thread aufgerufen

    private void initialize() { //In der Methode wird geschaut, welchen Zustand der CoordinatorThread zurzeit hat und dementsprechend verschiedene Methoden ausgeführt
        while (this.monitorDataCoCoThread.getTransaction(uuid).getStateC() != Finish)
        { //wenn der Thread noch nicht fertig ist läuft die Abfrage, welchen Zustand der CoordinatorThread zurzeit hat
            switch (this.monitorDataCoCoThread.getTransaction(uuid).getStateC()) { //Abfrage des Zustands des CoordinatorThreads über den Monitor
                case INIT:
                    stateInit();
                    break;
                case WAIT:
                    stateWait();
                    break;
                case ABORT:
                    sendGlobalAbort();
                    receiveAck();
                    break;
                case COMMIT:
                    sendGlobalCommit();
                    receiveAck();
                    break;
                case SENDCLIENT:
                    sendClientMessage();
                    break;
                case Finish:
            }
        }
        if(this.monitorDataCoCoThread.getTransaction(this.uuid) != null){//Rm Transaction when finished
            this.monitorDataCoCoThread.rmTranscaction(this.uuid);
        }
    }

    private void stateInit(){ //Methode für den initialen Zustand des CoordinatorThreads
        monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.INIT); //Der Zustand des CoordinatorThreads wird auf INIT gesetzt
        System.out.println("CoordinatorThread "+uuid+ " Festlegen des INIT Zustands");
        writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid), this.monitorDataCoCoThread.getTransaction(this.uuid).getDatagramPacket()); //Der aktuelle Zustand und die anderen wichtigen Daten werden in die Log-File geschrieben
        monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.WAIT); //Der Zustand des CoordinatorThreads wird auf WAIT gesetzt
        System.out.println("CoordinatorThread "+uuid+ " Festlegen des WAIT Zustands");
        writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid), this.monitorDataCoCoThread.getTransaction(this.uuid).getDatagramPacket());
    }
    private void stateWait(){ //Methode im Wartezustand des CoordinatorThreads
        this.participantsAllThreadUse.stream() //An alle Partizipanten aus dieser Transaktion werden VOTE_REQUESTS gesendet
            .forEach(participantRef -> sendMsgParticipant(this.uuid.toString() + " VOTE_REQUEST",participantRef));
        System.out.println("CoordinatorThread "+uuid+ " VOTE_REQUESTS an Partizipanten versendet");
        // Warte auf eingehende Stimmen
        long startTime = System.nanoTime();
        long endTime = startTime + (15 * 1000000000L); // 15 Sekunden in Nanosekunden umrechnen
        boolean allReceived = false;
        for(ParticipantRef participantRef : participantsSingleThread){
            participantRef.setStateP(states_participant.INIT); //In der Referenzklasse wird der Zustand der Partizipanten auf INIT gesetzt
        }


        while(System.nanoTime() < endTime && (!allReceived)){ //solange die bestimmte Zeit noch nicht vergangen sind und noch nicht alle Nachrichten von den Partizipanten angekommen sind
            this.tempDP = this.monitorDataCoCoThread.getTransaction(uuid).getDatagramPacket(); //Das DatagramPacket wird geholt, falls etwas angekommen ist, wurde es dort abgelegt
            if(this.tempDP == null){
                //nichts Neues angekommen
            }else{
                this.monitorDataCoCoThread.getTransaction(uuid).setDatagramPacket(null); //Da wo das DatagramPacket geholt wurde, wird es auf null gesetzt, damit der Platz für neue DatagramPackets frei wird
                String msg[] = new String(this.tempDP.getData(), 0, this.tempDP.getLength()).split(" "); //Die Nachricht aus dem DatagramPacket wird ausgelesen
                participantsSingleThread.stream() //Zuordnung der Nachricht zu den Partizipanten
                        .filter(participantRef -> participantRef.getAddress().equals(this.tempDP.getAddress()) && participantRef.getPort() == this.tempDP.getPort()) //überprüfung, ob das ausgelesene DatagramPacket vom Partizipanten kommt
                        .forEach(participantRef -> {
                            if ("VOTE_COMMIT".equals(msg[1])) {
                                participantRef.setStateP(states_participant.COMMIT); //falls VOTE_COMMIT wird der Zustand des Partizipanten in der Referenzklasse auf COMMIT gesetzt
                                System.out.println("CoordinatorThread "+uuid+ " Partizipant: "+participantRef.getAddress()+" "+participantRef.getPort()+" hat COMMIT gesendet");                           
                            } else if ("VOTE_ABORT".equals(msg[1])) {
                                participantRef.setStateP(states_participant.ABORT); //falls VOTE_ABORT wird der Zustand des Partizipanten in der Referenzklasse auf ABORT gesetzt
                                System.out.println("CoordinatorThread "+uuid+ " Partizipant: "+participantRef.getAddress()+" "+participantRef.getPort()+" hat ABORT gesendet");                           

                            }
                        });
                allReceived = participantsSingleThread.stream().allMatch(participantRef -> participantRef.getStateP() != states_participant.INIT); //prüfe ob Nachrichten von allen Partizipanten empfangen wurden und ein Zustand gesetzt wurde
            }
        }
        if(allReceived && (participantsSingleThread.stream().allMatch(participantRef -> participantRef.getStateP().equals(states_participant.COMMIT)))){
            this.monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.COMMIT); //wenn alle Partizipanten mit VOTE_COMMIT geantwortet haben, wird ein der Zustand des Koordinators auf Commit gesetzt
            System.out.println("CoordinatorThread "+uuid+ " Alle Partizipanten haben mit Commit geantwortet, festsetzen des Status auf COMMIT");                           

            this.writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid), this.tempDP);

        } else{
            this.monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.ABORT); //wenn mindestens ein Partizipant mit VOTE_ABORT geantwortet hat, wird ein der Zustand des Koordinators auf ABORT gesetzt
            System.out.println("CoordinatorThread "+uuid+ " Mindestens ein Partizipant hat mit ABORT oder nicht in der gesetzen Zeit geantwortet, festsetzen des Status auf ABORT");


            this.writeLogFileMonitor.writeToFileExtra(monitorDataCoCoThread.getTransaction(this.uuid));
            }
        }

    private void sendGlobalCommit(){ //Methode für den COMMIT Zustand des CoordinatorThreads
        this.participantsAllThreadUse.stream()
            .forEach(participantRef -> {sendMsgParticipant(this.uuid.toString() + " GLOBAL_COMMIT",participantRef); //Im COMMIT wird ein GLOBAL_COMMIT an alle Partizipanten geschickt
            });
        System.out.println("CoordinatorThread "+uuid+ " Sendet GLOBAL_COMMIT an alle Partizipanten");                           

    }

    private void sendGlobalAbort(){ //Methode für den ABORT Zustand des CoordinatorThreads
        this.participantsAllThreadUse.stream()
            .forEach(participantRef -> sendMsgParticipant(this.uuid.toString() + " GLOBAL_ABORT",participantRef)); //Im ABORT wird ein GLOBAL_ABORT an alle Partizipanten geschickt
        System.out.println("CoordinatorThread "+uuid+ " Sendet GLOBAL_ABORT an alle Partizipanten");                           
    }

    private void receiveAck(){ //Methode wird sowohl im COMMIT, als auch im ABORT Zustand ausgeführt
        final boolean finalSendCommit = monitorDataCoCoThread.getTransaction(uuid).getStateC().equals(states_coordinator.COMMIT); //letzter Zustand des Koordinators
        long startTime = System.nanoTime();
        long endTime = startTime + (15 * 1000000000L); // 15 Sekunden in Nanosekunden umrechnen
        while(!(participantsSingleThread.stream().allMatch(participantRef -> participantRef.getStateP().equals(states_participant.ACK)))){ //solange man nock kein ACK von allen Partizipanten bekommen hat
            if(System.nanoTime() < endTime){ //Warten für eine bestimmte Zeit, um alle ACKs zu bekommen
                DatagramPacket tempDatagramPacket = this.monitorDataCoCoThread.getTransaction(uuid).getDatagramPacket();
                if(tempDatagramPacket == null){
                    //nichts Neues angekommen
                }else{
                    this.monitorDataCoCoThread.getTransaction(uuid).setDatagramPacket(null);
                    String msg[] = new String(tempDatagramPacket.getData(), 0, tempDatagramPacket.getLength()).split(" "); //Auslesen des DatagramPackets
                    if(msg[1].equals("ACK")){ //falls Nachricht ACK
                        participantsSingleThread.stream()
                                .filter(participantRef -> participantRef.getAddress().equals(tempDatagramPacket.getAddress()) && participantRef.getPort() == tempDatagramPacket.getPort())
                                .forEach(participantRef -> participantRef.setStateP(states_participant.ACK)); //Zustand in der Referenzklasse der Partizipanten wird auf ACK gesetzt
                        System.out.println("CoordinatorThread "+this.uuid+" habe eine ACK erhalten");
                    }else{
                        //keine erwartete Nachricht
                    }
                }
            }else{ //falls die Zeit, um auf die ACKs zu warten, abgelaufen ist
                participantsSingleThread.stream()
                        .filter(participantRef -> !participantRef.getStateP().equals(states_participant.ACK))
                        .forEach(participantRef -> {if(finalSendCommit){ //falls der letzte Zustand des CoordinatorThreads COMMIT war, wird erneut ein GLOBAL_COMMIT gesendet
                            sendMsgParticipant(this.uuid.toString() + " GLOBAL_COMMIT",participantRef);
                        }else{  //falls der letzte Zustand des CoordinatorThreads nicht COMMIT war, wird erneut ein GLOBAL_ABORT gesendet
                            sendMsgParticipant(this.uuid.toString() + " GLOBAL_ABORT",participantRef);
                        }
                        });
                System.out.println("CoordinatorThread "+this.uuid+" nicht alle ACK Nachrichten eingetroffen, Nachrichten wurden erneut versendet");

                startTime = System.nanoTime();
                endTime = startTime + (15 * 1000000000L); //Zeit wird neu gesetzt
            }
        }
        System.out.println("CoordinatorThread "+this.uuid+" habe alle ACK erhalten");

        monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.SENDCLIENT); //wenn alle Partizipanten ein ACK gesendet haben, wird der Zustand des Koordinators auf SENDCLIENT gesetzt
        System.out.println("CoordinatorThread "+this.uuid+" Status auf SendCleint gesetzt");

        monitorDataCoCoThread.getTransaction(uuid).finalResult = String.valueOf(finalSendCommit);
        writeLogFileMonitor.writeToFileFinalResult(monitorDataCoCoThread.getTransaction(this.uuid), String.valueOf(finalSendCommit)); //Zusätzliche Notation des letzten Zustandes in der Log-File, um beim Absturz zu wissen, ob man im COMMIT oder ABORT war
    }
    private void sendClientMessage(){ //Methode für den SENDCLIENT Zustand des CoordinatorThreads
        byte[] tempSendData;
        if(this.monitorDataCoCoThread.getTransaction(uuid).finalResult.equals(String.valueOf(true))) { //falls man im COMMIT war
            tempSendData = "Successfully-Booked".getBytes();
        }else{ //falls man im ABORT war
            tempSendData = "Booking-Error".getBytes();
        }
        try{
            DatagramPacket dp = new DatagramPacket(tempSendData, tempSendData.length, this.monitorDataCoCoThread.getTransaction(uuid).senderReference.getSenderAddress(), this.monitorDataCoCoThread.getTransaction(uuid).senderReference.getSenderPort());
            socket.send(dp);
            System.out.println("CoordinatorThread "+this.uuid+" Nachricht an fehler.fehlerfall2.java.Client gesendet");

            this.monitorDataCoCoThread.setTransactionStatus(uuid, Finish); //Zustand des Koordinators wird auf FINISH gesetzt
            System.out.println("CoordinatorThread "+this.uuid+" Status auf FINISH gesetzt");
            this.writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid),dp);
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void sendMsgParticipant(String message, ParticipantRef participantRef){ //Methode um Nachrichten an die Partizipanten zu senden
        try {
            TransactionCoordinator tempTransaction = this.monitorDataCoCoThread.getTransaction(uuid);
            message = message + " "+ tempTransaction.rooms + " "+tempTransaction.autos+ " "+tempTransaction.fromDate+ " "+tempTransaction.toDate;
            byte[] tempSendData = message.getBytes();
            DatagramPacket dp = new DatagramPacket(tempSendData, tempSendData.length, participantRef.getAddress(), participantRef.getPort());
            this.socket.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
