package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentPatient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    //Part 1
    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "Select * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occured when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1 FINISHED
        if (currentPatient != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    // Part 2
    private static void searchCaregiverSchedule(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first!");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        System.out.println(date);
        try {
            Date d = Date.valueOf(date);
            getSchedule(d);
        } catch (IllegalArgumentException e){
            System.out.println("Please try again!");
        } catch (SQLException e) {
            System.out.println("Please try again!");
        }
    }

    public static void getSchedule(Date date) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String retrieveAvailableCaregivers = "SELECT DISTINCT A.Username FROM Availabilities AS A WHERE A.time = ? " +
                "ORDER BY A.Username ASC";

        try {
            PreparedStatement statement = con.prepareStatement(retrieveAvailableCaregivers);
            statement.setDate(1, date);
            ResultSet caregiverResult = statement.executeQuery();

            while (caregiverResult.next()) {
                    System.out.println("Caregiver: " + caregiverResult.getString("Username"));
            }
            String retrieveVaccineDoses = "SELECT V.Name, V.Doses FROM Vaccines AS V";
            PreparedStatement statement2 = con.prepareStatement(retrieveVaccineDoses);
            ResultSet dosesResult = statement2.executeQuery();
            while (dosesResult.next()) {
                System.out.println("Vaccine: " + dosesResult.getString(1) + " Available Doses: " +
                dosesResult.getInt(2));
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first!");
            return;
        }

        if (currentCaregiver != null && currentPatient == null) {
            System.out.println("Please login as a patient first!");
            return;
        }

        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }

        String name = currentPatient.getUsername();
        String date = tokens[1];
        String vaccineName = tokens[2];

        try {
            Date d = Date.valueOf(date);
            currentPatient.reserve(name, d, vaccineName);
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first!");
            return;
        }
        int AppointmentID = Integer.parseInt(tokens[1]);
        try {
            cancelAppointment(AppointmentID);
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        }
    }

    private static void cancelAppointment(int ID) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String cancelDate = "";
        String CaregiverName = "";
        String VaccineName = "";

        String getRemovedDate = "SELECT A.Time,A.CaregiverName,A.Vaccine FROM Appointments AS A WHERE A.AppointmentID = ?";

        try {
            PreparedStatement statement = con.prepareStatement(getRemovedDate);
            statement.setInt(1, ID);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                cancelDate = resultSet.getString("Time");
                CaregiverName = resultSet.getString("CaregiverName");
                VaccineName = resultSet.getString("Vaccine");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException();
        }

        String removeAppointment = "DELETE FROM Appointments WHERE Appointments.AppointmentID = ?";
        try {
            PreparedStatement statement = con.prepareStatement(removeAppointment);
            statement.setInt(1, ID);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        }

        String addAvailability = "INSERT INTO Availabilities VALUES (?,?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAvailability);
            statement.setString(1, cancelDate);
            statement.setString(2, CaregiverName);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        }

        String addDose = "UPDATE Vaccines SET doses = doses + 1 WHERE Name = ?";
        try {
            PreparedStatement statement = con.prepareStatement(addDose);
            statement.setString(1, VaccineName);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException();
        }
        System.out.println("Appointment Canceled");
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        if ((currentCaregiver == null) && (currentPatient == null)) {
            System.out.println("Please login first");
            return;
        }
        if (currentPatient == null) {
            try {
                currentCaregiver.showAppointments(currentCaregiver.getUsername());
            } catch (SQLException e) {
                System.out.println("Please try again!");
            }
        } else {
            try {
                currentPatient.showAppointments(currentPatient.getUsername());
            } catch (SQLException e) {
                System.out.println("Please try again!");
            }
        }
    }

    private static void logout(String[] tokens) {
        if ((currentCaregiver == null) && (currentPatient == null)) {
            System.out.println("Please login first");
            return;
        }
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }

        currentCaregiver = null;
        currentPatient = null;
        System.out.println("Successfully logged out!");
    }
}

