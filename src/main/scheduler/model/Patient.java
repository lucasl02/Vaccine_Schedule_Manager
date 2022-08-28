package scheduler.model;

import scheduler.db.ConnectionManager;
import scheduler.util.Util;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Patient {
    private final String username;
    private final byte[] salt;
    private final byte[] hash;

    private Patient(PatientBuilder builder){
        this.username = builder.username;
        this.salt = builder.salt;
        this.hash = builder.hash;
    }

    private Patient(PatientGetter getter) {
        this.username = getter.username;
        this.salt = getter.salt;
        this.hash = getter.hash;
    }
    public String getUsername() {return this.username;}

    public byte[] getSalt() {return this.salt;}

    public byte[] getHash() {return this.hash;}


    public void saveToDB() throws SQLException{
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addPatient = "INSERT INTO Patients VALUES (? , ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addPatient);
            statement.setString(1, this.username);
            statement.setBytes(2, this.salt);
            statement.setBytes(3, this.hash);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }


    public void reserve (String patientName, Date date, String vaccine) throws SQLException{
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        ArrayList availableCaregiversList = new ArrayList();
        ArrayList takenCaregiversList = new ArrayList();
        int availableDoses = -1;

        try {
            // get available caregivers first
            String allCaregiversQuery = "SELECT DISTINCT A.Username FROM Availabilities AS A WHERE Time = ? ORDER BY A.Username ASC";

            PreparedStatement allCaregiversStatement = con.prepareStatement(allCaregiversQuery);
            allCaregiversStatement.setDate(1,date);
            ResultSet allCaregivers = allCaregiversStatement.executeQuery();

            if(allCaregivers.next()) {
                availableCaregiversList.add(allCaregivers.getString("Username"));
            }

            //check if no caregivers are available
            if (availableCaregiversList.isEmpty()) {
                System.out.println("No Caregiver is available");
                return;
            }

            //check if no doses are available
            String checkDosesQuery = "SELECT V.Doses from Vaccines AS V WHERE V.Name = ?";
            PreparedStatement doseCheckStatement = con.prepareStatement(checkDosesQuery);
            doseCheckStatement.setString(1,vaccine);
            ResultSet doseCheckResult = doseCheckStatement.executeQuery();

            while (doseCheckResult.next()) {
                availableDoses = doseCheckResult.getInt("Doses");
            }

            if(availableDoses == 0){
                System.out.println("Not enough available doses!");
                return;
            }

            //get desired caregiver from user
            String desiredCaregiver = availableCaregiversList.get(0).toString();

            // insert into table caregiver, patientName, date
            String reserveStatement = "INSERT INTO Appointments VALUES (? ,? ,? ,?) ";
            String updateDoses = "UPDATE Vaccines SET doses = doses - 1 WHERE Name = ?";
            String updateAvailabilities = "DELETE FROM Availabilities WHERE Username = ? AND Time = ? ";

            PreparedStatement statement = con.prepareStatement(reserveStatement);

            statement.setString(1,desiredCaregiver);
            statement.setString(2,patientName);
            statement.setDate(3,date);
            statement.setString(4,vaccine);
            statement.executeUpdate();
            int AppointmentID = getID(desiredCaregiver, date);

            //Update Vaccines table to reduce number of doses for the vaccine by one
            PreparedStatement updateDosesStatement = con.prepareStatement(updateDoses);
            updateDosesStatement.setString(1,vaccine);
            updateDosesStatement.executeUpdate();

            //remove caregiver from Availabilities for that day
            PreparedStatement updateAvailabilityStatement = con.prepareStatement(updateAvailabilities);
            updateAvailabilityStatement.setString(1,desiredCaregiver);
            updateAvailabilityStatement.setDate(2,date);
            updateAvailabilityStatement.executeUpdate();

            System.out.println("Appointment ID: " + AppointmentID + ", Caregiver Username: " + desiredCaregiver);
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }
    public static void showAppointments(String patientName) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();


        try {
            String getAppointmentsQuery = "SELECT A.AppointmentID, A.Vaccine, A.Time, A.CaregiverName FROM Appointments AS A WHERE A.PatientName = ? " +
                    "ORDER BY A.AppointmentID DESC";

            PreparedStatement statement = con.prepareStatement(getAppointmentsQuery);
            statement.setString(1,patientName);
            ResultSet appointments = statement.executeQuery();
            while(appointments.next()) {
                System.out.println("Appointment ID: " + appointments.getInt(1) + " Vaccine: " + appointments.getString(2) +
                        " Time: " + appointments.getDate(3) + " Caregiver: " + appointments.getString(4));
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }
    // getting AppointmentID of generated appointment
    public int getID(String CaregiverName, Date d) throws SQLException{
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        ArrayList IDList = new ArrayList();
        int id = -1;
        try{
            String getIDQuery = "SELECT A.AppointmentID FROM Appointments AS A WHERE A.CaregiverName = ? AND" +
                    " A.Time = ?";
            PreparedStatement statement = con.prepareStatement(getIDQuery);
            statement.setString(1,CaregiverName);
            statement.setDate(2,d);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                id = result.getInt(1);
            }
        } catch ( SQLException e) {
            e.printStackTrace();
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
        return id;
    }
    public static class PatientBuilder{

        private final String username;

        private final byte[] salt;

        private final byte[] hash;

        public PatientBuilder(String username, byte[] salt, byte[] hash) {
            this.username = username;
            this.salt = salt;
            this.hash = hash;
        }

        public Patient build() {
            return new Patient(this);
        }
    }

    public static class PatientGetter{

        private final String username;

        private final String password;

        private byte[] salt;

        private byte[] hash;

        public PatientGetter(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public Patient get() throws SQLException{
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getPatient = "SELECT Salt,Hash FROM Patients WHERE Username =?";

            try{
                PreparedStatement statement = con.prepareStatement(getPatient);
                statement.setString(1,this.username);
                ResultSet resultSet = statement.executeQuery();
                while(resultSet.next()) {
                    byte[] salt = resultSet.getBytes("Salt");
                    // we need to call Util.trim() to get rid of the paddings,
                    // try to remove the use of Util.trim() and you'll see :)
                    byte[] hash = Util.trim(resultSet.getBytes("Hash"));
                    // check if the password matches
                    byte[] calculatedHash = Util.generateHash(password, salt);
                    if (!Arrays.equals(hash, calculatedHash)) {
                        return null;
                    } else {
                        this.salt = salt;
                        this.hash = hash;
                        return new Patient(this);
                    }
                }
                return null;
            }catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }

}

