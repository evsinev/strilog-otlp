package com.acme.strilog.sender.task.batch;

public enum BatchSendStatus {

    DELIVERED,

    /** The request body is over the collector limit: a half of the batch may pass. */
    TOO_LARGE,

    /** Network failure, 429 or 5xx: the same batch may pass later. */
    RETRY,

    /** Any other 4xx: resending the same batch gets the same answer. */
    REJECTED

}
