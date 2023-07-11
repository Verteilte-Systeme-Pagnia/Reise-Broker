CREATE DATABASE autoverleih;

CREATE TABLE autos (
id INT PRIMARY KEY AUTO_INCREMENT,
reserved BOOLEAN,
transactionId VARCHAR(36)
);

CREATE TABLE buchungen (
id INT PRIMARY KEY AUTO_INCREMENT,
carId INT,
startDatum DATE,
endDatum Date,
FOREIGN KEY (carId) REFERENCES autos(id)
)

