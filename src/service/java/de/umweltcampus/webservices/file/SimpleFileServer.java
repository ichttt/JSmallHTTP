package de.umweltcampus.webservices.file;

import de.umweltcampus.webservices.WebserviceBase;

import java.nio.file.Paths;

public class SimpleFileServer extends WebserviceBase {

    public SimpleFileServer() {
        super(null, new FileServerModule(Paths.get("test"), "", CompressionStrategy.compressAndStore(true, true)));
    }
}
