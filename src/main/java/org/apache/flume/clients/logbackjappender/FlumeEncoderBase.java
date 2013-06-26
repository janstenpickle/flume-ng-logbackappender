package org.apache.flume.clients.logbackjappender;

/**
 * Created with IntelliJ IDEA.
 * User: chris
 * Date: 26/06/2013
 * Time: 22:07
 * To change this template use File | Settings | File Templates.
 */
import java.io.IOException;
import java.io.OutputStream;

import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import org.apache.flume.api.RpcClient;
import org.apache.flume.api.RpcClientConfigurationConstants;

abstract public class FlumeEncoderBase<E> extends ContextAwareBase implements ContextAware, LifeCycle {

    protected boolean started;

    protected String host;
    protected String targetHostName;
    protected int port;
    protected long timeout = RpcClientConfigurationConstants
            .DEFAULT_REQUEST_TIMEOUT_MILLIS;
    protected String instance;
    protected boolean avroReflectionEnabled;
    protected String avroSchemaUrl;

    protected boolean unsafeMode = false;

    public void init(String targetHostName, int port, String host,  String instance, boolean avroReflectionEnabled, String avroSchemaUrl, long timeout) {
        this.host = host;
        this.targetHostName = targetHostName;
        this.port = port;
        this.instance = instance;
        this.avroReflectionEnabled = avroReflectionEnabled;
        this.avroSchemaUrl = avroSchemaUrl;
        this.timeout = timeout;

    }

    public boolean isStarted() {
        return started;
    }

    public void start() {
        started = true;
    }

    public void stop() {
        started = false;
    }

    /**
     * Encode and write an event to the appropriate {@link OutputStream}.
     * Implementations are free to differ writing out of the encoded event and
     * instead write in batches.
     *
     * @param event
     * @throws IOException
     */
    abstract void doEncode(E event) throws IOException;

    /**
     * This method is called prior to the closing of the underling
     * {@link OutputStream}. Implementations MUST not close the underlying
     * {@link OutputStream} which is the responsibility of the owning appender.
     *
     * @throws IOException
     */
    abstract void close() throws IOException;
}  