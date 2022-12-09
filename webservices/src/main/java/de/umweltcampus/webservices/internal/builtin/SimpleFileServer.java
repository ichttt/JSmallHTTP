package de.umweltcampus.webservices.internal.builtin;

import de.umweltcampus.smallhttp.header.PrecomputedHeader;
import de.umweltcampus.webservices.file.FileServerModule;
import de.umweltcampus.webservices.file.compress.CompressionStrategy;
import de.umweltcampus.webservices.internal.builtin.config.FolderInfo;
import de.umweltcampus.webservices.internal.builtin.config.SimpleFileServerConfig;
import de.umweltcampus.webservices.service.RedirectInfo;
import de.umweltcampus.webservices.service.WebserviceBase;

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
