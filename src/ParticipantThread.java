
import transaction.*;
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

    public ParticipantThread(UUID uuid, MonitorDataPaPaThread monitorDataPaPaThread, WriteLogFile writeLogFileMonitor, DatagramSocket socket){
        this.decisionRequests = new LinkedBlockingQueue<>();
        this.uuid = uuid;
        this.monitorDataPaPaThread = monitorDataPaPaThread;
        this.writeLogFileMonitor = writeLogFileMonitor;
        this.socket = socket;
    }

    public void run() {
        initialize();
    }

    private void initialize(){
        switch (this.monitorDataPaPaThread.getTransaction(uuid).getStateP()){
            case INIT:
                stateInit();
            case READY:
                stateReady();
            case COMMIT:
                bookRoomCar();
            case ABORT:
                doAbort();
            case ACK:
                ackGlobalMsg();
        }
    }

    private void stateInit() {

        long startTime = System.nanoTime();
        long endTime = startTime + (5 * 1000000000L);
        boolean vote_request = false;
        while (System.nanoTime() < endTime && !vote_request) {
            DatagramPacket tempDatagramPacket = this.monitorDataPaPaThread.getTransaction(uuid).getDatagramPacket();
            tempDP = tempDatagramPacket;
            if (tempDatagramPacket == null) {
                //nichts Neues angekommen
            } else {
                this.monitorDataPaPaThread.getTransaction(uuid).setDatagramPacket(null);
                String msg[] = new String(tempDatagramPacket.getData(), 0, tempDatagramPacket.getLength()).split(" ");
                if ("VOTE_REQUEST".equals(msg[1])) {
                    vote_request = true;
                }
            }
        }
        if(vote_request) {
            if (checkDatabase()) {
                monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.READY);
                writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid));
            } else {
                monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.ABORT);
                writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid));
            }
        }
        else {
            monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.ABORT);
            writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid));
        }
    }

    private void stateReady() {

        sendMsgCoordinator(" VOTE_COMMIT");

        long startTime = System.nanoTime();
        long endTime = startTime + (5 * 1000000000L);
        boolean receivedMsg = false;
        while (System.nanoTime() < endTime && !receivedMsg) {
            DatagramPacket tempDatagramPacket = this.monitorDataPaPaThread.getTransaction(uuid).getDatagramPacket();
            if (tempDatagramPacket == null) {
                //nichts Neues angekommen
            } else {
                this.monitorDataPaPaThread.getTransaction(uuid).setDatagramPacket(null);
                String msg[] = new String(tempDatagramPacket.getData(), 0, tempDatagramPacket.getLength()).split(" ");
                if ("GLOBAL_COMMIT".equals(msg[1])) {
                    monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.COMMIT);
                    writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid));
                    receivedMsg = true;
                } else if ("GLOBAL_ABORT".equals(msg[1])) {
                    monitorDataPaPaThread.setTransactionStatus(this.uuid, states_participant.ABORT);
                    writeLogFileMonitor.writeToFileParticipant(monitorDataPaPaThread.getTransaction(this.uuid));
                    receivedMsg = true;
                }
            }
        }
    }

    private void doAbort() {
        // send VOTE_ABORT
    }

    private void ackGlobalMsg() {
        sendMsgCoordinator("ACK");
    }




    private boolean checkDatabase() {
        // Logik um in datenbank zu checken ob etwas da ist und ggfs zu reservieren
        return true;
    }

    private void bookRoomCar() {
        // Logik um Mietwagen/hotelzimmer aus der datenbank zu nehmen
    }

    private void sendMsgCoordinator(String message){
        try {
            byte[] tempSendData = (this.uuid + message).getBytes();
            DatagramPacket dp = new DatagramPacket(tempSendData, tempSendData.length, tempDP.getAddress(), tempDP.getPort());
            this.socket.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}




