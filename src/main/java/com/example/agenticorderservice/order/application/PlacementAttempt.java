package com.example.agenticorderservice.order.application;

/** State belongs to one attempt and survives exceptional transaction completion. */
public final class PlacementAttempt {
    public enum Completion {
        UNKNOWN,
        COMMITTED,
        ROLLED_BACK
    }

    private Completion completion = Completion.UNKNOWN;
    private RuntimeException lockCallFailure;

    public Completion completion() {
        return completion;
    }

    public void complete(Completion completion) {
        this.completion = completion;
    }

    void recordLockCallFailure(RuntimeException failure) {
        lockCallFailure = failure;
    }

    RuntimeException lockCallFailure() {
        return lockCallFailure;
    }
}
