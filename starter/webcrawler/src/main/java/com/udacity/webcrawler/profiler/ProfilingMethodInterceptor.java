package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

    private final Clock clock;
    private final Object delegate;
    private final ProfilingState state;

    // TODO: You will need to add more instance fields and constructor arguments to this class.
    ProfilingMethodInterceptor(
        Clock clock,
        Object delegate,
        ProfilingState state
    ) {
        this.clock = Objects.requireNonNull(clock);
        this.delegate = Objects.requireNonNull(delegate);
        this.state = Objects.requireNonNull(state);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable {
        if (
            method.getDeclaringClass() == Object.class &&
            method.getName().equals("equals")
        ) {
            return method.invoke(delegate, args);
        }

        Instant start = clock.instant();

        try {
            Object result = method.invoke(delegate, args);

            if (method.isAnnotationPresent(Profiled.class)) {
                Instant end = clock.instant();

                state.record(
                    delegate.getClass(),
                    method,
                    Duration.between(start, end)
                );
            }

            return result;
        } catch (InvocationTargetException e) {
            if (method.isAnnotationPresent(Profiled.class)) {
                Instant end = clock.instant();

                state.record(
                    delegate.getClass(),
                    method,
                    Duration.between(start, end)
                );
            }

            throw e.getCause();
        }
    }
}
