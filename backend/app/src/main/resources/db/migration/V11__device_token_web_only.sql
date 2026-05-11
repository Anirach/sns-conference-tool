-- Now that the Flutter shell is gone, every push subscription will arrive from the web
-- app (later via Web Push, currently logging-only). Lock the device_tokens.platform column
-- to WEB so a stale ANDROID/IOS row can't be inserted again. Existing rows are zero in any
-- environment we ship to (the native platforms never made it past internal demos), so the
-- constraint adds cleanly.

ALTER TABLE device_tokens ADD CONSTRAINT chk_device_token_platform_web
    CHECK (platform = 'WEB');
