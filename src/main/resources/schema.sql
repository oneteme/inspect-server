CREATE TABLE IncomingRequest (
    id INT AUTO_INCREMENT PRIMARY KEY, --uuid varchar(36)
    contentType  VARCHAR(255),
    application VARCHAR(255),
    endpoint VARCHAR(255),
    resource VARCHAR(255),
    principal VARCHAR(255)
);

CREATE TABLE OutcomingRequest (
    id INT AUTO_INCREMENT PRIMARY KEY, --uuid varchar(36)
    url VARCHAR(255),
    method VARCHAR(255),
    status INT,
    start TIMESTAMP,
    end TIMESTAMP,
    incomingRequestId INT,
    FOREIGN KEY (incomingRequestId) REFERENCES IncomingRequest(id)
);

CREATE TABLE OutcomingQuery (
    id INT AUTO_INCREMENT PRIMARY KEY, --uuid varchar(36)
    start TIMESTAMP,
    end TIMESTAMP,
    incomingRequestId INT,
    FOREIGN KEY  (incomingRequestId) REFERENCES IncomingRequest(id)
);




CREATE TABLE DatabaseAction (
    id INT AUTO_INCREMENT PRIMARY KEY, 
    type VARCHAR(255),
    start TIMESTAMP,
    end TIMESTAMP,
    failed BOOLEAN,
    outcomingQueryId INT,
    FOREIGN KEY (outcomingQueryId) REFERENCES outcomingQueryId(id)
);