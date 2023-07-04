
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
                sendVoteCommit();
            case ABORT:
                sendVoteAbort();
            case ACK:
                ackGlobalMsg();
        }
    }

    private void stateInit(){
        monitorDataPaPaThread.setTransactionStatus(this.uuis, states_participant.INIT)
        long startTime = System.nanoTime();
        long endTime = startTime + (5 * 1000000000L);
    }while(System.nanoTime() < endTime){
            DatagramPacket tempDatagramPacket = this.monitorDataPaPaThread.getTransaction(uuid).getDatagramPacket();

        }

        writeToFile("INIT");

        // Auf VOTE_REQUEST vom Koordinator warten
        waitForVoteRequest(); //falls received messsage "VOTE_REQUEST -> weiterläuft"

        // Überprüfe Zeitüberschreitung
        if (isTimeoutExpired()) { //Timout abändern, funktioniert so nicht
            writeToFile("VOTE_ABORT");
            exit();
        }

        // Überprüfe die Entscheidung des Teilnehmers
        if (participantVoteForCommit()) {
            writeToFile("VOTE_COMMIT");
            sendVoteCommit();
            waitForDecision();

            // Überprüfe Zeitüberschreitung
            if (isTimeoutExpired()) { //Timout abändern
                sendDecisionRequest();
                waitForDecision();
                writeToFile("DECISION in lokale Protokolldatei schreiben");
            }

            String lastRecordedState = readLastRecordedState();
            if (lastRecordedState.equals("GLOBAL_COMMIT")) {
                writeToFile("GLOBAL_COMMIT"); //Commit -> Transaktion ausführen, auto und zimmer von datenbank nehmen
            } else if (lastRecordedState.equals("INIT") || lastRecordedState.equals("GLOBAL_ABORT")) {
                writeToFile("GLOBAL_ABORT"); //READY Zustand???
            }
        } else {
            writeToFile("VOTE_ABORT");
            sendVoteAbort();
        }

        // Separate Thread-Implementierung für den Empfang von DECISION_REQUESTs
        Thread decisionRequestThread = new Thread(() -> {
            while (true) {
                // Warten, bis alle ankommenden DECISION_REQUESTs empfangen wurden
                String decisionRequest = waitForDecisionRequest();
                String lastRecordedState = readLastRecordedState();

                // Überprüfe den letzten aufgezeichneten STATE und sende entsprechende Antwort
                if (lastRecordedState.equals("GLOBAL_COMMIT")) { //Anfrage an anderen patizipanten, was gerade los ist
                    sendGlobalCommit(decisionRequest);
                } else if (lastRecordedState.equals("INIT") || lastRecordedState.equals("GLOBAL_ABORT")) {
                    sendGlobalAbort(decisionRequest);
                } else {
                    // Teilnehmer bleibt blockiert (skip)
                }
            }
        });

        decisionRequestThread.start();
    }

    private void waitForVoteRequest() {
        // Warte auf VOTE_REQUEST vom Koordinator
        try {
            DatagramSocket socket = new DatagramSocket(UDP_PORT);
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            while(true) {
                try {
                    socket.receive(receivePacket);
                    String voteRequest = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    System.out.println("Received VOTE_REQUEST: " + voteRequest);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isTimeoutExpired() {
        // Überprüfe, ob die Zeitüberschreitung abgelaufen ist
        // Implementierung abhängig von der verwendeten Zeitmessung
        return false;
    }

    private void exit() {
        // Beende das Protokoll
        System.exit(0);
    }

    private boolean participantVoteForCommit() {
        // Kontrollieren in datenbank, + reservieren
        return true;
    }

    private void sendVoteCommit() {
        // Sende VOTE_COMMIT an den Koordinator
        try {
            InetAddress coordinatorAddress = InetAddress.getByName(MULTICAST_ADDRESS);
            byte[] sendData = "VOTE_COMMIT".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, coordinatorAddress, UDP_PORT);
            socket.send(sendPacket);
            System.out.println("VOTE_COMMIT");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void waitForDecision() {
        // Warte auf DECISION vom Koordinator
        try {
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);

            String decision = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Received DECISION: " + decision);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendDecisionRequest() {
        // Sende DECISION_REQUEST als Multicast an andere Teilnehmer
        try {
            InetAddress multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS);
            byte[] sendData = "DECISION_REQUEST".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, multicastAddress, UDP_PORT);
            socket.send(sendPacket);
            System.out.println("Sent DECISION_REQUEST to Participants");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readLastRecordedState() {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(LOCAL_LOG_FILE))) {
            String lastRecordedState = null;
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lastRecordedState = line;
            }
            return lastRecordedState;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendVoteAbort() {
        // Sende VOTE_ABORT an den Koordinator
        try {
            InetAddress coordinatorAddress = InetAddress.getByName(MULTICAST_ADDRESS);
            byte[] sendData = "VOTE_ABORT".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, coordinatorAddress, UDP_PORT);
            socket.send(sendPacket);
            System.out.println("Sent VOTE_ABORT to Coordinator");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String waitForDecisionRequest() {
        try {
            String decisionRequest = decisionRequests.take();
            System.out.println("Received DECISION_REQUEST: " + decisionRequest);
            return decisionRequest;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendGlobalCommit(String decisionRequest) {
        // Sende GLOBAL_COMMIT an den Teilnehmer, der DECISION_REQUEST gesendet hat
        try {
            InetAddress participantAddress = InetAddress.getByName(decisionRequest);
            byte[] sendData = "GLOBAL_COMMIT".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, participantAddress, UDP_PORT);
            socket.send(sendPacket);
            System.out.println("Sent GLOBAL_COMMIT to Participant: " + decisionRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendGlobalAbort(String decisionRequest) {
        // Sende GLOBAL_ABORT an den Teilnehmer, der DECISION_REQUEST gesendet hat
        try {
            InetAddress participantAddress = InetAddress.getByName(decisionRequest);
            byte[] sendData = "GLOBAL_ABORT".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, participantAddress, UDP_PORT);
            socket.send(sendPacket);
            System.out.println("Sent GLOBAL_ABORT to Participant: " + decisionRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Beispielverwendung der Klasse TwoPhaseCommitProtocol
        String participantName = "Teilnehmer1";

        try {
            DatagramSocket socket = new DatagramSocket(UDP_PORT);
            ParticipantThread participantThread = new ParticipantThread(participantName);
            participantThread.socket = socket;
            participantThread.startProtocol();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void setStateP(states_participant stateP) {
        this.stateP = stateP;
    }

    public states_participant getStateP(){
        return stateP;
    }
}

