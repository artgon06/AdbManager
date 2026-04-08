package com.adbmanager.logic.client;

import java.io.InputStream;
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

public class AdbClient {

    private final String adbPath;
    private final Duration timeout;

    public AdbClient(String adbPath, Duration timeout) {
        this.adbPath = Objects.requireNonNull(adbPath);
        this.timeout = Objects.requireNonNull(timeout);
    }

    public AdbResult run(List<String> args) throws Exception {
        AdbBinaryResult result = runBinary(args);
        return new AdbResult(result.exitCode(), new String(result.output(), StandardCharsets.UTF_8));
    }

    public AdbResult runForSerial(String serial, List<String> args) throws Exception {
        return run(withSerial(serial, args));
    }

    public AdbBinaryResult runBinary(List<String> args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(adbPath);
        command.addAll(args);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<byte[]> outputFuture = executor.submit(() -> readOutput(process.getInputStream()));
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                byte[] partialOutput = getOutputBytes(outputFuture);
                byte[] timeoutSuffix = "\n[TIMEOUT]".getBytes(StandardCharsets.UTF_8);
                byte[] timeoutOutput = new byte[partialOutput.length + timeoutSuffix.length];
                System.arraycopy(partialOutput, 0, timeoutOutput, 0, partialOutput.length);
                System.arraycopy(timeoutSuffix, 0, timeoutOutput, partialOutput.length, timeoutSuffix.length);
                return new AdbBinaryResult(-1, timeoutOutput);
            }

            return new AdbBinaryResult(process.exitValue(), outputFuture.get());
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

    private byte[] readOutput(InputStream inputStream) throws Exception {
        try (InputStream stream = inputStream) {
            return stream.readAllBytes();
        }
    }

    private byte[] getOutputBytes(Future<byte[]> outputFuture) throws InterruptedException {
        try {
            return outputFuture.get(2, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            return new byte[0];
        }
    }
}
