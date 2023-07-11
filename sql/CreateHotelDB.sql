CREATE DATABASE hotel;

CREATE TABLE zimmer (
id INT PRIMARY KEY AUTO_INCREMENT,
reserved BOOLEAN,
transactionId VARCHAR(36)
);

CREATE TABLE buchungen (
id INT PRIMARY KEY AUTO_INCREMENT,
roomId INT,
startDatum DATE,
endDatum Date,
FOREIGN KEY (roomId) REFERENCES zimmer(id)
)