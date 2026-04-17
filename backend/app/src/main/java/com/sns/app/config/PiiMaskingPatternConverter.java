package com.sns.app.config;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sns.identity.app.PiiScrubber;

/**
 * Logback classic pattern converter used as {@code %piiMsg} to emit the message with PII redacted.
 */
public class PiiMaskingPatternConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String msg = event.getFormattedMessage();
        return msg == null ? null : PiiScrubber.mask(msg);
    }
}
