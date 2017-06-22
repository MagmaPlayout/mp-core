package playoutCore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class manages the user defined configurations.
 * The configuration file is expected to be in ~/.magma-playout.conf
 * (It's used by mp-melted-status project as well. TODO: put this class inside it's own jar)
 * 
 * @author rombus
 */
public class ConfigurationManager {
    private static ConfigurationManager instance;

    private static final String CONFIG_PATH = System.getProperty("user.home")+File.separator+".magma-playout.conf";

    private static final String REDIS_HOST_KEY = "redis_server_hostname";
    private static final String REDIS_PORT_KEY = "redis_server_port";
    private static final String REDIS_PCCP_CHANNEL_KEY = "redis_pccp_channel";
    private static final String REDIS_FSCP_CHANNEL_KEY = "redis_fscp_channel";
    private static final String REDIS_PCR_CHANNEL_KEY = "redis_pcr_channel";
    private static final String REDIS_RECONNECTION_TIMEOUT_KEY = "redis_reconnection_timeout";

    private static final String MELTED_HOST_KEY = "melted_server_hostname";
    private static final String MELTED_PORT_KEY = "melted_server_port";
    private static final String MELTED_RECONNECTION_TIMEOUT_KEY = "melted_reconnection_timeout";
    private static final String MELTED_RECONNECTION_TRIES_KEY = "melted_reconnection_tries";

    private static final String MELT_PATH_KEY = "melt_path";
    private static final String FILTER_SERVER_URL_KEY = "filter_server_hostname";

    private static final String BASH_TIMEOUT_KEY = "bash_timeout_ms";

    private static final String REST_BASE_URL = "rest_base_url";

    private Properties properties;

    private ConfigurationManager(){
    }

    public static ConfigurationManager getInstance(){
        if(instance==null){
            instance = new ConfigurationManager();
        }
        return instance;
    }

    /**
     * Reads the configuration file into a Properties object.
     * If the file doesn't exists it creates one with default values.
     * If there are IO errors, a warning is logged and the application
     * continues by using the default values.
     *
     * @param logger The application logger
     */
    public void init(Logger logger){
        properties = setDefaultValues(new Properties());
        boolean ioError = false;

        try (FileInputStream configFile = new FileInputStream(CONFIG_PATH)) {
            properties.load(configFile);
        }
        catch (FileNotFoundException e){
            try (FileOutputStream out = new FileOutputStream(CONFIG_PATH)) {
                logger.log(Level.WARNING, "Configuration file not found at {0}. Creating it with default values.", CONFIG_PATH);
                properties.store(out, "Magma Playout Configuration File");
            }
            catch (IOException ex) {
                ioError = true;
            }
        }
        catch (IOException ex) {
            ioError = true;
        }

        if(ioError){
            logger.log(Level.WARNING, "Failed reading/writing the configuration file. Continuing with default values.");
        }
    }

    /**
     * Default configuration values are added here.
     *
     * @param p Properties object where to load the default values
     * @return Returns the Properties object received as an argument for convenience
     */
    private Properties setDefaultValues(Properties p){
        p.setProperty(REDIS_HOST_KEY, "localhost");
        p.setProperty(REDIS_PORT_KEY, "6379");
        p.setProperty(REDIS_PCCP_CHANNEL_KEY, "PCCP");
        p.setProperty(REDIS_FSCP_CHANNEL_KEY, "FSCP");
        p.setProperty(REDIS_PCR_CHANNEL_KEY, "PCR");
        p.setProperty(REDIS_RECONNECTION_TIMEOUT_KEY, "1000");

        p.setProperty(MELTED_HOST_KEY, "localhost");
        p.setProperty(MELTED_PORT_KEY, "5250");
        p.setProperty(MELTED_RECONNECTION_TIMEOUT_KEY, "1000");
        p.setProperty(MELTED_RECONNECTION_TRIES_KEY, "0");

        p.setProperty(MELT_PATH_KEY, "/usr/bin/melt/melt");
        p.setProperty(FILTER_SERVER_URL_KEY, "http://localhost:3001/filter-banner.html");
        p.setProperty(BASH_TIMEOUT_KEY, "5000");

        p.setProperty(REST_BASE_URL, "localhost:8001/api/");

        return p;
    }

    
    public String getRedisHost(){
        return properties.getProperty(REDIS_HOST_KEY);
    }
    
    public int getRedisPort(){
        return Integer.parseInt(properties.getProperty(REDIS_PORT_KEY));
    }

    public String getRedisPccpChannel(){
        return properties.getProperty(REDIS_PCCP_CHANNEL_KEY);
    }

    public String getRedisFscpChannel(){
        return properties.getProperty(REDIS_FSCP_CHANNEL_KEY);
    }

    public String getRedisPcrChannel(){
        return properties.getProperty(REDIS_PCR_CHANNEL_KEY);
    }

    public int getRedisReconnectionTimeout(){
        return Integer.parseInt(properties.getProperty(REDIS_RECONNECTION_TIMEOUT_KEY));
    }

    public String getMeltedHost(){
        return properties.getProperty(MELTED_HOST_KEY);
    }

    public int getMeltedPort(){
        return Integer.parseInt(properties.getProperty(MELTED_PORT_KEY));
    }

    public int getMeltedReconnectionTimeout(){
        return Integer.parseInt(properties.getProperty(MELTED_RECONNECTION_TIMEOUT_KEY));
    }

    public int getMeltedReconnectionTries(){
        return Integer.parseInt(properties.getProperty(MELTED_RECONNECTION_TRIES_KEY));
    }

    public String getMeltPath(){
        return properties.getProperty(MELT_PATH_KEY);
    }

    public String getFilterServerHost(){
        return properties.getProperty(FILTER_SERVER_URL_KEY);
    }
    public int getMeltXmlTimeout(){
        return Integer.parseInt(properties.getProperty(BASH_TIMEOUT_KEY));
    }

    public String getRestBaseUrl(){
        return properties.getProperty(REST_BASE_URL);
    }
}
