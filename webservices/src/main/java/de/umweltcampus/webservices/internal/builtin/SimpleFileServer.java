package de.umweltcampus.webservices.internal.builtin;

import de.umweltcampus.smallhttp.header.PrecomputedHeader;
import de.umweltcampus.webservices.file.FileServerModule;
import de.umweltcampus.webservices.file.compress.CompressionStrategy;
import de.umweltcampus.webservices.internal.builtin.config.FolderInfo;
import de.umweltcampus.webservices.internal.builtin.config.SimpleFileServerConfig;
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
            final PrecomputedHeader[] headers;
            if (folderInfo.additionalHeaders != null) {
                headers = folderInfo.additionalHeaders.entrySet().stream().map(header -> PrecomputedHeader.create(header.getKey(), header.getValue())).toArray(PrecomputedHeader[]::new);
            } else {
                headers = new PrecomputedHeader[0];
            }
            modules[i] = new FileServerModule(Paths.get(folderInfo.pathOnDisk), folderInfo.prefixToServe, CompressionStrategy.compressAndStore(true, folderInfo.precompress), name, (httpRequest, headerWriter) -> {
                for (PrecomputedHeader header : headers) {
                    headerWriter.addHeader(header);
                }
            });
        }
        return new SimpleFileServer(name, modules);
    }
}