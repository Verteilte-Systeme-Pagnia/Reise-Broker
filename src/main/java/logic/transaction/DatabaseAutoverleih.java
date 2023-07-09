package logic.transaction;

import java.sql.*;

public class DatabaseAutoverleih {
    private Connection connection;
    public DatabaseAutoverleih() {
        try {
            this.connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/autoverleih", "root", "pass");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public int getFreeCars(String startDate, String endDate) throws SQLException {
        this.checkOutdatedBookings();
        String sql = "SELECT COUNT(*) FROM autos WHERE " + "reserved = 0 AND" +
                " id NOT IN (SELECT carId FROM buchungen WHERE (startDatum >= " + "'" +  startDate + "'" + " AND startDatum <=  " + "'" + endDate + "'" + ") OR (" +
                " endDatum >= " + "'" + startDate + "'" + " AND endDatum <= " + "'" + endDate + "'" + ") )";

        PreparedStatement ps = this.connection.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt("COUNT(*)");
    }

    public int getReservedCars() throws SQLException {

        String sql = "SELECT COUNT(*) FROM autos WHERE reserved = 1";
        PreparedStatement ps = this.connection.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt("COUNT(*)");
    }

    public synchronized boolean reserveCar( int number, String startDate, String endDate,String transactionId) throws SQLException {
        String sql = "UPDATE autos SET reserved = 1, SET transactionId =" + transactionId + " WHERE reserved = 0 AND id NOT IN " +
                "(SELECT carId FROM buchungen WHERE (startDatum >= " + "'" +  startDate + "'" + " AND startDatum <=  " + "'" + endDate + "'" + ") OR (" +
                " endDatum >= " + "'" + startDate + "'" + " AND endDatum <= " + "'" + endDate + "'" + ") ) LIMIT " + number;
        PreparedStatement ps = this.connection.prepareStatement(sql);
        int affectedRows = ps.executeUpdate();
        /*Wenn die Anzahl der zu reservierenden Autos mit der Anzahl der aktualisierten Zeilen übereinstimmt, war die Datenbankoperation erfolgreich.*/
        return affectedRows == number;
    }

    public  synchronized boolean cancelReservation(String transactionId) throws SQLException {
        String sql = "UPDATE autos SET reserved = 0 WHERE reserved = 1 AND transactionId = " + transactionId;
        PreparedStatement ps = this.connection.prepareStatement(sql);
        ps.executeUpdate();
        /*Wenn es keine reservierten Autos mehr gibt, war die Datenbankoperation erfolgreich*/
        return this.getReservedCars() == 0;
    }

    public synchronized boolean  bookReservedCars(String startDate, String endDate, String transactioniD) throws  SQLException{
        int reservedCars = getReservedCars();
        int insertCount = 0;
        for(int i = 0; i<reservedCars;i++) {
            String sql = "INSERT INTO buchungen (carId,startDatum,endDatum) " +
                    "VALUES((SELECT id FROM autos WHERE reserved = 1 AND transactioniD = " + transactioniD + " LIMIT 1)," + "'" + startDate + "', '" + endDate + "' )";
            PreparedStatement ps = this.connection.prepareStatement(sql);
            insertCount += ps.executeUpdate();
            sql = "UPDATE autos SET reserved = 0 WHERE reserved = 1 LIMIT 1";
            ps = this.connection.prepareStatement(sql);
            ps.executeUpdate();
        }
        /*Wenn es keine reservierten Autos mehr gibt und genauso viele Buchungen erstellt wurden,
         wie ursprünglich reservierte Autos, dann war die Datenbankoperation erfolgreich.*/
        return getReservedCars() == 0 && reservedCars == insertCount;
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
