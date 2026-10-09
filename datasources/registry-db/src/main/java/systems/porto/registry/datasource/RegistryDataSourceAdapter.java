package systems.porto.registry.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.porto.adapter.Adapter;
import systems.porto.adapter.config.Config;
import systems.porto.api.db.DatabaseMigrator;
import systems.porto.api.spi.HostContext;
import systems.porto.api.spi.HostContextConstants;
import systems.porto.context.Context;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Registers a named {@link DataSource} into the host context under {@code datasources.{id}}.
 *
 * <p>Config ({@code plugins/datasources/{id}-{env}.yaml}):
 * <ul>
 *   <li>{@code datasource.id} — context key (default: plugin id)</li>
 *   <li>{@code datasource.source} — {@code spring} (use host Spring bean) or {@code hikari}</li>
 *   <li>For hikari: {@code jdbc.url}, {@code jdbc.username}, {@code jdbc.password}, {@code jdbc.driver}</li>
 * </ul>
 */
public class RegistryDataSourceAdapter implements Adapter<Context> {
    private static final Logger logger = LoggerFactory.getLogger(RegistryDataSourceAdapter.class);

    private Config config;
    private Context context;
    private HikariDataSource ownedDataSource;

    @Override
    public String id() {
        return "registry";
    }

    @Override
    public void init(final Context context) {
        this.context = context;
        String configName = id() + "-" + context.getEnvironment() + ".yaml";
        this.config = readConfig(context, configName);

        if (!(context instanceof HostContext hostContext)) {
            throw new IllegalStateException("HostContext is required for datasource plugin " + id());
        }

        String datasourceId = config.getConfigValue("datasource.id").orElse(id());
        DataSource dataSource = resolveDataSource(hostContext);
        hostContext.setVariable(HostContextConstants.datasourceKey(datasourceId), dataSource);
        config.getConfigValue("liquibase.change-log").ifPresent(changeLog ->
            DatabaseMigrator.migrate(
                dataSource,
                changeLog,
                context.getHomeDirectory()
            )
        );
        logger.info("Registered datasource '{}' (source={})", datasourceId,
            config.getConfigValue("datasource.source").orElse("spring"));
    }

    @Override
    public void finish() {
        if (ownedDataSource != null && !ownedDataSource.isClosed()) {
            ownedDataSource.close();
        }
    }

    private DataSource resolveDataSource(final HostContext hostContext) {
        String source = config.getConfigValue("datasource.source").orElse("spring");
        if ("hikari".equalsIgnoreCase(source)) {
            return createHikariDataSource();
        }
        DataSource springDataSource = resolveSpringDataSource(hostContext);
        if (springDataSource != null) {
            return springDataSource;
        }
        if (config.getConfigValue("jdbc.url").isPresent()) {
            logger.warn("Spring DataSource not available; falling back to Hikari for {}", id());
            return createHikariDataSource();
        }
        throw new IllegalStateException(
            "No DataSource available for plugin " + id() + " (datasource.source=" + source + ")"
        );
    }

    private DataSource resolveSpringDataSource(final HostContext hostContext) {
        Object applicationContext = hostContext.getVariable(HostContextConstants.CONTEXT_APPLICATION_CONTEXT)
            .orElse(null);
        if (applicationContext == null) {
            return null;
        }
        try {
            Method getBean = applicationContext.getClass().getMethod("getBean", Class.class);
            Object bean = getBean.invoke(applicationContext, DataSource.class);
            if (bean instanceof DataSource dataSource) {
                return dataSource;
            }
        } catch (ReflectiveOperationException ex) {
            logger.debug("Could not resolve Spring DataSource bean: {}", ex.toString());
        }
        return null;
    }

    private DataSource createHikariDataSource() {
        String url = config.getConfigValue("jdbc.url")
            .orElseThrow(() -> new IllegalStateException("jdbc.url is required for hikari datasource " + id()));
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(config.getConfigValue("jdbc.username").orElse(""));
        hikariConfig.setPassword(config.getConfigValue("jdbc.password").orElse(""));
        config.getConfigValue("jdbc.driver").ifPresent(hikariConfig::setDriverClassName);
        hikariConfig.setPoolName("porto-" + id());
        ownedDataSource = new HikariDataSource(hikariConfig);
        return ownedDataSource;
    }

    private Config readConfig(final Context context, final String configFileName) {
        Path path = resolveConfigPath(context, configFileName);
        if (!Files.exists(path)) {
            logger.warn("Configuration file: {} not found", path);
            return new Config(List.of());
        }
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(path.toFile(), Config.class);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read configuration file: " + path, ex);
        }
    }

    private Path resolveConfigPath(final Context context, final String configFileName) {
        if (context instanceof HostContext hostContext) {
            String application = hostContext.getApplication();
            if (application != null && !application.isBlank()) {
                Path appScoped = Paths.get(
                    context.getHomeDirectory(),
                    "plugins",
                    "datasources",
                    application,
                    configFileName
                );
                if (Files.exists(appScoped)) {
                    return appScoped;
                }
            }
        }
        return Paths.get(context.getHomeDirectory(), "plugins", "datasources", configFileName);
    }
}
