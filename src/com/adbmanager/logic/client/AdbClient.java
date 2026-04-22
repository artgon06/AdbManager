package com.adbmanager.logic.client;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class AdbClient {

    private final AdbExecutableResolver adbExecutableResolver;
    private final Duration timeout;

    public AdbClient(String adbPath, Duration timeout) {
        this(() -> adbPath, timeout);
    }

    public AdbClient(AdbExecutableResolver adbExecutableResolver, Duration timeout) {
        this.adbExecutableResolver = Objects.requireNonNull(adbExecutableResolver);
        this.timeout = Objects.requireNonNull(timeout);
    }

    public AdbResult run(List<String> args) throws Exception {
        AdbBinaryResult result = runBinary(args);
        return new AdbResult(result.exitCode(), new String(result.output(), StandardCharsets.UTF_8));
    }

    public AdbResult run(List<String> args, String standardInput) throws Exception {
        AdbBinaryResult result = runBinary(
                args,
                standardInput == null ? null : standardInput.getBytes(StandardCharsets.UTF_8));
        return new AdbResult(result.exitCode(), new String(result.output(), StandardCharsets.UTF_8));
    }

    public AdbResult runForSerial(String serial, List<String> args) throws Exception {
        return run(withSerial(serial, args));
    }

    public AdbResult runStreaming(List<String> args, Consumer<String> outputConsumer) throws Exception {
        AdbBinaryResult result = runBinaryStreaming(args, null, outputConsumer, null);
        return new AdbResult(result.exitCode(), new String(result.output(), StandardCharsets.UTF_8));
    }

    public AdbResult runForSerialStreaming(String serial, List<String> args, Consumer<String> outputConsumer)
            throws Exception {
        return runStreaming(withSerial(serial, args), outputConsumer);
    }

    public AdbResult runForSerialStreaming(
            String serial,
            List<String> args,
            Consumer<String> outputConsumer,
            AdbExecutionControl executionControl) throws Exception {
        AdbBinaryResult result = runBinaryStreaming(withSerial(serial, args), null, outputConsumer, executionControl);
        return new AdbResult(result.exitCode(), new String(result.output(), StandardCharsets.UTF_8));
    }

    public AdbBinaryResult runBinary(List<String> args) throws Exception {
        return runBinary(args, null);
    }

    public AdbBinaryResult runBinary(List<String> args, byte[] standardInput) throws Exception {
        return runBinaryStreaming(args, standardInput, null, null);
    }

    public AdbBinaryResult runBinaryStreaming(List<String> args, byte[] standardInput, Consumer<String> outputConsumer)
            throws Exception {
        return runBinaryStreaming(args, standardInput, outputConsumer, null);
    }

    public AdbBinaryResult runBinaryStreaming(
            List<String> args,
            byte[] standardInput,
            Consumer<String> outputConsumer,
            AdbExecutionControl executionControl) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(resolveAdbExecutable());
        command.addAll(args);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AdbExecutionControl control = executionControl == null ? new AdbExecutionControl() : executionControl;
        Future<byte[]> outputFuture = null;
        control.markActivity();

        try {
            if (standardInput != null && standardInput.length > 0) {
                process.getOutputStream().write(standardInput);
                control.markActivity();
            }
            process.getOutputStream().flush();
            process.getOutputStream().close();

            outputFuture = executor.submit(() -> readOutput(process.getInputStream(), outputConsumer, control));
            long timeoutNanos = timeout.toNanos();
            boolean finished = false;
            while (!finished) {
                finished = process.waitFor(250L, TimeUnit.MILLISECONDS);
                if (finished) {
                    break;
                }

                if (control.isCancelled()) {
                    process.destroyForcibly();
                    return new AdbBinaryResult(-2, appendSuffix(getOutputBytes(outputFuture), "\n[CANCELED]"));
                }

                long inactiveNanos = System.nanoTime() - control.lastActivityNanos();
                if (inactiveNanos > timeoutNanos) {
                    process.destroyForcibly();
                    return new AdbBinaryResult(-1, appendSuffix(getOutputBytes(outputFuture), "\n[TIMEOUT]"));
                }
            }

            return new AdbBinaryResult(process.exitValue(), outputFuture.get());
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            return new AdbBinaryResult(-2, appendSuffix(getOutputBytes(outputFuture), "\n[CANCELED]"));
        } finally {
            executor.shutdownNow();
        }
    }

    public AdbBinaryResult runBinaryForSerial(String serial, List<String> args) throws Exception {
        return runBinary(withSerial(serial, args));
    }

    private List<String> withSerial(String serial, List<String> args) {
        List<String> command = new ArrayList<>();
        command.add("-s");
        command.add(serial);
        command.addAll(args);
        return command;
    }

    private String resolveAdbExecutable() throws Exception {
        String adbPath = adbExecutableResolver.resolveExecutable();
        if (adbPath == null || adbPath.isBlank()) {
            throw new IllegalStateException("No se ha podido resolver la ruta de adb.");
        }
        return adbPath;
    }

    private byte[] readOutput(InputStream inputStream, Consumer<String> outputConsumer, AdbExecutionControl executionControl)
            throws Exception {
        try (InputStream stream = inputStream) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                if (outputConsumer != null && bytesRead > 0) {
                    outputConsumer.accept(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
                }
                if (bytesRead > 0) {
                    executionControl.markActivity();
                }
            }
            return outputStream.toByteArray();
        }
    }

    private byte[] appendSuffix(byte[] output, String suffix) {
        byte[] base = output == null ? new byte[0] : output;
        byte[] suffixBytes = suffix.getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[base.length + suffixBytes.length];
        System.arraycopy(base, 0, combined, 0, base.length);
        System.arraycopy(suffixBytes, 0, combined, base.length, suffixBytes.length);
        return combined;
    }

    private byte[] getOutputBytes(Future<byte[]> outputFuture) throws InterruptedException {
        if (outputFuture == null) {
            return new byte[0];
        }
        try {
            return outputFuture.get(2, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            return new byte[0];
        }
    }
}
