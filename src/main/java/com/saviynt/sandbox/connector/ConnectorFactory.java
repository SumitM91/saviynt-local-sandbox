package com.saviynt.sandbox.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Factory for routing and instantiating TargetConnector implementations based on connectiontype.
 */
@Component
public class ConnectorFactory {

    private static final Logger log = LoggerFactory.getLogger(ConnectorFactory.class);

    private final Map<String, TargetConnector> connectors = new HashMap<>();
    private final TargetConnector defaultConnector;

    public ConnectorFactory(
            RestTargetConnector restTargetConnector,
            ActiveDirectoryConnector activeDirectoryConnector,
            LdapTargetConnector ldapTargetConnector,
            JdbcTargetConnector jdbcTargetConnector
    ) {
        this.defaultConnector = restTargetConnector;

        connectors.put("REST", restTargetConnector);
        connectors.put("ACTIVEDIRECTORY", activeDirectoryConnector);
        connectors.put("ACTIVE DIRECTORY", activeDirectoryConnector);
        connectors.put("AD", activeDirectoryConnector);
        connectors.put("LDAP", ldapTargetConnector);
        connectors.put("JDBC", jdbcTargetConnector);
    }

    /**
     * Resolves the appropriate TargetConnector based on connectiontype string.
     *
     * @param connectionType target connection type ('REST', 'ActiveDirectory', 'AD', 'LDAP', 'JDBC')
     * @return matching TargetConnector
     */
    public TargetConnector getConnector(String connectionType) {
        if (connectionType == null || connectionType.isBlank()) {
            log.info("Null or empty connectionType provided, falling back to REST target connector.");
            return defaultConnector;
        }

        String normalizedKey = connectionType.trim().toUpperCase(Locale.ROOT);
        TargetConnector connector = connectors.get(normalizedKey);

        if (connector != null) {
            return connector;
        }

        log.warn("Unrecognized connection type '{}', defaulting to REST connector.", connectionType);
        return defaultConnector;
    }
}
