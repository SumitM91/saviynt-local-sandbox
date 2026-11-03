-- ==========================================
-- MODULE 1: IDENTITY REPOSITORY (USERS & ORGS)
-- ==========================================
CREATE TABLE IF NOT EXISTS orgs (
                                    orgkey BIGINT PRIMARY KEY AUTO_INCREMENT,
                                    orgname VARCHAR(100) UNIQUE NOT NULL, -- Vendor / Department Organization
    customproperty1 VARCHAR(100)          -- Sponsor Username for Contractors
    );

CREATE TABLE IF NOT EXISTS users (
                                     userkey BIGINT PRIMARY KEY AUTO_INCREMENT,
                                     username VARCHAR(100) UNIQUE NOT NULL,
    firstname VARCHAR(100),
    lastname VARCHAR(100),
    email VARCHAR(150),
    departmentname VARCHAR(100),
    employeetype VARCHAR(50),             -- 'Full-Time', 'Contractor', 'Service'
    manager VARCHAR(100),                 -- Manager's username
    orgkey BIGINT,                        -- Linked Organization
    statuskey INT DEFAULT 1,              -- Saviynt Standard: 1 = Active, 0 = Inactive/Terminated
    startdate DATE,
    termdate DATE,
    customproperty1 VARCHAR(100),         -- Employee ID (Used in Correlation!)
    createdate DATETIME DEFAULT CURRENT_TIMESTAMP
    );

-- ==========================================
-- MODULE 2: 3-LAYER APPLICATION ARCHITECTURE
-- (Connection -> Security System -> Endpoint)
-- ==========================================
CREATE TABLE IF NOT EXISTS connections (
                                           connectionkey BIGINT PRIMARY KEY AUTO_INCREMENT,
                                           connectionname VARCHAR(100) UNIQUE NOT NULL,
    connectiontype VARCHAR(50),           -- 'REST', 'AD', 'JDBC', 'SOAP'
    connectionjson TEXT,                  -- Auth URL, ClientID, Secret
    createaccountjson TEXT,               -- Provisioning payload for new account
    addaccessjson TEXT,                   -- Provisioning payload for role assignment
    accountimportjson TEXT                -- Payload to pull accounts from target
    );

CREATE TABLE IF NOT EXISTS securitysystems (
                                               systemkey BIGINT PRIMARY KEY AUTO_INCREMENT,
                                               systemname VARCHAR(100) UNIQUE NOT NULL,
    connectionkey BIGINT,                 -- Links to Connection
    accessaddworkflow VARCHAR(100),       -- Workflow name for requests
    provisioningtries INT DEFAULT 3,      -- Max retries for WSRETRY job
    FOREIGN KEY (connectionkey) REFERENCES connections(connectionkey)
    );

CREATE TABLE IF NOT EXISTS endpoints (
                                         endpointkey BIGINT PRIMARY KEY AUTO_INCREMENT,
                                         endpointname VARCHAR(100) UNIQUE NOT NULL,
    systemkey BIGINT,                     -- Links to Security System
    accountnamerule VARCHAR(255),         -- Rule to generate target login ID
    correlationrule VARCHAR(255),         -- Rule to match Account to User
    preprovisiongroovy TEXT,              -- Groovy script before account creation
    FOREIGN KEY (systemkey) REFERENCES securitysystems(systemkey)
    );

-- ==========================================
-- TARGET ACCOUNTS & CORRELATION BRIDGE
-- ==========================================
CREATE TABLE IF NOT EXISTS accounts (
                                        accountkey BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        name VARCHAR(100) NOT NULL,           -- Target login ID (e.g., sAMAccountName)
    endpointkey BIGINT,
    status INT DEFAULT 1,                 -- Saviynt Standard: 1 = Active, 2 = Disabled, 0 = Deleted
    accounttype VARCHAR(50) DEFAULT 'User', -- 'User', 'Service', 'Firefighter'
    customproperty1 VARCHAR(100),         -- Target Employee ID imported from app
    lastlogondate DATETIME,
    FOREIGN KEY (endpointkey) REFERENCES endpoints(endpointkey)
    );

CREATE TABLE IF NOT EXISTS user_accounts (
                                             userkey BIGINT,
                                             accountkey BIGINT,
                                             PRIMARY KEY (userkey, accountkey),
    FOREIGN KEY (userkey) REFERENCES users(userkey),
    FOREIGN KEY (accountkey) REFERENCES accounts(accountkey)
    );

-- ==========================================
-- MODULE 4, 5 & 6: ARS TASKS & WORKFLOW ENGINE
-- ==========================================
CREATE TABLE IF NOT EXISTS arstasks (
    taskkey BIGINT PRIMARY KEY AUTO_INCREMENT,
    tasktype INT NOT NULL,                -- Saviynt Standard: 1 = Add Access, 2 = Remove Access, 3 = New Account, 12 = Disable Account
    taskdescription VARCHAR(255),
    userkey BIGINT,
    accountkey BIGINT,
    endpointkey BIGINT,
    status INT DEFAULT 0,                 -- Saviynt Standard: 0 = Pending/Assigned, 1 = Completed/Provisioning Ready, -1 = Rejected, 2 = Failed
    comments TEXT,
    createdate DATETIME DEFAULT CURRENT_TIMESTAMP,
    workflow_stage VARCHAR(50) DEFAULT 'MANAGER_PENDING',
    approver_comments TEXT,
    FOREIGN KEY (userkey) REFERENCES users(userkey),
    FOREIGN KEY (accountkey) REFERENCES accounts(accountkey),
    FOREIGN KEY (endpointkey) REFERENCES endpoints(endpointkey)
    );
