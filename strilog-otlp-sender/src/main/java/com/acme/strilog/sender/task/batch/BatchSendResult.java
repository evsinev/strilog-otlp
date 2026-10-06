package com.acme.strilog.sender.task.batch;

public record BatchSendResult(BatchSendStatus status, String description) {

    private static final BatchSendResult DELIVERED = new BatchSendResult(BatchSendStatus.DELIVERED, null);

    public static BatchSendResult delivered() {
        return DELIVERED;
    }

    public static BatchSendResult tooLarge(String aDescription) {
        return new BatchSendResult(BatchSendStatus.TOO_LARGE, aDescription);
    }

    public static BatchSendResult retry(String aDescription) {
        return new BatchSendResult(BatchSendStatus.RETRY, aDescription);
    }

    public static BatchSendResult rejected(String aDescription) {
        return new BatchSendResult(BatchSendStatus.REJECTED, aDescription);
    }

}
