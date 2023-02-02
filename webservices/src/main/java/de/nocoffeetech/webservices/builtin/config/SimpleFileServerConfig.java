package de.nocoffeetech.webservices.builtin.config;

import de.nocoffeetech.webservices.config.service.BaseServiceConfig;
import de.nocoffeetech.webservices.service.InvalidConfigValueException;
import de.nocoffeetech.webservices.service.RedirectInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SimpleFileServerConfig extends BaseServiceConfig {
    public final List<FolderInfo> folderInfos;
    public final List<ConfigRedirectInfo> redirectInfos;
    public transient List<RedirectInfo> actualRedirectInfos;

    public SimpleFileServerConfig(String serviceIdentifier, List<FolderInfo> folderInfos, List<ConfigRedirectInfo> redirectInfos) {
        super(serviceIdentifier);
        this.folderInfos = folderInfos;
        this.redirectInfos = redirectInfos;
    }

    @Override
    public void validateConfig() throws InvalidConfigValueException {
        if (folderInfos == null || folderInfos.isEmpty()) throw new InvalidConfigValueException("folderInfos", "Missing or empty folderInfos!");
        for (int i = 0; i < folderInfos.size(); i++) {
            FolderInfo folderInfo = folderInfos.get(i);
            if (folderInfo == null) throw new InvalidConfigValueException("folderInfos[" + i + "]", "Found null folder info!");
            if (folderInfo.pathOnDisk == null || folderInfo.pathOnDisk.isBlank()) throw new InvalidConfigValueException("folderInfos[" + i + "]->pathOnDisk", "Missing or empty pathOnDisk!");
            Path path = Paths.get(folderInfo.pathOnDisk);
            if (!Files.isDirectory(path)) throw new InvalidConfigValueException("folderInfos[" + i + "]->pathOnDisk", "Not a valid directory!");

            if (folderInfo.prefixToServe == null) throw new InvalidConfigValueException("folderInfos[" + i + "]->prefixToServe", "Missing prefixToServe!");
        }

        if (redirectInfos != null && !redirectInfos.isEmpty()) {
            actualRedirectInfos = new ArrayList<>();
            for (int i = 0; i < redirectInfos.size(); i++) {
                ConfigRedirectInfo currentRedirectInfo = redirectInfos.get(i);
                if (currentRedirectInfo == null) throw new InvalidConfigValueException("redirectInfos[" + i + "]", "Found null redirect info!");
                if (currentRedirectInfo.to == null || currentRedirectInfo.to.isBlank()) throw new InvalidConfigValueException("redirectInfos[" + i + "]->to", "Missing or empty to");
                if (currentRedirectInfo.from == null || currentRedirectInfo.from.isBlank()) throw new InvalidConfigValueException("redirectInfos[" + i + "]->from", "Missing or empty from");
                RedirectInfo newInfo;
                try {
                    if (currentRedirectInfo.permanent)
                        newInfo = RedirectInfo.newPermanentRedirect(currentRedirectInfo.from, currentRedirectInfo.to);
                    else
                        newInfo = RedirectInfo.newTempRedirect(currentRedirectInfo.from, currentRedirectInfo.to);
                } catch (Exception e) {
                    throw new InvalidConfigValueException("redirectInfos[" + i + "]", "Failed to parse message: " + e.getMessage());
                }
                actualRedirectInfos.add(newInfo);
            }
        }
    }
}
