package com.udacity.webcrawler.profiler;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;
import javax.inject.Inject;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

    private final Clock clock;
    private final ProfilingState state = new ProfilingState();
    private final ZonedDateTime startTime;

    @Inject
    ProfilerImpl(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        this.startTime = ZonedDateTime.now(clock);
    }

    @Override
    public <T> T wrap(Class<T> klass, T delegate) {
        Objects.requireNonNull(klass);

        if (!klass.isInterface()) {
            throw new IllegalArgumentException(
                klass.getSimpleName() + "is unbale to find this interface"
            );
        }

        boolean hasProfiledMethod = Arrays.stream(klass.getMethods()).anyMatch(
            method -> method.isAnnotationPresent(Profiled.class)
        );

        if (!hasProfiledMethod) {
            throw new IllegalArgumentException(
                klass.getSimpleName() + "Profiled method does not exist"
            );
        }

        Objects.requireNonNull(delegate);

        var handler = new ProfilingMethodInterceptor(clock, delegate, state);

        Object proxy = Proxy.newProxyInstance(
            klass.getClassLoader(),
            new Class<?>[] { klass },
            handler
        );

        return klass.cast(proxy);
    }

    @Override
    public void writeData(Path path) {
        try (
            BufferedWriter writer = Files.newBufferedWriter(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            )
        ) {
            writeData(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeData(Writer writer) throws IOException {
        writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
        writer.write(System.lineSeparator());
        state.write(writer);
        writer.write(System.lineSeparator());
    }
}
