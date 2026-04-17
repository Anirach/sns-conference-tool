package com.sns.app.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import com.sns.identity.app.PiiScrubber;
import java.io.IOException;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.fieldnames.LogstashFieldNames;

/**
 * Replaces the default logstash-encoder "message" field with a PII-scrubbed variant.
 * Plugs into {@code logback-spring.xml} via
 * {@code <provider class="com.sns.app.config.PiiMaskingMessageJsonProvider"/>}.
 */
public class PiiMaskingMessageJsonProvider
        extends AbstractFieldJsonProvider<ILoggingEvent>
        implements FieldNamesAware<LogstashFieldNames> {

    public static final String FIELD_MESSAGE = "message";

    public PiiMaskingMessageJsonProvider() {
        setFieldName(FIELD_MESSAGE);
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        String formatted = event.getFormattedMessage();
        String masked = formatted == null ? null : PiiScrubber.mask(formatted);
        generator.writeStringField(getFieldName(), masked);
    }

    @Override
    public void setFieldNames(LogstashFieldNames fieldNames) {
        setFieldName(fieldNames.getMessage());
    }
}
