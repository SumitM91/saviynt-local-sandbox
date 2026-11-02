package com.saviynt.sandbox.service;

import com.saviynt.sandbox.dto.CorrelationJobResponse;
import com.saviynt.sandbox.dto.OrphanAccountResponse;
import com.saviynt.sandbox.dto.TerminatedViolationResponse;
import com.saviynt.sandbox.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Module3CorrelationServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private Module3CorrelationService correlationService;

    @Test
    @DisplayName("getOrphanAccounts: Should return list of orphan accounts when present")
    void testGetOrphanAccounts_WithData() {
        List<OrphanAccountResponse> mockOrphans = List.of(
                new OrphanAccountResponse("AWS_Production", 503L, "admin_backdoor_aws", null, "Service"),
                new OrphanAccountResponse("AWS_Production", 504L, "pnair_aws_unlinked", "EMP003", "User")
        );
        when(accountRepository.findOrphanAccounts()).thenReturn(mockOrphans);

        List<OrphanAccountResponse> result = correlationService.getOrphanAccounts();

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().accountName()).isEqualTo("admin_backdoor_aws");
        verify(accountRepository, times(1)).findOrphanAccounts();
    }

    @Test
    @DisplayName("getOrphanAccounts: Should return empty list when no orphans exist")
    void testGetOrphanAccounts_Empty() {
        when(accountRepository.findOrphanAccounts()).thenReturn(Collections.emptyList());

        List<OrphanAccountResponse> result = correlationService.getOrphanAccounts();

        assertThat(result).isEmpty();
        verify(accountRepository, times(1)).findOrphanAccounts();
    }

    @Test
    @DisplayName("runAccountCorrelationJob: Should return response with count of correlated accounts")
    void testRunAccountCorrelationJob_Success() {
        when(accountRepository.runAccountCorrelation()).thenReturn(2);

        CorrelationJobResponse response = correlationService.runAccountCorrelationJob();

        assertThat(response.jobName()).isEqualTo("AccountCorrelationJob");
        assertThat(response.newlyCorrelatedAccounts()).isEqualTo(2);
        assertThat(response.message()).contains("customproperty1");
        verify(accountRepository, times(1)).runAccountCorrelation();
    }

    @Test
    @DisplayName("runAccountCorrelationJob: Edge case when 0 accounts are correlated")
    void testRunAccountCorrelationJob_ZeroCorrelated() {
        when(accountRepository.runAccountCorrelation()).thenReturn(0);

        CorrelationJobResponse response = correlationService.runAccountCorrelationJob();

        assertThat(response.newlyCorrelatedAccounts()).isZero();
        verify(accountRepository, times(1)).runAccountCorrelation();
    }

    @Test
    @DisplayName("getTerminatedUsersWithActiveAccounts: Should return list of violations")
    void testGetTerminatedUsersWithActiveAccounts_WithViolations() {
        List<TerminatedViolationResponse> mockViolations = List.of(
                new TerminatedViolationResponse("rahul.verma", 0, "rahul.verma.aws", "AWS_Production", 1)
        );
        when(accountRepository.findTerminatedUsersWithActiveAccounts()).thenReturn(mockViolations);

        List<TerminatedViolationResponse> result = correlationService.getTerminatedUsersWithActiveAccounts();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().terminatedUser()).isEqualTo("rahul.verma");
        assertThat(result.getFirst().accountStatus()).isEqualTo(1);
        verify(accountRepository, times(1)).findTerminatedUsersWithActiveAccounts();
    }

    @Test
    @DisplayName("getTerminatedUsersWithActiveAccounts: Should return empty list when compliant")
    void testGetTerminatedUsersWithActiveAccounts_Compliant() {
        when(accountRepository.findTerminatedUsersWithActiveAccounts()).thenReturn(Collections.emptyList());

        List<TerminatedViolationResponse> result = correlationService.getTerminatedUsersWithActiveAccounts();

        assertThat(result).isEmpty();
        verify(accountRepository, times(1)).findTerminatedUsersWithActiveAccounts();
    }
}
