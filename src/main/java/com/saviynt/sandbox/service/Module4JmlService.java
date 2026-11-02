package com.saviynt.sandbox.service;

import com.saviynt.sandbox.dto.*;
import com.saviynt.sandbox.exception.ResourceNotFoundException;
import com.saviynt.sandbox.repository.AccountRepository;
import com.saviynt.sandbox.repository.ArsTaskRepository;
import com.saviynt.sandbox.repository.EndpointRepository;
import com.saviynt.sandbox.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Service for Module 4: JML Lifecycle Engine (Joiner, Mover, Leaver).
 */
@Service
public class Module4JmlService {

    private static final Logger log = LoggerFactory.getLogger(Module4JmlService.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final EndpointRepository endpointRepository;
    private final ArsTaskRepository arsTaskRepository;

    public Module4JmlService(UserRepository userRepository,
                             AccountRepository accountRepository,
                             EndpointRepository endpointRepository,
                             ArsTaskRepository arsTaskRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.endpointRepository = endpointRepository;
        this.arsTaskRepository = arsTaskRepository;
    }

    /**
     * 1. JOINER: Onboards identity and generates birthright account and entitlement tasks.
     */
    @Transactional
    public JoinerResponse processJoiner(JoinerRequest request) {
        log.info("SAVIYNT JML SERVICE: Processing Joiner lifecycle request...");

        String baseUsername = (request != null && request.username() != null && !request.username().isBlank())
                ? request.username().trim()
                : "alex.smith";

        String username = baseUsername;
        if (userRepository.existsByUsername(username)) {
            username = baseUsername + "." + (System.currentTimeMillis() % 10000);
        }

        String firstname = (request != null && request.firstname() != null && !request.firstname().isBlank())
                ? request.firstname()
                : "Alex";
        String lastname = (request != null && request.lastname() != null && !request.lastname().isBlank())
                ? request.lastname()
                : "Smith";
        String email = (request != null && request.email() != null && !request.email().isBlank())
                ? request.email()
                : username + "@hcl.com";
        String departmentname = (request != null && request.departmentname() != null && !request.departmentname().isBlank())
                ? request.departmentname()
                : "Engineering";
        String employeetype = (request != null && request.employeetype() != null && !request.employeetype().isBlank())
                ? request.employeetype()
                : "Full-Time";
        String manager = (request != null && request.manager() != null && !request.manager().isBlank())
                ? request.manager()
                : "vikram.boss";
        Long orgkey = (request != null && request.orgkey() != null)
                ? request.orgkey()
                : 1L;
        String employeeId = (request != null && request.employeeId() != null && !request.employeeId().isBlank())
                ? request.employeeId()
                : "EMP" + (int) (1000 + Math.random() * 9000);
        int statuskey = 1;

        // Step 1: Create User
        long userKey = userRepository.createUser(
                username, firstname, lastname, email,
                departmentname, employeetype, manager, orgkey, statuskey, employeeId
        );

        // Step 2: Determine Target Endpoint
        var endpointOpt = endpointRepository.findFirstEndpoint();
        long endpointKey = endpointOpt.map(EndpointRepository.EndpointEntity::endpointkey).orElse(10L);
        String endpointName = endpointOpt.map(EndpointRepository.EndpointEntity::endpointname).orElse("AWS_Production");

        // Step 3: Insert TaskType 3 (New Account)
        long task3Key = arsTaskRepository.createTask(
                3,
                "Create Target Account (" + endpointName + ")",
                userKey,
                null,
                endpointKey,
                0,
                "Saviynt Birthright Engine: Automated account creation for Full-Time " + departmentname + " employee"
        );

        // Step 4: Insert TaskType 1 (Add Access - Birthright)
        long task1Key = arsTaskRepository.createTask(
                1,
                "Add Access - Birthright: " + departmentname + "_Base_Role",
                userKey,
                null,
                endpointKey,
                0,
                "Saviynt Birthright Engine: Automated baseline role assignment for " + departmentname
        );

        log.info("SAVIYNT JML SERVICE: User created [{}] with TaskType 3 [{}] and TaskType 1 [{}]", username, task3Key, task1Key);

        ArsTaskDto task3Dto = new ArsTaskDto(
                task3Key, 3, "New Account",
                "Create Target Account (" + endpointName + ")",
                null, null, endpointName, "0 (Pending)",
                "Automated birthright account creation"
        );

        ArsTaskDto task1Dto = new ArsTaskDto(
                task1Key, 1, "Add Access - Birthright",
                "Add Access - Birthright: " + departmentname + "_Base_Role",
                null, null, endpointName, "0 (Pending)",
                "Automated baseline role assignment: " + departmentname + "_Base_Role"
        );

        return new JoinerResponse(
                "JOINER",
                userKey,
                username,
                firstname,
                lastname,
                email,
                departmentname,
                employeetype,
                employeeId,
                statuskey,
                List.of(task3Dto, task1Dto),
                "User onboarding simulated successfully. Saviynt Birthright Engine generated TaskType 3 (New Account) and TaskType 1 (Add Access)."
        );
    }

    /**
     * 2. MOVER: Updates department and evaluates access delta.
     */
    @Transactional
    public MoverResponse processMover(MoverRequest request) {
        log.info("SAVIYNT JML SERVICE: Processing Mover lifecycle request...");

        String username = (request != null && request.username() != null && !request.username().isBlank())
                ? request.username().trim()
                : "rahul.verma";

        String newDepartment = (request != null && request.newDepartment() != null && !request.newDepartment().isBlank())
                ? request.newDepartment().trim()
                : "Engineering";

        String newManager = (request != null && request.newManager() != null && !request.newManager().isBlank())
                ? request.newManager().trim()
                : null;

        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findFirstUser();
            if (userOpt.isEmpty()) {
                throw new ResourceNotFoundException("No users found to simulate Mover lifecycle.");
            }
        }

        var user = userOpt.get();
        long userKey = user.userkey();
        String currentUsername = user.username();
        String oldDepartment = user.departmentname();
        String oldManager = user.manager();

        userRepository.updateDepartmentAndManager(userKey, newDepartment, newManager);

        String moverLogMessage = String.format(
                "SAVIYNT MOVER ENGINE: User '%s' (userkey=%d) transferred from '%s' to '%s'. " +
                        "Mover Access Delta Evaluated: 1) Previous department ('%s') entitlements flagged for Manager Recertification / Auto-Revocation. " +
                        "2) New department ('%s') birthright policies queued for delta provisioning.",
                currentUsername, userKey, oldDepartment, newDepartment, oldDepartment, newDepartment
        );

        log.info(moverLogMessage);

        return new MoverResponse(
                "MOVER",
                userKey,
                currentUsername,
                oldDepartment,
                newDepartment,
                oldManager,
                newManager != null ? newManager : oldManager,
                moverLogMessage,
                "User department updated successfully. Saviynt Mover delta access evaluation triggered and logged."
        );
    }

