-- 1. Organization (Vendor / Internal)
INSERT IGNORE INTO orgs (orgkey, orgname, customproperty1) VALUES
(1, 'HCL_Engineering_Org', 'sumit.mishra'),
(2, 'TechVendor_Contractors', 'sumit.mishra');

-- 2. Users (Identity Repository)
-- Notice statuskey: 1 = Active, 0 = Terminated (Rahul Verma resigned!)
INSERT IGNORE INTO users (userkey, username, firstname, lastname, email, departmentname, employeetype, manager, orgkey, statuskey, customproperty1) VALUES
(101, 'sumit.mishra', 'Sumit', 'Mishra', 'sumit@hcl.com', 'Engineering', 'Full-Time', 'vikram.boss', 1, 1, 'EMP001'),
(102, 'rahul.verma', 'Rahul', 'Verma', 'rahul@hcl.com', 'Finance', 'Full-Time', 'vikram.boss', 1, 0, 'EMP002'),
(103, 'priya.nair', 'Priya', 'Nair', 'priya@vendor.com', 'Engineering', 'Contractor', 'sumit.mishra', 2, 1, 'EMP003');

-- 3. Layer 1: Connection
INSERT IGNORE INTO connections (connectionkey, connectionname, connectiontype, connectionjson, createaccountjson) VALUES
(1, 'AWS_REST_Conn', 'REST', '{"authType":"oauth2","url":"http://localhost:8080/mock-target/oauth"}', '{"username":"${user.username}","empId":"${user.customproperty1}"}');

-- 4. Layer 2: Security System
INSERT IGNORE INTO securitysystems (systemkey, systemname, connectionkey, accessaddworkflow, provisioningtries) VALUES
(1, 'AWS_SecSystem', 1, 'Manager_Approval_Workflow', 3);

-- 5. Layer 3: Endpoint (With Account Name Rule & Correlation Rule)
INSERT IGNORE INTO endpoints (endpointkey, endpointname, systemkey, accountnamerule, correlationrule) VALUES
(10, 'AWS_Production', 1, '${user.firstname}.${user.lastname}.aws', 'users.customproperty1 = accounts.customproperty1');

-- 6. Target Accounts (Imported from AWS)
INSERT IGNORE INTO accounts (accountkey, name, endpointkey, status, accounttype, customproperty1, lastlogondate) VALUES
(501, 'sumit.mishra.aws', 10, 1, 'User', 'EMP001', NOW()),
(502, 'rahul.verma.aws', 10, 1, 'User', 'EMP002', NOW()),
(503, 'admin_backdoor_aws', 10, 1, 'Service', NULL, NOW()),
(504, 'pnair_aws_unlinked', 10, 1, 'User', 'EMP003', NOW());

-- 7. Existing Correlation Bridge (Only 501 and 502 are linked right now!)
-- 503 (admin_backdoor_aws) and 504 (pnair_aws_unlinked) are NOT in user_accounts yet!
INSERT IGNORE INTO user_accounts (userkey, accountkey) VALUES
(101, 501),
(102, 502);
