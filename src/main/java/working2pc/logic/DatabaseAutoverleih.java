package working2pc.logic;

import java.sql.*;
import java.util.concurrent.Semaphore;

public class DatabaseAutoverleih {
    private Connection connection;
    private Semaphore semaphore = new Semaphore(1,true);
    public DatabaseAutoverleih() {
        try {
            //stellt die Verbindung zu der MySQL Datenbank her
            this.connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/autoverleih", "root", "pass");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //gibt die Anzahl an freien Autos für den ausgewählten Zeitraum zurück
    public int getFreeCars(String startDate, String endDate) {
        try {
            semaphore.acquire();
            try {
                this.checkOutdatedBookings();
                String sql = "SELECT COUNT(*) FROM autos WHERE " + "reserved = 0 AND" +
                        " id NOT IN (SELECT carId FROM buchungen WHERE (startDatum >= " + "'" + startDate + "'" + " AND startDatum <=  " + "'" + endDate + "'" + ") OR (" +
                        " endDatum >= " + "'" + startDate + "'" + " AND endDatum <= " + "'" + endDate + "'" + ") )";
                PreparedStatement ps = this.connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                rs.next();
                return rs.getInt("COUNT(*)");
            }catch (SQLException e){
                e.printStackTrace();
            }finally {
                semaphore.release();
            }
        }catch(InterruptedException e){
            throw new RuntimeException(e);
        }
        return -1;
    }

    //gibt die Anzahl an reservierten Autos der dementsprechenden Transaktion zurück
    public int getReservedCars(String transactionId) {
        try {
            String sql = "SELECT COUNT(*) FROM autos WHERE reserved = 1 AND transactionId = " + "'" + transactionId + "'";
            PreparedStatement ps = this.connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt("COUNT(*)");
        }catch (SQLException e){
                e.printStackTrace();
            }
        return -1;
    }
    //reserviert die gewünschte Anzahl an Autos für den ausgewählten Zeitraum
    public boolean reserveCar( int number, String startDate, String endDate,String transactionId){
        try {
            semaphore.acquire();
            try {
                String sql = "UPDATE autos SET reserved = 1, transactionId = " + "'" + transactionId + "'" + " WHERE reserved = 0 AND id NOT IN " +
                        "(SELECT carId FROM buchungen WHERE (startDatum >= " + "'" + startDate + "'" + " AND startDatum <=  " + "'" + endDate + "'" + ") OR (" +
                        " endDatum >= " + "'" + startDate + "'" + " AND endDatum <= " + "'" + endDate + "'" + ") ) LIMIT " + number;
                PreparedStatement ps = this.connection.prepareStatement(sql);
                int affectedRows = ps.executeUpdate();
                //wenn nicht genügend Autos reserviert werden konnten, werden alle Reservierungen rückgängig gemacht
                if (affectedRows != number) {
                    cancelReservation(transactionId);
                    return false;
                }
            }catch(SQLException e ){
                e.printStackTrace();
            }finally{
                semaphore.release();
            }
        }catch(InterruptedException e){
            throw new RuntimeException(e);
        }
        return true;
    }

    //stoniert alle Reservierungen der Transaktion
    public boolean cancelReservation(String transactionId) {
            try {
                String sql = "UPDATE autos SET reserved = 0 WHERE reserved = 1 AND transactionId = " + "'" + transactionId + "'";
                PreparedStatement ps = this.connection.prepareStatement(sql);
                ps.executeUpdate();
                /*Wenn es keine reservierten Autos mehr gibt, war die Datenbankoperation erfolgreich*/
                return this.getReservedCars(transactionId) == 0;
            }catch(SQLException e){
                e.printStackTrace();
            }
        return false;
    }

    //bucht die reservierten Autos der Transaktion für den ausgewählten Zeitraum
    public boolean bookReservedCars(String startDate, String endDate, String transactionId){
        try {
            semaphore.acquire();
            try {
                int reservedCars = getReservedCars(transactionId);
                int insertCount = 0;
                for (int i = 0; i < reservedCars; i++) {
                    String sql = "INSERT INTO buchungen (carId,startDatum,endDatum) " +
                            "VALUES((SELECT id FROM autos WHERE reserved = 1 AND transactionId = " + "'" + transactionId + "'" + " LIMIT 1)," + "'" + startDate + "', '" + endDate + "' )";
                    PreparedStatement ps = this.connection.prepareStatement(sql);
                    insertCount += ps.executeUpdate();

                    sql = "UPDATE autos SET reserved = 0, transactionId = null WHERE reserved = 1 AND transactionId = " + "'" + transactionId + "'" + " LIMIT 1";
                    ps = this.connection.prepareStatement(sql);
                    ps.executeUpdate();
                }
                /*Wenn es keine reservierten Autos mehr gibt und genauso viele Buchungen erstellt wurden,
                wie ursprünglich reservierte Autos, dann war die Datenbankoperation erfolgreich.*/
                return getReservedCars(transactionId) == 0 && reservedCars == insertCount;
            }catch(SQLException e){
                e.printStackTrace();
            }finally {
                semaphore.release();
            }
        }catch (InterruptedException e){
            throw new RuntimeException(e);
        }
        return false;
    }

    //löscht Buchungen, die in der Vergangenheit liegen und somit nicht mehr von Relevanz sind
    public boolean checkOutdatedBookings(){

            try {
                String sql = "DELETE FROM buchungen WHERE endDatum <  CURDATE()";
                PreparedStatement ps = this.connection.prepareStatement(sql);
                ps.executeUpdate();
                sql = "SELECT COUNT(*) FROM buchungen WHERE endDatum <  CURDATE()";
                ps = this.connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                rs.next();
                /* Wenn es keine Buchungen in der Vergangenheit gibt, war die Datenbankoperation erfolgreich*/
                return rs.getInt("COUNT(*)") == 0;
            }catch (SQLException e){
                e.printStackTrace();
            }
        return false;
    }

}
