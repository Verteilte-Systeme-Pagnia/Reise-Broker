// Package not detected, please report project structure on CodeTogether's GitHub Issues


import transaction.*;

import java.io.*;
import java.net.*;
import java.util.*;

import static transaction.states_coordinator.Finish;


public class CoordinatorThread extends Thread{
    private MonitorDataCoCoThread monitorDataCoCoThread;
    private WriteLogFile writeLogFileMonitor;
    private ArrayList<ParticipantRef> participantsAllThreadUse; // Liste der Teilnehmer
    private ArrayList<ParticipantRef> participantsSingleThread;
    private final DatagramSocket socket;
    private final UUID uuid;

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
        writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid));

        // Sende VOTE_REQUEST als Multicast an alle Teilnehmer
        monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.WAIT);
        writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid));
    }
    private void stateWait(){
        this.participantsAllThreadUse.stream()
            .forEach(participantRef -> sendMsgParticipant(this.uuid.toString() + " VOTE_REQUEST",participantRef));
        // Warte auf eingehende Stimmen
        long startTime = System.nanoTime();
        long endTime = startTime + (5 * 1000000000L); // 5 Sekunden in Nanosekunden umrechnen
        boolean allReceived = false;
        for(ParticipantRef participantRef : participantsSingleThread){
            participantRef.setStateP(states_participant.INIT);
        }

        while(System.nanoTime() < endTime && (!allReceived)){
            DatagramPacket tempDatagramPacket = this.monitorDataCoCoThread.getTransaction(uuid).getDatagramPacket();
            if(tempDatagramPacket == null){
                //nichts Neues angekommen
            }else{
                this.monitorDataCoCoThread.getTransaction(uuid).setDatagramPacket(null);
                String msg[] = new String(tempDatagramPacket.getData(), 0, tempDatagramPacket.getLength()).split(" "); //msg UUID Command Content
                participantsSingleThread.stream()
                        .filter(participantRef -> participantRef.getAddress().equals(tempDatagramPacket.getAddress()) && participantRef.getPort() == tempDatagramPacket.getPort())
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
            if(allReceived && (participantsSingleThread.stream().allMatch(participantRef -> participantRef.getStateP().equals(states_participant.COMMIT)))){
                monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.COMMIT);
                writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid));

            } else{
                monitorDataCoCoThread.setTransactionStatus(this.uuid, states_coordinator.ABORT);
                writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid));

            }
    }

    private void sendGlobalCommit(){
        this.participantsAllThreadUse.stream()
            .forEach(participantRef -> sendMsgParticipant(this.uuid.toString() + " GLOBAL_COMMIT",participantRef));
    }

    private void sendGlobalAbort(){
        this.participantsAllThreadUse.stream()
            .forEach(participantRef -> sendMsgParticipant(this.uuid.toString() + " GLOBAL_ABORT",participantRef));
    }

    private void receiveAck(){
        final boolean finalSendCommit = monitorDataCoCoThread.getTransaction(uuid).getStateC().equals(states_coordinator.COMMIT);
        //prüfe ob alle etwas erhalten haben und sende gegebenenfalls nachricht bis sie es haben...
        long startTime = System.nanoTime();
        long endTime = startTime + (5 * 1000000000L); // 5 Sekunden in Nanosekunden umrechnen
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
                        }else{
                            sendMsgParticipant(this.uuid.toString() + " GLOBAL_ABORT",participantRef);
                        }
                        });
                startTime = System.nanoTime();
                endTime = startTime + (5 * 1000000000L);
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
            DatagramPacket dp = new DatagramPacket(tempSendData, tempSendData.length, this.monitorDataCoCoThread.getTransaction(uuid).clientReference.getClientAddress(), this.monitorDataCoCoThread.getTransaction(uuid).clientReference.getClientPort());
            socket.send(dp);
            this.monitorDataCoCoThread.setTransactionStatus(uuid, Finish);
            this.writeLogFileMonitor.writeToFile(monitorDataCoCoThread.getTransaction(this.uuid));
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
