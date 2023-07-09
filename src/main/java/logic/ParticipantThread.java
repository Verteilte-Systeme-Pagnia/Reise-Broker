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

    public ParticipantThread(UUID uuid, MonitorDataPaPaThread monitorDataPaPaThread, WriteLogFile writeLogFileMonitor, DatagramSocket socket,ArrayList<ParticipantRef> participantRefs, String type){
        this.decisionRequests = new LinkedBlockingQueue<>();
        this.uuid = uuid;
        this.monitorDataPaPaThread = monitorDataPaPaThread;
        this.writeLogFileMonitor = writeLogFileMonitor;
        this.socket = socket;
        this.participantRefs = participantRefs;
        this.type = type;
    }

    public void run() {
        initialize();
    }

    private void initialize() {
        while (this.monitorDataPaPaThread.getTransaction(uuid).getStateP() != states_participant.Finish) {
            switch (this.monitorDataPaPaThread.getTransaction(uuid).getStateP()) {
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

    private void stateInit() {
        writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid), monitorDataPaPaThread.getTransaction(this.uuid).getDatagramPacket());

        long startTime = System.nanoTime();
        long endTime = startTime + (5 * 1000000000L);
        boolean vote_request = false;
        while (System.nanoTime() < endTime && !vote_request) {
            DatagramPacket tempDatagramPacket = this.monitorDataPaPaThread.getTransaction(uuid).getDatagramPacket();
            if (tempDatagramPacket == null) {
                //nichts Neues angekommen
            } else {
                tempDP = new DatagramPacket(tempDatagramPacket.getData(),tempDatagramPacket.getLength(),tempDatagramPacket.getAddress(),tempDatagramPacket.getPort());

                this.monitorDataPaPaThread.getTransaction(uuid).setDatagramPacket(null);
                System.out.print(tempDP.getAddress());
                String msg[] = new String(tempDatagramPacket.getData(), 0, tempDatagramPacket.getLength()).split(" ");
                System.out.print("nachricht "+msg[1]);
                if ("VOTE_REQUEST".equals(msg[1])) {
                    vote_request = true;
                    System.out.print(vote_request);
                }
            }
        }


        if(vote_request) {
            if (checkDatabase()) {
                monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.READY);
                writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDP);
            } else {
                monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.ABORT);
                writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDP);
            }
        }
        else {
            monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.ABORT);
            writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDP);
        }
         System.out.print("amende"+tempDP.getAddress());

    }

    private void stateReady() {
        sendMsgCoordinator(" VOTE_COMMIT");

        long startTime = System.nanoTime();
        long endTime = startTime + (15 * 1000000000L);
        boolean receivedMsg = false;
        boolean expectParticipantToo = false;

        while (!receivedMsg) {
            DatagramPacket tempDatagramPacket = this.monitorDataPaPaThread.getTransaction(uuid).getDatagramPacket();
            if(System.nanoTime() > endTime){
                this.participantRefs.stream().forEach(participantRef ->  {sendMsgParticipant(" DESICION_REQUEST",participantRef.getAddress(),participantRef.getPort());});
                expectParticipantToo = true;
                 startTime = System.nanoTime();
                 endTime = startTime + (15 * 1000000000L);
            }
            if (tempDatagramPacket == null) {
                //nichts Neues angekommen
            } else {
                if(!expectParticipantToo){
                    this.monitorDataPaPaThread.getTransaction(uuid).setDatagramPacket(null);
                    String msg[] = new String(tempDatagramPacket.getData(), 0, tempDatagramPacket.getLength()).split(" ");
                    if ("GLOBAL_COMMIT".equals(msg[1])) {
                        monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.COMMIT);
                        writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDatagramPacket);
                        receivedMsg = true;
                    } else if ("GLOBAL_ABORT".equals(msg[1])) {
                        monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.ACK);
                        writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDatagramPacket);
                        receivedMsg = true;
                    }
                }else{
                    //Koordinator und Partizipant wird erwartet
                    this.monitorDataPaPaThread.getTransaction(uuid).setDatagramPacket(null);
                    String msg[] = new String(tempDatagramPacket.getData(), 0, tempDatagramPacket.getLength()).split(" ");
                    if ("GLOBAL_COMMIT".equals(msg[1])) {
                        monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.COMMIT);
                        writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDatagramPacket);
                        receivedMsg = true;
                    }else if ("GLOBAL_ABORT".equals(msg[1]) || "INIT".equals(msg[1])) {
                         monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.ACK);
                         writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDatagramPacket);
                         receivedMsg = true;
                    }
                }
            }
        }

    }

    private void doAbort() {
        sendMsgCoordinator(" VOTE_ABORT");
        System.out.println("ABOOOOOOOOOOOOOOORT");
        boolean receivedMsg = false;

        while(!receivedMsg){
            tempDP = this.monitorDataPaPaThread.getTransaction(uuid).getDatagramPacket();
            if(tempDP == null){

            }else{
                this.monitorDataPaPaThread.getTransaction(uuid).setDatagramPacket(null);
                String[] msg = new String(tempDP.getData(),0,tempDP.getLength()).split(" ");
                if(msg[1].equals("GLOBAL_ABORT")){
                    receivedMsg = true;
                }
            }
        }
        monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.ACK);
        writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDP);

    }

    private void ackGlobalMsg() {
        System.out.println("Ich bin im ack");
        sendMsgCoordinator(" ACK");
        monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.Finish);
        writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDP);
    }




    private boolean checkDatabase() {
        // Logik um in datenbank zu checken ob etwas da ist und ggfs zu reservieren

        if(this.type.equals("Hotel")){
            //Datenbank hotel checken
            return true;
        }else if(this.type.equals("Autoverleih")){
            //Datenbank autoverleih checken
            return false;
        }

        return false;
    }

    private void bookRoomCar() {
        // Logik um Mietwagen/hotelzimmer aus der datenbank zu nehmen

        if(this.type.equals("Hotel")){
            //Datenbank hotel buchen
        }else if(this.type.equals("Autoverleih")){
            //Datenbank auto buchen
        }

        System.out.println("Ich bin im Commit");
        monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.ACK);
        writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid),tempDP);
    }

    private void sendMsgCoordinator(String message){
        try {
            byte[] tempSendData = (this.uuid + message).getBytes();
            DatagramPacket dp = new DatagramPacket(tempSendData, tempSendData.length, this.tempDP.getAddress(), this.tempDP.getPort());
            this.socket.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendMsgParticipant(String message, InetAddress address, int port){
        try {
            byte[] tempSendData = (this.uuid + message).getBytes();
            DatagramPacket dp = new DatagramPacket(tempSendData, tempSendData.length, address, port);
            this.socket.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}




