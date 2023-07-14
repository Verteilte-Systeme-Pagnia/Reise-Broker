package fehler.fehlerfall1.java.logic.transaction;

import java.io.*;
import java.net.DatagramPacket;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class WriteLogFile {//schreiben um in Log File zu speichern

    private File logFile;
    private Semaphore semaphore = new Semaphore(1,true);
    public WriteLogFile(String LogFileName){//log file erstellen, falls diese noch nicht existiert
        try{
            this.logFile = new File(LogFileName);
            if(!this.logFile.exists()){
                    this.logFile.createNewFile();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToFile(TransactionCoordinator transaction, DatagramPacket dp) {//Schreiben in log-file wird von koordinatoren verwendet mit entsprechenden log daten
        try {
            semaphore.acquire();
            try (FileWriter fileWriter = new FileWriter(this.logFile, true)) {
                fileWriter.write(transaction.getUUID() + " " + transaction.getStateC() + " " + transaction.senderReference.getSenderAddress() + " " + transaction.senderReference.getSenderPort() + " " + new String(dp.getData(), 0, dp.getLength()) + " " + transaction.rooms + " " + transaction.autos + " " + transaction.fromDate + " " + transaction.toDate + "\n");
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeToFileExtra(TransactionCoordinator transaction) {//Schreiben in log-file wird von koordinatoren verwendet mit entsprechenden log daten
        try {
            semaphore.acquire();
            try (FileWriter fileWriter = new FileWriter(this.logFile, true)) {
                fileWriter.write(transaction.getUUID() + " " + transaction.getStateC() + " " + transaction.senderReference.getSenderAddress() + " " + transaction.senderReference.getSenderPort() + "\n");
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeToFileParticipant(TransactionParticipant transaction, DatagramPacket dp) {//Schreiben in log-file wird von Partizipanten verwendet mit entsprechenden log daten
        try {
            semaphore.acquire();
            try (FileWriter fileWriter = new FileWriter(this.logFile, true)) {
                fileWriter.write(transaction.getUUID() + " " + transaction.getStateP() + " " + transaction.senderReference.getSenderAddress() + " " + transaction.senderReference.getSenderPort() + " " + new String(dp.getData(), 0, dp.getLength()) + " " + transaction.rooms + " " + transaction.autos + " " + transaction.fromDate + " " + transaction.toDate + "\n");
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeToFileParticipantExtra(TransactionParticipant transaction) {//Schreiben in log-file wird von koordinatoren verwendet mit entsprechenden log daten
        try {
            semaphore.acquire();
            try (FileWriter fileWriter = new FileWriter(this.logFile, true)) {
                fileWriter.write(transaction.getUUID() + " " + transaction.getStateP() + " " + transaction.senderReference.getSenderAddress() + " " + transaction.senderReference.getSenderPort() + "\n");
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public void writeToFileFinalResult(TransactionCoordinator transaction, String finalResult) {//schreiben der final result, das der partizipant die endgültige entscheidung weiß, falls er ausfällt
        try {
            semaphore.acquire();
            try (FileWriter fileWriter = new FileWriter(this.logFile, true)) {
                fileWriter.write(transaction.getUUID() + " " + transaction.getStateC() + " " + transaction.senderReference.getSenderAddress() + " " + transaction.senderReference.getSenderPort() + " finalResult:" + finalResult + "\n");
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String readLastRecordedState(UUID uuid) {//auslesen des letzten status der für die jeweilige uuid, was in der entsprechenden logfile zuletzt gespeichert wurde
        try {
            semaphore.acquire();
            String lastRecordedState = null;
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(this.logFile))) {

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String tempLine[] = line.split(" ");
                    System.out.println(tempLine[0] + uuid.toString()+tempLine[0].equals(uuid.toString()));
                    if (tempLine[0].equals(uuid.toString())) {
                        if (!(tempLine[1].equals("ACK") || tempLine[1].equals("Finish"))) {
                            lastRecordedState = line;
                            System.out.println(lastRecordedState);
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            semaphore.release();
            return lastRecordedState;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
