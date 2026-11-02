package com.eve.app.telegram.qr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Native host binding only. Each Robolectric sandbox owns its loaded library. */
@DoNotInstrument
final class QrBackdropHostBridge {
    static {
        try {
            Path source = Path.of(System.getProperty("eve.qr.backdrop.hostLibrary"));
            Path copy = Files.createTempFile("eve-qr-backdrop-", "." + source.getFileName());
            Files.copy(source, copy, StandardCopyOption.REPLACE_EXISTING);
            copy.toFile().deleteOnExit();
            System.load(copy.toAbsolutePath().toString());
        } catch (IOException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }
    static native void generate(Object bitmap, Object ring, float cell, int count,
                                float[] geometry, int[] colors, float[] wave);
}
