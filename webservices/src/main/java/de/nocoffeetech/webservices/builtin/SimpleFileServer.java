package de.nocoffeetech.webservices.builtin;

import de.nocoffeetech.webservices.file.FileServerModule;
import de.nocoffeetech.webservices.file.compress.CompressionStrategy;
import de.nocoffeetech.webservices.service.RedirectInfo;
import de.nocoffeetech.webservices.service.WebserviceBase;
import de.nocoffeetech.smallhttp.header.PrecomputedHeader;
import de.nocoffeetech.webservices.builtin.config.FolderInfo;
import de.nocoffeetech.webservices.builtin.config.SimpleFileServerConfig;

import java.nio.file.Paths;

public class SimpleFileServer extends WebserviceBase {

    private SimpleFileServer(String name) {
        super(name);
    }

    public static SimpleFileServer create(SimpleFileServerConfig config, String name) {
        SimpleFileServer simpleFileServer = new SimpleFileServer(name);
        FileServerModule[] modules = new FileServerModule[config.folderInfos.size()];
        for (int i = 0; i < config.folderInfos.size(); i++) {
            FolderInfo folderInfo = config.folderInfos.get(i);
            final PrecomputedHeader[] headers;
            if (folderInfo.additionalHeaders != null) {
                headers = folderInfo.additionalHeaders.entrySet().stream().map(header -> PrecomputedHeader.create(header.getKey(), header.getValue())).toArray(PrecomputedHeader[]::new);
            } else {
                headers = new PrecomputedHeader[0];
            }
            modules[i] = new FileServerModule(Paths.get(folderInfo.pathOnDisk), folderInfo.prefixToServe, CompressionStrategy.compressAndStore(true, folderInfo.precompress), simpleFileServer, (httpRequest, headerWriter) -> {
                for (PrecomputedHeader header : headers) {
                    headerWriter.addHeader(header);
                }
            });
        }
        simpleFileServer.setFileServers(modules);
        if (config.actualRedirectInfos != null)
            simpleFileServer.setRedirectInfos(config.actualRedirectInfos.toArray(RedirectInfo[]::new));
        return simpleFileServer;
    }
}
