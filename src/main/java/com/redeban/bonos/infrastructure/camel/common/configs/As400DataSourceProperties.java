package com.redeban.bonos.infrastructure.camel.common.configs;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.util.Optional;

@ConfigMapping(prefix = "bonos.datasource")
public interface As400DataSourceProperties {

    Optional<String> url();

    Optional<String> user();

    Optional<String> password();

    @WithDefault("com.ibm.as400.access.AS400JDBCDriver")
    String driver();

    Optional<Integer> loginTimeout();
}
