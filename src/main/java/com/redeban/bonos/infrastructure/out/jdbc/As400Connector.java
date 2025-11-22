package com.redeban.bonos.infrastructure.out.jdbc;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redeban.bonos.domain.exception.BonoTechnicalException;
import com.redeban.bonos.infrastructure.camel.common.configs.As400DataSourceProperties;

@Startup
@ApplicationScoped
public class As400Connector {

    private static final Logger LOGGER = LoggerFactory.getLogger(As400Connector.class);

    private final As400DataSourceProperties dataSourceProperties;
    private final AtomicBoolean driverLoaded = new AtomicBoolean(false);
    private volatile boolean enabled;

    @Inject
    public As400Connector(As400DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }

    @PostConstruct
    void init() {
        Optional<String> url = sanitize(dataSourceProperties.url());
        Optional<String> user = sanitize(dataSourceProperties.user());
        Optional<String> password = sanitize(dataSourceProperties.password());

        if (url.isEmpty() || user.isEmpty() || password.isEmpty()) {
            enabled = false;
            LOGGER.warn("Datasource AS400 no configurado completamente (url/user/password). El servicio seguirá disponible pero las operaciones devolverán error hasta completar la configuración.");
            return;
        }

        try {
            if (driverLoaded.compareAndSet(false, true)) {
                Class.forName(dataSourceProperties.driver());
                dataSourceProperties.loginTimeout().ifPresent(DriverManager::setLoginTimeout);
            }
            enabled = true;
            LOGGER.info("Datasource AS400 configurado correctamente (url={} user={})", mask(url.get()), user.get());
            testConnection(url.get(), user.get(), password.get());
        } catch (ClassNotFoundException e) {
            enabled = false;
            throw new BonoTechnicalException("No se pudo cargar el driver JDBC AS400", 500, e);
        }
    }

    public String execute(String storedProcedure, String payload) {
        if (!enabled) {
            throw new BonoTechnicalException("Datasource AS400 no configurado. Verifique bonos.datasource.*", 503);
        }

        try (Connection connection = DriverManager.getConnection(
                sanitize(dataSourceProperties.url()).orElseThrow(),
                sanitize(dataSourceProperties.user()).orElseThrow(),
                sanitize(dataSourceProperties.password()).orElseThrow());
             CallableStatement statement = connection.prepareCall("call " + storedProcedure)) {

            statement.setString(1, payload);
            statement.registerOutParameter(2, Types.VARCHAR);
            statement.execute();
            return statement.getString(2);

        } catch (SQLException ex) {
            throw new BonoTechnicalException("Error ejecutando stored procedure AS400: " + ex.getMessage(), 500, ex);
        }
    }

    private void testConnection(String url, String user, String password) {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1 FROM SYSIBM.SYSDUMMY1")) {

            if (resultSet.next()) {
                LOGGER.info("Prueba de conectividad AS400 exitosa (SELECT 1 FROM SYSIBM.SYSDUMMY1 -> {})", resultSet.getInt(1));
            } else {
                LOGGER.warn("Prueba de conectividad AS400 no devolvió resultados (SELECT 1 FROM SYSIBM.SYSDUMMY1).");
            }

        } catch (SQLException ex) {
            LOGGER.warn("La prueba de conectividad AS400 falló: {}", ex.getMessage());
        }
    }

    private String mask(String value) {
        if (value == null || value.length() <= 6) {
            return "****";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 3);
    }

    private Optional<String> sanitize(Optional<String> value) {
        return value.filter(v -> v != null && !v.isBlank());
    }
}
