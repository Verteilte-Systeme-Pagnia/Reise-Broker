package logic;

import logic.transaction.*;

import java.io.*;
import java.net.*;
import java.util.*;

import static logic.transaction.states_coordinator.*;

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
        while (this.monitorDataCoCoThread.getTransaction(uuid).getStateC() != Finish) { //wenn der Thread noch nicht fertig ist läuft die Abfrage, welchen Zustand der CoordinatorThread zurzeit hat
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
    }

    private void stateInit(){ //Methode für den initialen Zustand des CoordinatorThreads
        monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.INIT); //Der Zustand des CoordinatorThreads wird auf INIT gesetzt
        writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid), this.monitorDataCoCoThread.getTransaction(this.uuid).getDatagramPacket()); //Der aktuelle Zustand und die anderen wichtigen Daten werden in die Log-File geschrieben

        monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.WAIT); //Der Zustand des CoordinatorThreads wird auf WAIT gesetzt
        writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid), this.monitorDataCoCoThread.getTransaction(this.uuid).getDatagramPacket());
    }
    private void stateWait(){ //Methode im Wartezustand des CoordinatorThreads
        this.participantsAllThreadUse.stream() //An alle Partizipanten aus dieser Transaktion werden VOTE_REQUESTS gesendet
            .forEach(participantRef -> sendMsgParticipant(this.uuid.toString() + " VOTE_REQUEST",participantRef));
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
                System.out.println("ich habe kein null datapacket bekommen in wait():");
                this.monitorDataCoCoThread.getTransaction(uuid).setDatagramPacket(null); //Da wo das DatagramPacket geholt wurde, wird es auf null gesetzt, damit der Platz für neue DatagramPackets frei wird
                String msg[] = new String(this.tempDP.getData(), 0, this.tempDP.getLength()).split(" "); //Die Nachricht aus dem DatagramPacket wird ausgelesen
                participantsSingleThread.stream() //Zuordnung der Nachricht zu den Partizipanten
                        .filter(participantRef -> participantRef.getAddress().equals(this.tempDP.getAddress()) && participantRef.getPort() == this.tempDP.getPort()) //überprüfung, ob das ausgelesene DatagramPacket vom Partizipanten kommt
                        .forEach(participantRef -> {
                            if ("VOTE_COMMIT".equals(msg[1])) {
                                participantRef.setStateP(states_participant.COMMIT); //falls VOTE_COMMIT wird der Zustand des Partizipanten in der Referenzklasse auf COMMIT gesetzt
                            } else if ("VOTE_ABORT".equals(msg[1])) {
                                participantRef.setStateP(states_participant.ABORT); //falls VOTE_ABORT wird der Zustand des Partizipanten in der Referenzklasse auf ABORT gesetzt
                            }
                        });
                allReceived = participantsSingleThread.stream().allMatch(participantRef -> participantRef.getStateP() != states_participant.INIT); //prüfe ob Nachrichten von allen Partizipanten empfangen wurden und ein Zustand gesetzt wurde
            }
        }

            System.out.println("Evaluiere wait ob commit oder abort: "+ (allReceived && (participantsSingleThread.stream().allMatch(participantRef -> participantRef.getStateP().equals(states_participant.COMMIT)))));
            System.out.println("Evaluiere wait ob commit oder abort allreceived: "+allReceived);
            System.out.println("Evaluiere wait ob commit oder abort allreceived: "+ participantsSingleThread.stream().allMatch(participantRef -> participantRef.getStateP().equals(states_participant.COMMIT)));

            if(allReceived && (participantsSingleThread.stream().allMatch(participantRef -> participantRef.getStateP().equals(states_participant.COMMIT)))){
                this.monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.COMMIT); //wenn alle Partizipanten mit VOTE_COMMIT geantwortet haben, wird ein der Zustand des Koordinators auf Commit gesetzt
                System.out.println("Setze mich auf Commit");

                this.writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid), this.tempDP);

            } else{
                this.monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.ABORT); //wenn mindestens ein Partizipant mit VOTE_ABORT geantwortet hat, wird ein der Zustand des Koordinators auf ABORT gesetzt
                System.out.println("Setze mich auf Abort");
                this.writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid), this.tempDP);

            }
    }

    private void sendGlobalCommit(){ //Methode für den COMMIT Zustand des CoordinatorThreads
        this.participantsAllThreadUse.stream()
            .forEach(participantRef -> {sendMsgParticipant(this.uuid.toString() + " GLOBAL_COMMIT",participantRef); //Im COMMIT wird ein GLOBAL_COMMIT an alle Partizipanten geschickt
                                            //try {
                                                //this.sleep(20000); //timeout für koordinator fällt aus
                                            //} catch (InterruptedException e) {
                                            //    throw new RuntimeException(e);
                                            //}
                                        });
    }

    private void sendGlobalAbort(){ //Methode für den ABORT Zustand des CoordinatorThreads
        this.participantsAllThreadUse.stream()
            .forEach(participantRef -> sendMsgParticipant(this.uuid.toString() + " GLOBAL_ABORT",participantRef)); //Im ABORT wird ein GLOBAL_ABORT an alle Partizipanten geschickt
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
                    }else{
                        //keine erwartete Nachricht
                    }
                }
            }else{ //falls die Zeit, um auf die ACKs zu warten, abgelaufen ist
                participantsSingleThread.stream()
                        .filter(participantRef -> !participantRef.getStateP().equals(states_participant.ACK))
                        .forEach(participantRef -> {if(finalSendCommit){ //falls der letzte Zustand des CoordinatorThreads COMMIT war, wird erneut ein GLOBAL_COMMIT gesendet
                            sendMsgParticipant(this.uuid.toString() + " GLOBAL_COMMIT",participantRef);
                            System.out.println("ich habe ein global commit gesendet");
                        }else{  //falls der letzte Zustand des CoordinatorThreads nicht COMMIT war, wird erneut ein GLOBAL_ABORT gesendet
                            sendMsgParticipant(this.uuid.toString() + " GLOBAL_ABORT",participantRef);
                        }
                        });
                startTime = System.nanoTime();
                endTime = startTime + (15 * 1000000000L); //Zeit wird neu gesetzt
            }
        }

        monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.SENDCLIENT); //wenn alle Partizipanten ein ACK gesendet haben, wird der Zustand des Koordinators auf SENDCLIENT gesetzt
        monitorDataCoCoThread.getTransaction(uuid).finalResult = String.valueOf(finalSendCommit);
        writeLogFileMonitor.writeToFileFinalResult(monitorDataCoCoThread.getTransaction(this.uuid), String.valueOf(finalSendCommit)); //Zusätzliche Notation des letzten Zustandes in der Log-File, um beim Absturz zu wissen, ob man im COMMIT oder ABORT war
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        sendClientMessage();
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
            this.monitorDataCoCoThread.setTransactionStatus(uuid, Finish); //Zustand des Koordinators wird auf FINISH gesetzt
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
