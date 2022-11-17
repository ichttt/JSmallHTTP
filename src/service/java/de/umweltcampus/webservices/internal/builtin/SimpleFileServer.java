package de.umweltcampus.webservices.internal.builtin;

import de.umweltcampus.webservices.file.CompressionStrategy;
import de.umweltcampus.webservices.file.FileServerModule;
import de.umweltcampus.webservices.service.WebserviceBase;

import java.nio.file.Paths;

public class SimpleFileServer extends WebserviceBase {

    private SimpleFileServer(String name, FileServerModule[] modules) {
        super(name, null, modules);
    }

    public static SimpleFileServer create(SimpleFileServerConfig config, String name) {
        FileServerModule[] modules = new FileServerModule[config.folderInfos.size()];
        for (int i = 0; i < config.folderInfos.size(); i++) {
            FolderInfo folderInfo = config.folderInfos.get(i);
            modules[i] = new FileServerModule(Paths.get(folderInfo.pathOnDisk), folderInfo.prefixToServe, CompressionStrategy.compressAndStore(true, folderInfo.precompress), name);
        }
        return new SimpleFileServer(name, modules);
    }
}
