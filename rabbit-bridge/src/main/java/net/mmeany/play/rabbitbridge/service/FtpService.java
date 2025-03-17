package net.mmeany.play.rabbitbridge.service;

import lombok.extern.slf4j.Slf4j;
import net.mmeany.play.rabbitbridge.config.FtpConfig;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

@Service
@Slf4j
public class FtpService {

    private final FtpConfig ftpConfig;

    public FtpService(FtpConfig ftpConfig) {
        this.ftpConfig = ftpConfig;
    }

    public void sendFile(String fileName) {
        log.info("Sending file: {}", fileName);
    }

    public void receiveFile(String fileName, Path localFilePath) {
        log.info("Receiving file: {}", fileName);
        // Use Apache Commons Net for FTP operations
        try {
            // For non-secure FTP
            FTPClient ftpClient = new FTPClient();
            ftpClient.connect(ftpConfig.getHost(), ftpConfig.getPort());
            ftpClient.login(ftpConfig.getUsername(), ftpConfig.getPassword());
            ftpClient.enterLocalPassiveMode();

            // Download the file
            try (FileOutputStream fos = new FileOutputStream(localFilePath.toFile())) {
                ftpClient.retrieveFile(fileName, fos);
            }

            ftpClient.logout();
            ftpClient.disconnect();

            log.info("File {} successfully downloaded to {}", fileName, localFilePath);
        } catch (IOException e) {
            log.error("Error receiving file via FTP: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download file via FTP", e);
        }
    }
}
