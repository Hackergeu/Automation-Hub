package com.vansh.automation_hub.exception;

// Jab given id ka job DB mein na mile
public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(Long id) {
        super("Job not found with id " + id);
    }
}