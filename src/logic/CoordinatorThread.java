package logic;// Package not detected, please report project structure on CodeTogether's GitHub Issues


import logic.transaction.*;

import java.io.*;
import java.net.*;
import java.util.*;

import static logic.transaction.states_coordinator.*;


public class CoordinatorThread extends Thread{
    private MonitorDataCoCoThread monitorDataCoCoThread;
    private WriteLogFile writeLogFileMonitor;
    private ArrayList<ParticipantRef> participantsAllThreadUse; // Liste der Teilnehmer
    private ArrayList<ParticipantRef> participantsSingleThread;
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
    }

    private void initialize() {
        while (this.monitorDataCoCoThread.getTransaction(uuid).getStateC() != Finish) {
            switch (this.monitorDataCoCoThread.getTransaction(uuid).getStateC()) {
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

    private void stateInit(){
        monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.INIT);
        writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid), this.monitorDataCoCoThread.getTransaction(this.uuid).getDatagramPacket());

        // Sende VOTE_REQUEST als Multicast an alle Teilnehmer
        monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.WAIT);
        writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid), this.monitorDataCoCoThread.getTransaction(this.uuid).getDatagramPacket());
    }
    private void stateWait(){
        this.participantsAllThreadUse.stream()
            .forEach(participantRef -> sendMsgParticipant(this.uuid.toString() + " VOTE_REQUEST",participantRef));
        // Warte auf eingehende Stimmen
        long startTime = System.nanoTime();
        long endTime = startTime + (15 * 1000000000L); // 15 Sekunden in Nanosekunden umrechnen
        boolean allReceived = false;
        for(ParticipantRef participantRef : participantsSingleThread){
            participantRef.setStateP(states_participant.INIT);
        }


        while(System.nanoTime() < endTime && (!allReceived)){
            this.tempDP = this.monitorDataCoCoThread.getTransaction(uuid).getDatagramPacket();
            if(this.tempDP == null){
                //nichts Neues angekommen
            }else{
                System.out.println("ich habe kein null datapcket bekommen in wait():");
                this.monitorDataCoCoThread.getTransaction(uuid).setDatagramPacket(null);
                String msg[] = new String(this.tempDP.getData(), 0, this.tempDP.getLength()).split(" "); //msg UUID Command Content
                participantsSingleThread.stream()
                        .filter(participantRef -> participantRef.getAddress().equals(this.tempDP.getAddress()) && participantRef.getPort() == this.tempDP.getPort())
                        .forEach(participantRef -> {
                            if ("VOTE_COMMIT".equals(msg[1])) {
                                participantRef.setStateP(states_participant.COMMIT);
                            } else if ("VOTE_ABORT".equals(msg[1])) {
                                participantRef.setStateP(states_participant.ABORT);
                            }
                        });
                allReceived = participantsSingleThread.stream().allMatch(participantRef -> participantRef.getStateP() != states_participant.INIT); //prüfe ob alle etwas empfangen haben und einen status gesetzt haben
            }
        }

            System.out.println("Evaluiere wait ob commit oder abbort: "+ (allReceived && (participantsSingleThread.stream().allMatch(participantRef -> participantRef.getStateP().equals(states_participant.COMMIT)))));
            System.out.println("Evaluiere wait ob commit oder abbort allreceived: "+allReceived);
            System.out.println("Evaluiere wait ob commit oder abbort allreceived: "+ participantsSingleThread.stream().allMatch(participantRef -> participantRef.getStateP().equals(states_participant.COMMIT)));

            if(allReceived && (participantsSingleThread.stream().allMatch(participantRef -> participantRef.getStateP().equals(states_participant.COMMIT)))){
                this.monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.COMMIT);
                System.out.println("Setze mich auf Commit");

                this.writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid), this.tempDP);

            } else{
                this.monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.ABORT);
                System.out.println("Setze mich auf Abort");
                this.writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid), this.tempDP);

            }
    }

    private void sendGlobalCommit(){
        this.participantsAllThreadUse.stream()
            .forEach(participantRef -> {sendMsgParticipant(this.uuid.toString() + " GLOBAL_COMMIT",participantRef);
                                            //try {
                                                //this.sleep(20000); //timeout für koordinator fällt aus
                                            //} catch (InterruptedException e) {
                                            //    throw new RuntimeException(e);
                                            //}
                                        });
    }

    private void sendGlobalAbort(){
        this.participantsAllThreadUse.stream()
            .forEach(participantRef -> sendMsgParticipant(this.uuid.toString() + " GLOBAL_ABORT",participantRef));
    }

    private void receiveAck(){
        final boolean finalSendCommit = monitorDataCoCoThread.getTransaction(uuid).getStateC().equals(states_coordinator.COMMIT);
        //prüfe ob alle etwas erhalten haben und sende gegebenenfalls nachricht bis sie es haben...
        long startTime = System.nanoTime();
        long endTime = startTime + (15 * 1000000000L); // 5 Sekunden in Nanosekunden umrechnen
        while(!(participantsSingleThread.stream().allMatch(participantRef -> participantRef.getStateP().equals(states_participant.ACK)))){
            if(System.nanoTime() < endTime){
                DatagramPacket tempDatagramPacket = this.monitorDataCoCoThread.getTransaction(uuid).getDatagramPacket();
                if(tempDatagramPacket == null){
                    //nichts Neues angekommen
                }else{
                    this.monitorDataCoCoThread.getTransaction(uuid).setDatagramPacket(null);
                    String msg[] = new String(tempDatagramPacket.getData(), 0, tempDatagramPacket.getLength()).split(" "); //msg UUID Command Content --> Command ACK
                    if(msg[1].equals("ACK")){
                        participantsSingleThread.stream()
                                .filter(participantRef -> participantRef.getAddress().equals(tempDatagramPacket.getAddress()) && participantRef.getPort() == tempDatagramPacket.getPort())
                                .forEach(participantRef -> participantRef.setStateP(states_participant.ACK));
                    }else{
                        //keine erwartete nachricht
                    }
                }
            }else{
                participantsSingleThread.stream()
                        .filter(participantRef -> !participantRef.getStateP().equals(states_participant.ACK))
                        .forEach(participantRef -> {if(finalSendCommit){
                            sendMsgParticipant(this.uuid.toString() + " GLOBAL_COMMIT",participantRef);
                            System.out.println("ich habe ein lgobal commit gesendet");
                        }else{
                            sendMsgParticipant(this.uuid.toString() + " GLOBAL_ABORT",participantRef);
                        }
                        });
                startTime = System.nanoTime();
                endTime = startTime + (15 * 1000000000L);
            }
        }

        monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.SENDCLIENT);
        monitorDataCoCoThread.getTransaction(uuid).finalResult = String.valueOf(finalSendCommit);
        writeLogFileMonitor.writeToFileFinalResult(monitorDataCoCoThread.getTransaction(this.uuid), String.valueOf(finalSendCommit));
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        sendClientMessage();
    }
    private void sendClientMessage(){
        byte[] tempSendData;
        if(this.monitorDataCoCoThread.getTransaction(uuid).finalResult.equals(String.valueOf(true))) {
            tempSendData = "Successfully-Booked".getBytes();
        }else{
            tempSendData = "Booking-Error".getBytes();
        }
        try{
            DatagramPacket dp = new DatagramPacket(tempSendData, tempSendData.length, this.monitorDataCoCoThread.getTransaction(uuid).senderReference.getSenderAddress(), this.monitorDataCoCoThread.getTransaction(uuid).senderReference.getSenderPort());
            socket.send(dp);
            this.monitorDataCoCoThread.setTransactionStatus(uuid, Finish);
            this.writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid),dp);
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void sendMsgParticipant(String message, ParticipantRef participantRef){
        try {
            byte[] tempSendData = message.getBytes();
            DatagramPacket dp = new DatagramPacket(tempSendData, tempSendData.length, participantRef.getAddress(), participantRef.getPort());
            this.socket.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
