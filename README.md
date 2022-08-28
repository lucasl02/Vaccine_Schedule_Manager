# Vaccine_Schedule_Manager
Program implemented in Java and interacts with a Microsoft Azure Database. Simulates an online vaccine schedule (stored in online SQL Database). Allows for the creation of "caregivers" and "patients" accounts whose passwords are encrypted through adding salt and a hash function.

Caregivers have the ability to login and add appointment availabilities, check their appointments, update vaccine doses.

Patients have the ability to login to check schedule for available appointments, reserve apointments for specific vaccines, and cancel appointments.

Methods in program either send SQL queries to online database and returns query results, or update the online database through SQL queries.

Database ER diagram included.
