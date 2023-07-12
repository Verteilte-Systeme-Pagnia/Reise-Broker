package logic.transaction;

import java.io.*;
import java.net.DatagramPacket;
import java.util.*;

public class WriteLogFile {//schreiben um in Log File zu speichern

    private File logFile;

    public WriteLogFile(String LogFileName){//log file ersttellen falls diese noch nicht existiert
        try{
            this.logFile = new File(LogFileName);
            if(!this.logFile.exists()){
                    this.logFile.createNewFile();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void writeToFile(TransactionCoordinator transaction, DatagramPacket dp) {//Schreiben in log-file wird von koordinatoren verwendet mit entsprechenden log daten
        try (FileWriter fileWriter = new FileWriter( this.logFile, true)) {
            fileWriter.write(transaction.getUUID() + " " + transaction.getStateC() + " " + transaction.senderReference.getSenderAddress() + " " + transaction.senderReference.getSenderPort() + " "+ new String(dp.getData(),0,dp.getLength()) + " "+ transaction.rooms+ " "+ transaction.autos+ " "+ transaction.fromDate+ " "+ transaction.toDate+"\n");
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void writeToFileParticipant(TransactionParticipant transaction, DatagramPacket dp) {//Schreiben in log-file wird von Partizipanten verwendet mit entsprechenden log daten
        try (FileWriter fileWriter = new FileWriter( this.logFile, true)) {
            fileWriter.write(transaction.getUUID() + " " + transaction.getStateP() + " " + transaction.senderReference.getSenderAddress() + " " + transaction.senderReference.getSenderPort() + " "+ new String(dp.getData(),0,dp.getLength()) + " "+ transaction.rooms+ " "+ transaction.autos+ " "+ transaction.fromDate+ " "+ transaction.toDate+"\n");
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public synchronized void writeToFileFinalResult(TransactionCoordinator transaction,String finalResult) {//schreiben der final result, das der partizipant die endgültige entscheidung weiß, falls er ausfällt
        try (FileWriter fileWriter = new FileWriter( this.logFile, true)) {
            fileWriter.write(transaction.getUUID() + " " + transaction.getStateC() + " " + transaction.senderReference.getSenderAddress() + " " + transaction.senderReference.getSenderPort()+ " finalResult:"+finalResult +"\n");
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized String readLastRecordedState(UUID uuid) {//auslesen des letzten status der für die jeweilige uuid, was in der entsprechenden logfile zuletzt gespeichert wurde
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(this.logFile))) {
            String lastRecordedState = null;
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String tempLine[] = line.split(" ");
                if(tempLine[0].equals(uuid.toString())){
                    if(!tempLine[1].equals("ACK")){
                        lastRecordedState = line;
                    }
                }
            }
            return lastRecordedState;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
