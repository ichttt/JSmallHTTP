package de.umweltcampus.webservices.internal.builtin;

import de.umweltcampus.webservices.config.BaseServiceConfig;
import de.umweltcampus.webservices.service.InvalidConfigValueException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SimpleFileServerConfig extends BaseServiceConfig {
    public final List<FolderInfo> folderInfos;

    public SimpleFileServerConfig(String serviceIdentifier, List<FolderInfo> folderInfos) {
        super(serviceIdentifier);
        this.folderInfos = folderInfos;
    }

    @Override
    public void validateConfig() throws InvalidConfigValueException {
        if (folderInfos == null || folderInfos.isEmpty()) throw new InvalidConfigValueException("folderInfos", "Missing or empty folderInfos!");
        for (int i = 0; i < folderInfos.size(); i++) {
            FolderInfo folderInfo = folderInfos.get(i);
            if (folderInfo.pathOnDisk == null || folderInfo.pathOnDisk.isEmpty()) throw new InvalidConfigValueException("folderInfos[" + i + "]->pathOnDisk", "Missing or empty pathOnDisk!");
            Path path = Paths.get(folderInfo.pathOnDisk);
            if (!Files.isDirectory(path)) throw new InvalidConfigValueException("folderInfos[" + i + "]->pathOnDisk", "Not a valid directory!");

            if (folderInfo.prefixToServe == null) throw new InvalidConfigValueException("folderInfos[" + i + "]->prefixToServe", "Missing prefixToServe!");
        }
    }
}
