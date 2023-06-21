CREATE TABLE E_IN_REQ (
    ID_IN_REQ VARCHAR(36) PRIMARY KEY,
    VA_PRTCL VARCHAR(255) NOT NULL,
    VA_HST VARCHAR(255) NOT NULL,
    CD_PRT INT,
    VA_PTH VARCHAR(255) NOT NULL,
    VA_QRY VARCHAR(255) NOT NULL,
    VA_MTH VARCHAR(10) NOT NULL,
    CD_STT INT,
    VA_I_SZE BIGINT,
    VA_O_SZE BIGINT,
    DH_DBT TIMESTAMP(3) NOT NULL,
    DH_FIN TIMESTAMP(3) NOT NULL,
    VA_THRED VARCHAR(255) NOT NULL,
    VA_CNT_TYP VARCHAR(255),
    VA_ACT VARCHAR(255 ),
    VA_RSC VARCHAR(255),
    VA_CLI VARCHAR(255),
    VA_GRP VARCHAR(255)
);

CREATE TABLE E_OUT_REQ (
    ID_OUT_REQ VARCHAR(36) PRIMARY KEY,
    VA_PRTCL VARCHAR(255) NOT NULL,
    VA_HST VARCHAR(255) NOT NULL,
    CD_PRT INT,
    VA_PTH VARCHAR(255) NOT NULL,
    VA_QRY VARCHAR(255) NOT NULL,
    VA_MTH VARCHAR(10) NOT NULL,
    CD_STT INT,
    VA_I_SZE BIGINT,
    VA_O_SZE BIGINT,
    DH_DBT TIMESTAMP(3) NOT NULL,
    DH_FIN TIMESTAMP(3) NOT NULL,
    VA_THRED VARCHAR(255) NOT NULL,
    CD_IN_REQ VARCHAR(36) NOT NULL,
    FOREIGN KEY (CD_IN_REQ) REFERENCES E_IN_REQ(ID_IN_REQ)
);

CREATE TABLE E_OUT_QRY (
    ID_OUT_QRY BIGINT,
    VA_HST VARCHAR(255),
    VA_SCHMA VARCHAR(255),
    DH_DBT TIMESTAMP(3) NOT NULL,
    DH_FIN TIMESTAMP(3) NOT NULL,
    VA_THRED VARCHAR(255) NOT NULL,
    VA_FAIL TINYINT NOT NULL, --0=false,1=true
    CD_IN_REQ VARCHAR(36) NOT NULL,
    FOREIGN KEY (CD_IN_REQ) REFERENCES E_IN_REQ(ID_IN_REQ)
);

CREATE TABLE  E_DB_ACT(
    VA_TYP VARCHAR(255) NOT NULL,
    DH_DBT TIMESTAMP(3) NOT NULL,
    DH_FIN TIMESTAMP(3) NOT NULL,
    VA_FAIL  TINYINT NOT NULL,
    CD_OUT_QRY BIGINT NOT NULL,
    FOREIGN KEY (CD_OUT_QRY) REFERENCES E_OUT_QRY(ID_OUT_QRY)
);
