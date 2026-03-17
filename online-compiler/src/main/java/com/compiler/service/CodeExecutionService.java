package com.compiler.service;

import com.compiler.model.CodeRequest;
import com.compiler.model.CodeResponse;
import com.compiler.model.Submission;
import com.compiler.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * ============================================================
 * CodeExecutionService  —  NO DOCKER VERSION
 * ============================================================
 * Runs code directly using compilers on your machine.
 *
 *  Language | What is needed
 *  ---------|-----------------------------
 *  Java     | Already works — IntelliJ has the JDK ✅
 *  C / C++  | Install MinGW on Windows (free, one installer)
 *
 * Flow per request:
 *   1. Write source code to a temp file
 *   2. Run the compiler (javac / gcc / g++)
 *   3. Run the compiled binary, pipe stdin
 *   4. Capture output, enforce 10-second timeout
 *   5. Delete temp files
 *   6. Save result to MySQL
 * ============================================================
 */
@Service
public class CodeExecutionService {

    private static final int TIMEOUT_SECONDS = 10;

    @Autowired
    private SubmissionRepository submissionRepository;

    // Compiler paths — change these in application.properties if needed
    // Windows MinGW example: compiler.gcc.path=C:/MinGW/bin/gcc.exe
    @Value("${compiler.gcc.path:gcc}")
    private String gccPath;

    @Value("${compiler.gpp.path:g++}")
    private String gppPath;

    // ==================================================================
    // PUBLIC ENTRY POINT
    // ==================================================================

    public CodeResponse executeCode(CodeRequest request) {
        long start = System.currentTimeMillis();

        String lang      = request.getLanguage().toLowerCase().trim();
        String code      = request.getSourceCode();
        String input     = request.getInputData() != null ? request.getInputData() : "";

        if (!List.of("c", "cpp", "java").contains(lang)) {
            return errorResponse("Unsupported language: " + lang + ". Use: c, cpp, java", 0);
        }

        CodeResponse response;
        try {
            response = runCode(lang, code, input);
        } catch (Exception e) {
            response = errorResponse("Internal error: " + e.getMessage(), 0);
        }

        response.setExecutionTimeMs(System.currentTimeMillis() - start);
        response.setSubmissionId(saveToDb(request, response));
        return response;
    }

    // ==================================================================
    // STEP 1: Create temp dir, write file, compile, run
    // ==================================================================

    private CodeResponse runCode(String lang, String sourceCode, String inputData)
            throws Exception {

        // Create a unique temp folder, e.g. /tmp/compiler_a1b2c3d4/
        Path tempDir = Files.createTempDirectory(
            "compiler_" + UUID.randomUUID().toString().substring(0, 8));

        try {
            // Write source file
            String filename = getSourceFilename(lang);
            Files.writeString(tempDir.resolve(filename), sourceCode, StandardCharsets.UTF_8);

            // Compile
            ExecResult compileResult = compile(lang, filename, tempDir);
            if (!compileResult.success) {
                // Compilation failed — return error with compiler message
                return new CodeResponse("", compileResult.text, "ERROR", 0, null);
            }

            // Run
            return runBinary(lang, tempDir, inputData, compileResult.text);

        } finally {
            deleteDir(tempDir); // always clean up
        }
    }

    // ==================================================================
    // STEP 2: COMPILE
    // ==================================================================

    private ExecResult compile(String lang, String filename, Path dir) throws Exception {
        String[] cmd = compileCommand(lang, filename);
        if (cmd == null) return new ExecResult(true, ""); // Java — compiled at run time

        ProcessOutput out = execute(cmd, dir, "", 30);
        return new ExecResult(out.exitCode == 0, out.text);
    }

    /** Build the compile command array for the given language */
    private String[] compileCommand(String lang, String filename) {
        return switch (lang) {
            case "c"    -> new String[]{ gccPath, filename, "-o", "main_out", "-Wall" };
            case "cpp"  -> new String[]{ gppPath, filename, "-o", "main_out", "-Wall", "-std=c++17" };
            case "java" -> new String[]{ getJavacPath(), filename };
            default     -> null;
        };
    }

    // ==================================================================
    // STEP 3: RUN
    // ==================================================================

    private CodeResponse runBinary(String lang, Path dir,
                                   String inputData, String warnings) throws Exception {
        String[] cmd = runCommand(lang, dir);
        ProcessOutput out = execute(cmd, dir, inputData, TIMEOUT_SECONDS);

        if (out.timedOut) {
            return new CodeResponse(
                "",
                "Execution timed out after " + TIMEOUT_SECONDS + " seconds.\n"
                + "Tip: Check for infinite loops.",
                "TIMEOUT", (long) TIMEOUT_SECONDS * 1000, null
            );
        }

        if (out.exitCode != 0) {
            String errMsg = "Runtime Error (exit code " + out.exitCode + ")\n" + out.text;
            if (!warnings.isBlank()) errMsg += "\n\nCompiler Warnings:\n" + warnings;
            return new CodeResponse(out.text, errMsg, "ERROR", 0, null);
        }

        return new CodeResponse(out.text, warnings, "SUCCESS", 0, null);
    }

