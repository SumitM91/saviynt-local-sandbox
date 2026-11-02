package com.saviynt.sandbox.service;

import com.saviynt.sandbox.dto.CorrelationJobResponse;
import com.saviynt.sandbox.dto.OrphanAccountResponse;
import com.saviynt.sandbox.dto.TerminatedViolationResponse;
import com.saviynt.sandbox.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Enterprise Service for Module 3: Account Correlation & Orphan Analytics Engine.
 */
@Service
public class Module3CorrelationService {

    private static final Logger log = LoggerFactory.getLogger(Module3CorrelationService.class);

    private final AccountRepository accountRepository;

    public Module3CorrelationService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public List<OrphanAccountResponse> getOrphanAccounts() {
        log.info("Executing Saviynt Analytics V2: Querying orphan/uncorrelated accounts");
        return accountRepository.findOrphanAccounts();
    }

    @Transactional
    public CorrelationJobResponse runAccountCorrelationJob() {
        log.info("Executing Saviynt Account Correlation Engine: Correlating accounts via Employee ID (customproperty1)");
        int newlyCorrelatedCount = accountRepository.runAccountCorrelation();
        log.info("Correlation job finished. Correlated accounts count: {}", newlyCorrelatedCount);

        return new CorrelationJobResponse(
                "AccountCorrelationJob",
                newlyCorrelatedCount,
                "Accounts with matching Employee ID (customproperty1) have been linked in user_accounts!"
        );
    }

    @Transactional(readOnly = true)
    public List<TerminatedViolationResponse> getTerminatedUsersWithActiveAccounts() {
        log.info("Executing Saviynt Compliance Audit: Checking for terminated users with active accounts");
        return accountRepository.findTerminatedUsersWithActiveAccounts();
    }
}
