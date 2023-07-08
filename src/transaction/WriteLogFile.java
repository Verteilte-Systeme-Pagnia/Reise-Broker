package transaction;

import java.io.*;
import java.net.DatagramPacket;
import java.util.*;

public class WriteLogFile {

    private File logFile;

    public WriteLogFile(String LogFileName){
        try{
            this.logFile = new File(LogFileName);
            if(!this.logFile.exists()){
                    this.logFile.createNewFile();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void writeToFile(TransactionCoordinator transaction, DatagramPacket dp) {
        try (FileWriter fileWriter = new FileWriter( this.logFile, true)) {
            fileWriter.write(transaction.getUUID() + " " + transaction.getStateC() + " " + transaction.senderReference.getSenderAddress() + " " + transaction.senderReference.getSenderPort() + " "+ new String(dp.getData(),0,dp.getLength()) +"\n");
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void writeToFileParticipant(TransactionParticipant transaction, DatagramPacket dp) {
        try (FileWriter fileWriter = new FileWriter( this.logFile, true)) {
            fileWriter.write(transaction.getUUID() + " " + transaction.getStateP() + " " + transaction.senderReference.getSenderAddress() + " " + transaction.senderReference.getSenderPort() + " "+ new String(dp.getData(),0,dp.getLength()) + "\n");
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public synchronized void writeToFileFinalResult(TransactionCoordinator transaction,String finalResult) {
        try (FileWriter fileWriter = new FileWriter( this.logFile, true)) {
            fileWriter.write(transaction.getUUID() + " " + transaction.getStateC() + " " + transaction.senderReference.getSenderAddress() + " " + transaction.senderReference.getSenderPort()+ " finalResult:"+finalResult +"\n");
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized String readLastRecordedState(UUID uuid) {
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
