package com.compiler.controller;

import com.compiler.model.CodeRequest;
import com.compiler.model.CodeResponse;
import com.compiler.model.Submission;
import com.compiler.repository.SubmissionRepository;
import com.compiler.service.CodeExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST Controller — handles all HTTP requests from the frontend.
 *
 * Endpoints:
 *   POST /api/run              → execute code, return result
 *   GET  /api/submissions      → get 10 recent submissions
 *   GET  /api/submissions/{id} → get one submission by id
 *   GET  /api/health           → health check
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")   // allow frontend to call this from any origin
public class CompilerController {

    @Autowired
    private CodeExecutionService codeExecutionService;

    @Autowired
    private SubmissionRepository submissionRepository;

    /** Main endpoint — run the submitted code */
    @PostMapping("/run")
    public ResponseEntity<CodeResponse> runCode(@RequestBody CodeRequest request) {

        if (request.getSourceCode() == null || request.getSourceCode().isBlank()) {
            return ResponseEntity.badRequest()
                .body(new CodeResponse("", "Source code cannot be empty.", "ERROR", 0, null));
        }
        if (request.getLanguage() == null || request.getLanguage().isBlank()) {
            return ResponseEntity.badRequest()
                .body(new CodeResponse("", "Language must be specified.", "ERROR", 0, null));
        }

        System.out.println("[RUN] Language=" + request.getLanguage()
            + "  CodeLen=" + request.getSourceCode().length());

        CodeResponse result = codeExecutionService.executeCode(request);

        System.out.println("[DONE] Status=" + result.getStatus()
            + "  Time=" + result.getExecutionTimeMs() + "ms");

        return ResponseEntity.ok(result);
    }

    /** Return the 10 most recent submissions for the history panel */
    @GetMapping("/submissions")
    public ResponseEntity<List<Submission>> recent() {
        return ResponseEntity.ok(submissionRepository.findTop10ByOrderByCreatedAtDesc());
    }

    /** Return a single submission by ID (for reloading in editor) */
    @GetMapping("/submissions/{id}")
    public ResponseEntity<Submission> byId(@PathVariable Long id) {
        return submissionRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /** Health check */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Online Compiler API is running!");
    }
}
