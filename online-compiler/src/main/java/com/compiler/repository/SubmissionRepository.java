package com.compiler.repository;

import com.compiler.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Spring Data JPA repository.
 * All basic CRUD methods (save, findById, findAll, delete)
 * are auto-provided — no SQL needed!
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    // Get 10 most recent submissions (newest first)
    List<Submission> findTop10ByOrderByCreatedAtDesc();

    // Get all submissions for one language
    List<Submission> findByLanguageOrderByCreatedAtDesc(String language);
}