    /** Build the run command for the given language */
    private String[] runCommand(String lang, Path dir) {
        boolean win = isWindows();
        return switch (lang) {
            case "c", "cpp" -> win
                ? new String[]{ dir.resolve("main_out.exe").toString() }
                : new String[]{ dir.resolve("main_out").toString() };
            case "java" -> new String[]{ getJavaPath(), "-cp", dir.toString(), "Main" };
            default -> new String[]{ "echo", "unknown language" };
        };
    }

    // ==================================================================
    // PROCESS EXECUTOR — runs a command, captures output, enforces timeout
    // ==================================================================

    private ProcessOutput execute(String[] cmd, Path workDir,
                                  String stdinText, int timeoutSec) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true); // merge stdout + stderr

        Process proc = pb.start();

        // Write stdin input to the process
        try (OutputStream os = proc.getOutputStream()) {
            if (stdinText != null && !stdinText.isBlank()) {
                os.write(stdinText.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            // Always close stdin — otherwise the program waits forever
        }

        // Read output in a background thread to prevent blocking
        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<String> future = ex.submit(() -> {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                    if (sb.length() > 500_000) { // 500KB safety limit
                        sb.append("[Output truncated]");
                        break;
                    }
                }
            }
            return sb.toString();
        });

        try {
            String text = future.get(timeoutSec, TimeUnit.SECONDS);
            int exitCode = proc.waitFor();
            return new ProcessOutput(text, exitCode, false);

        } catch (TimeoutException e) {
            proc.destroyForcibly();
            future.cancel(true);
            return new ProcessOutput("", -1, true);
        } finally {
            ex.shutdownNow();
        }
    }

    // ==================================================================
    // JAVA PATH DETECTION
    // Uses the same JDK that IntelliJ/Maven already uses, so
    // Java compilation works without any extra setup.
    // ==================================================================

    private String getJavacPath() {
        String javaHome = System.getProperty("java.home");
        String exe = isWindows() ? "javac.exe" : "javac";

        // JDK 9+: java.home = JDK root
        Path p = Paths.get(javaHome, "bin", exe);
        if (Files.exists(p)) return p.toString();

        // JDK 8: java.home = JDK/jre
        p = Paths.get(javaHome, "..", "bin", exe);
        if (Files.exists(p)) return p.normalize().toString();

        return "javac"; // fallback to PATH
    }

    private String getJavaPath() {
        String javaHome = System.getProperty("java.home");
        String exe = isWindows() ? "java.exe" : "java";
        Path p = Paths.get(javaHome, "bin", exe);
        if (Files.exists(p)) return p.toString();
        return "java";
    }

    // ==================================================================
    // DATABASE
    // ==================================================================

    private Long saveToDb(CodeRequest req, CodeResponse resp) {
        try {
            Submission s = new Submission();
            s.setLanguage(req.getLanguage());
            s.setSourceCode(req.getSourceCode());
            s.setInputData(req.getInputData() != null ? req.getInputData() : "");
            s.setOutputData(resp.getOutput());
            s.setStatus(resp.getStatus());
            return submissionRepository.save(s).getId();
        } catch (Exception e) {
            System.err.println("DB save warning: " + e.getMessage());
            return null;
        }
    }

    // ==================================================================
    // UTILITIES
    // ==================================================================

    private String getSourceFilename(String lang) {
        return switch (lang) {
            case "c"    -> "main.c";
            case "cpp"  -> "main.cpp";
            case "java" -> "Main.java";
            default     -> "source.txt";
        };
    }

    private CodeResponse errorResponse(String msg, long ms) {
        return new CodeResponse("", msg, "ERROR", ms, null);
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private void deleteDir(Path dir) {
        try {
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        } catch (IOException e) {
            System.err.println("Temp cleanup warning: " + e.getMessage());
        }
    }

    // ==================================================================
    // INNER CLASSES — simple data holders
    // ==================================================================

    /** Result from a compile step */
    private static class ExecResult {
        boolean success;
        String  text;    // compiler output (errors or warnings)
        ExecResult(boolean success, String text) {
            this.success = success;
            this.text    = text;
        }
    }

    /** Result from executing a process */
    private static class ProcessOutput {
        String  text;
        int     exitCode;
        boolean timedOut;
        ProcessOutput(String text, int exitCode, boolean timedOut) {
            this.text     = text;
            this.exitCode = exitCode;
            this.timedOut = timedOut;
        }
    }
}
