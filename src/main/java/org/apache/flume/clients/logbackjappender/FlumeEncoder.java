package org.apache.flume.clients.logbackjappender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.flume.Event;
import org.apache.flume.FlumeException;
import org.apache.flume.api.RpcClient;
import org.apache.flume.api.RpcClientConfigurationConstants;
import org.apache.flume.api.RpcClientFactory;
import org.apache.flume.event.EventBuilder;
import org.apache.log4j.helpers.LogLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: chris
 * Date: 26/06/2013
 * Time: 20:35
 * To change this template use File | Settings | File Templates.
 */
public class FlumeEncoder<E> extends FlumeEncoderBase<E> {

    protected RpcClient rpcClient;

    private void activate() {
        Properties props = new Properties();
        props.setProperty(RpcClientConfigurationConstants.CONFIG_HOSTS, "h1");
        props.setProperty(RpcClientConfigurationConstants.CONFIG_HOSTS_PREFIX + "h1",
                targetHostName + ":" + port);
        props.setProperty(RpcClientConfigurationConstants.CONFIG_CONNECT_TIMEOUT,
                String.valueOf(timeout));
        props.setProperty(RpcClientConfigurationConstants.CONFIG_REQUEST_TIMEOUT,
                String.valueOf(timeout));
        try {
            rpcClient = RpcClientFactory.getInstance(props);
        } catch (FlumeException e) {
            String errormsg = "RPC client creation failed! " +
                    e.getMessage();
            LogLog.error(errormsg);
            if (unsafeMode) {
                return;
            }
            throw e;
        }
    }


    private void deactivate() {
        // Any append calls after this will result in an Exception.
        if (rpcClient != null) {
            try {
                rpcClient.close();
            } catch (FlumeException ex) {
                LogLog.error("Error while trying to close RpcClient.", ex);
                if (unsafeMode) {
                    return;
                }
                throw ex;
            } finally {
                rpcClient = null;
            }
        } else {
            String errorMsg = "Flume logbackjappender already closed!";
            LogLog.error(errorMsg);
            if (unsafeMode) {
                return;
            }
            throw new FlumeException(errorMsg);
        }
    }


    @Override
    public void doEncode(E e) throws IOException {
        if (e instanceof ILoggingEvent) {

            //If rpcClient is null, it means either this appender object was never
            //setup by setting hostname and port and then calling activateOptions
            //or this appender object was closed by calling close(), so we throw an
            //exception to show the appender is no longer accessible.
            if (rpcClient == null) {
                String errorMsg = "Cannot Append to Appender! Appender either closed or" +
                        " not setup correctly!";
                LogLog.error(errorMsg);
                if (unsafeMode) {
                    return;
                }
                throw new FlumeException(errorMsg);
            }

            if (!rpcClient.isActive()) {
                reconnect();
            }

            ILoggingEvent event = (ILoggingEvent) e;
            Map<String, String> mdc = event.getMDCPropertyMap();

            Map<String, String> hdrs = new HashMap<String, String>();
            hdrs.put(LogbackAvroHeaders.LOGGER_NAME.toString(), event.getLoggerName());
            // hdrs.put(LogbackAvroHeaders.LOGGER_CLASS.toString(), event.);
            hdrs.put(LogbackAvroHeaders.TIMESTAMP.toString(),
                    String.valueOf(event.getTimeStamp()));
            hdrs.put(LogbackAvroHeaders.THREAD.toString(), event.getThreadName());


            //To get the level back simply use
            //LoggerEvent.toLevel(hdrs.get(Integer.parseInt(
            //LogbackAvroHeaders.LOG_LEVEL.toString()))
            hdrs.put(LogbackAvroHeaders.LOG_LEVEL.toString(),
                    String.valueOf(event.getLevel().toInt()));
            hdrs.put(LogbackAvroHeaders.LOG_LEVEL_STRING.toString(), event.getLevel().toString());
            hdrs.put(LogbackAvroHeaders.MESSAGE_ENCODING.toString(), "UTF8");
            if (host != null) {
                hdrs.put(LogbackAvroHeaders.HOST.toString(), host);
            }
            if (instance != null) {
                hdrs.put(LogbackAvroHeaders.INSTANCE.toString(), instance);
            }


            //Map the MDC to flume headers
            for (String key : mdc.keySet()) {
                hdrs.put(LogbackAvroHeaders.OTHER + "." + key, mdc.get(key));
            }

            Event flumeEvent;
            Object message = event.getMessage();
            if (message instanceof GenericRecord) {
                GenericRecord record = (GenericRecord) message;
                populateAvroHeaders(hdrs, record.getSchema(), message);
                flumeEvent = EventBuilder.withBody(serialize(record, record.getSchema()), hdrs);
            } else if (message instanceof SpecificRecord || avroReflectionEnabled) {
                Schema schema = ReflectData.get().getSchema(message.getClass());
                populateAvroHeaders(hdrs, schema, message);
                flumeEvent = EventBuilder.withBody(serialize(message, schema), hdrs);
            } else {
                hdrs.put(LogbackAvroHeaders.MESSAGE_ENCODING.toString(), "UTF8");
                String msg = message.toString();
                flumeEvent = EventBuilder.withBody(msg, Charset.forName("UTF8"), hdrs);
            }

            try {
                rpcClient.append(flumeEvent);
            } catch (Exception ex) {
                String msg = "Flume append() failed.";
                //      rpcClient = flumeErrorHandler.handle(flumeEvent);
                //throw new FlumeException(msg + " Exception follows.", e);
            }

        } else {

        }
    }


    private Schema schema;
    private ByteArrayOutputStream out;
    private DatumWriter<Object> writer;
    private BinaryEncoder encoder;

    protected void populateAvroHeaders(Map<String, String> hdrs, Schema schema,
                                       Object message) {
        if (avroSchemaUrl != null) {
            hdrs.put(LogbackAvroHeaders.AVRO_SCHEMA_URL.toString(), avroSchemaUrl);
            return;
        }
        LogLog.warn("Cannot find ID for schema. Adding header for schema, " +
                "which may be inefficient. Consider setting up an Avro Schema Cache.");
        hdrs.put(LogbackAvroHeaders.AVRO_SCHEMA_LITERAL.toString(), schema.toString());
    }

    private byte[] serialize(Object datum, Schema datumSchema) throws FlumeException {
        if (schema == null || !datumSchema.equals(schema)) {
            schema = datumSchema;
            out = new ByteArrayOutputStream();
            writer = new ReflectDatumWriter<Object>(schema);
            encoder = EncoderFactory.get().binaryEncoder(out, null);
        }
        out.reset();
        try {
            writer.write(datum, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new FlumeException(e);
        }
    }

    @Override
    public void start() {
        activate();
        super.start();
    }

    @Override
    public void stop() {
        deactivate();
        super.stop();
    }

    @Override
    public void close() throws IOException {
        stop();
    }

    private void reconnect() throws IOException {
        deactivate();
        activate();
    }


}
