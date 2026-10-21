package com.minihrms.model;

/** A leave request starts PENDING and moves to APPROVED or REJECTED exactly once. */
public enum LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED
}
