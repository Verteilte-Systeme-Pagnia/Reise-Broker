package logic;

import java.sql.*;
import java.util.concurrent.Semaphore;

public class DatabaseHotel {
    private Connection connection;
    private Semaphore semaphore = new Semaphore(1,true);

    public DatabaseHotel(){
        try {
            this.connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/hotel", "root", "pass");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
    public int getFreeRooms(String startDate, String endDate) {
        try {
            semaphore.acquire();
            try {
                this.checkOutdatedBookings();
                String sql = "SELECT COUNT(*) FROM zimmer WHERE " + "reserved = 0 AND" +
                        " id NOT IN (SELECT roomId FROM buchungen WHERE (startDatum >= " + "'" + startDate + "'" + " AND startDatum <=  " + "'" + endDate + "'" + ") OR (" +
                        " endDatum >= " + "'" + startDate + "'" + " AND endDatum <= " + "'" + endDate + "'" + ") )";

                PreparedStatement ps = this.connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                rs.next();
                return rs.getInt("COUNT(*)");
            }catch(SQLException e){

            }finally {
                semaphore.release();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    public int getReservedRooms() throws SQLException {

        String sql = "SELECT COUNT(*) FROM zimmer WHERE reserved = 1";
        PreparedStatement ps = this.connection.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt("COUNT(*)");
    }

    public synchronized boolean reserveRoom( int number, String startDate, String endDate, String transactionId) throws SQLException {
        String sql = "UPDATE zimmer SET reserved = 1, SET transactionId =" + transactionId +  " WHERE reserved = 0 AND id NOT IN " +
                "(SELECT roomId FROM buchungen WHERE (startDatum >= " + "'" +  startDate + "'" + " AND startDatum <=  " + "'" + endDate + "'" + ") OR (" +
                " endDatum >= " + "'" + startDate + "'" + " AND endDatum <= " + "'" + endDate + "'" + ") ) LIMIT " + number;
        PreparedStatement ps = this.connection.prepareStatement(sql);
        int affectedRows = ps.executeUpdate();
        if(affectedRows != number){
            cancelReservation(transactionId);
            return false;
        }
        /*Wenn die Anzahl der zu reservierenden Autos mit der Anzahl der aktualisierten Zeilen übereinstimmt, war die Datenbankoperation erfolgreich.*/
        return true;
    }

    public synchronized boolean cancelReservation(String transactionId) throws SQLException {
        String sql = "UPDATE zimmer SET reserved = 0 WHERE reserved = 1 AND transactionId = " + transactionId;;
        PreparedStatement ps = this.connection.prepareStatement(sql);
        ps.executeUpdate();
        /*Wenn es keine reservierten Zimmer mehr gibt, war die Datenbankoperation erfolgreich*/
        return this.getReservedRooms() == 0;
    }

    public synchronized boolean bookReservedRooms(String startDate, String endDate,String transactionId) throws  SQLException{
        int reservedRooms = getReservedRooms();
        int insertCount = 0;
        for(int i = 0; i<reservedRooms;i++) {
            String sql = "INSERT INTO buchungen (roomId,startDatum,endDatum) " +
                    "VALUES((SELECT id FROM zimmer WHERE reserved = 1 ÁND transactionId = " + transactionId + "Limit 1)," + "'" + startDate + "', '" + endDate + "' )";
            PreparedStatement ps = this.connection.prepareStatement(sql);
            insertCount += ps.executeUpdate();
            sql = "UPDATE zimmer SET reserved = 0 WHERE reserved = 1 LIMIT 1";
            ps = this.connection.prepareStatement(sql);
            ps.executeUpdate();
        }
       /*Wenn es keine reservierten Zimmer mehr gibt und genauso viele Buchungen erstellt wurden,
         wie ursprünglich reservierte Autos, dann war die Datenbankoperation erfolgreich.*/
        return getReservedRooms() == 0 && reservedRooms == insertCount;
    }

    public boolean checkOutdatedBookings() throws  SQLException{
        String sql = "DELETE FROM buchungen WHERE endDatum <  CURDATE()";
        PreparedStatement ps = this.connection.prepareStatement(sql);
        ps.executeUpdate();
        sql = "SELECT COUNT(*) FROM buchungen WHERE endDatum <  CURDATE()";
        ps = this.connection.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        rs.next();
        /* Wenn es keine Buchungen in der Vergangenheit gibt, war die Datenbankoperation erfolgreich*/
        return rs.getInt("COUNT(*)") == 0;
    }
}