    /**
     * 3. LEAVER: Terminates user, disables target accounts, and generates revocation tasks.
     */
    @Transactional
    public LeaverResponse processLeaver(LeaverRequest request) {
        log.info("SAVIYNT JML SERVICE: Processing Leaver termination request...");

        String username = (request != null && request.username() != null && !request.username().isBlank())
                ? request.username().trim()
                : "sumit.mishra";

        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findFirstActiveUserWithLinkedAccounts();
            if (userOpt.isEmpty()) {
                throw new ResourceNotFoundException("No active user found with linked accounts to simulate termination.");
            }
        }

        var user = userOpt.get();
        long userKey = user.userkey();
        String targetUsername = user.username();

        // 1. Terminate User (statuskey = 0)
        userRepository.terminateUser(userKey);

        // 2. Fetch linked accounts
        var linkedAccounts = accountRepository.findAccountsByUserKey(userKey);
        List<ArsTaskDto> taskDtos = new ArrayList<>();

        for (var account : linkedAccounts) {
            long accountKey = account.accountkey();
            String accountName = account.accountname();
            Long endpointKey = account.endpointkey();
            String endpointName = account.endpointname() != null ? account.endpointname() : "Endpoint-" + endpointKey;

            // Generate TaskType 12: Disable Account
            long task12Key = arsTaskRepository.createTask(
                    12,
                    "Disable Target Account: " + accountName,
                    userKey,
                    accountKey,
                    endpointKey,
                    0,
                    "Saviynt Leaver Deprovisioning: User terminated (statuskey=0) -> Disable account " + accountName
            );

            // Generate TaskType 2: Remove Access
            long task2Key = arsTaskRepository.createTask(
                    2,
                    "Revoke Access / Entitlements: " + accountName,
                    userKey,
                    accountKey,
                    endpointKey,
                    0,
                    "Saviynt Leaver Deprovisioning: User terminated (statuskey=0) -> Revoke all entitlements on " + endpointName
            );

            // Mark Account as Disabled (status = 2)
            accountRepository.updateAccountStatus(accountKey, 2);

            taskDtos.add(new ArsTaskDto(
                    task12Key, 12, "Disable Account",
                    "Disable Target Account: " + accountName,
                    accountKey, accountName, endpointName, "0 (Pending)",
                    "Account disabled due to termination"
            ));

            taskDtos.add(new ArsTaskDto(
                    task2Key, 2, "Remove Access",
                    "Revoke Access / Entitlements: " + accountName,
                    accountKey, accountName, endpointName, "0 (Pending)",
                    "Revoke all entitlements"
            ));
        }

        log.info("SAVIYNT LEAVER SERVICE: User '{}' (userkey={}) terminated. Generated {} deprovisioning tasks across {} linked accounts.",
                targetUsername, userKey, taskDtos.size(), linkedAccounts.size());

        return new LeaverResponse(
                "LEAVER",
                userKey,
                targetUsername,
                0,
                "Terminated / Inactive",
                linkedAccounts.size(),
                taskDtos,
                "Leaver termination processed. User status set to 0 and TaskType 12 (Disable Account) and TaskType 2 (Remove Access) queued in arstasks."
        );
    }

    /**
     * 4. AUDIT: Get all ARS tasks queue.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllArsTasks() {
        return arsTaskRepository.findAllTasksWithDetails();
    }
}
