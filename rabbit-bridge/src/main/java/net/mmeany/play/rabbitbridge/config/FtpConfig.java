package net.mmeany.play.rabbitbridge.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Component
@ConfigurationProperties(prefix = "ftp")
public class FtpConfig {
    private String host;
    private int port;
    private String username;
    private String password;
    private String remoteDirectory;
    private String localDirectory;
    private String filename;
    private String cron;
    private boolean enabled;
}