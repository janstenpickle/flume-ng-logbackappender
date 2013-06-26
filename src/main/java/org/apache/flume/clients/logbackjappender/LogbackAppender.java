package org.apache.flume.clients.logbackjappender;

import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.spi.LogbackLock;
import ch.qos.logback.core.status.ErrorStatus;
import org.apache.flume.api.RpcClient;
import org.apache.flume.api.RpcClientConfigurationConstants;
import org.apache.flume.api.RpcClientFactory;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: chris
 * Date: 26/06/2013
 * Time: 20:24
 * To change this template use File | Settings | File Templates.
 */
public class LogbackAppender<E> extends UnsynchronizedAppenderBase<E> {

    private String targetHostname;
    private String host;
    private String instance;
    private int port = 0;
    private boolean unsafeMode = false;
    private long timeout = RpcClientConfigurationConstants
            .DEFAULT_REQUEST_TIMEOUT_MILLIS;
    private boolean avroReflectionEnabled;
    private String avroSchemaUrl;

    protected LogbackLock lock = new LogbackLock();
    protected Encoder<E> encoder;




    @Override
    public void start() {

        if (name == null) {
            throw new RuntimeException("No name provided for Appender");
        }


        if (targetHostname == null) {
            throw new RuntimeException("No targetHostname provided for Appender");
        }

        if (host == null) {
            throw new RuntimeException("No host provided for Appender");
        }

        if (instance == null) {
            throw new RuntimeException("No instance provided for Appender");
        }

        if (port == 0) {
            throw new RuntimeException("No port provided for Appender");
        }



        this.encoder = new FlumeEncoder<E>(targetHostname, port, host, instance, avroReflectionEnabled, avroSchemaUrl, timeout);
        encoder.start();
        super.start();
    }

    @Override
    public void stop() {
        synchronized (lock) {
            encoder.stop();
            super.stop();
        }
    }

    @Override
    protected void append(E event) {
        if (!isStarted()) {
            return;
        }
        subAppend(event);
    }

    protected void subAppend(E event) {
        if (!isStarted()) {
            return;
        }
        try {
            // this step avoids LBCLASSIC-139
            if (event instanceof DeferredProcessingAware) {
                ((DeferredProcessingAware) event).prepareForDeferredProcessing();
            }
            // the synchronization prevents the OutputStream from being closed while we
            // are writing. It also prevents multiple threads from entering the same
            // converter. Converters assume that they are in a synchronized block.
            synchronized (lock) {
                writeOut(event);
            }
        } catch (IOException ioe) {
            // as soon as an exception occurs, move to non-started state
            // and add a single ErrorStatus to the SM.
            this.started = false;
            addStatus(new ErrorStatus("IO failure in appender", this, ioe));
        }
    }

    protected void writeOut(E event) throws IOException {
        this.encoder.doEncode(event);
    }
}
