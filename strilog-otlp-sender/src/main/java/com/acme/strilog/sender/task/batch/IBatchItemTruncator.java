package com.acme.strilog.sender.task.batch;

import jakarta.annotation.Nullable;

public interface IBatchItemTruncator<I> {

    /**
     * @return the item cut to fit {@code aMaxBytes}, or null when it does not fit even cut
     */
    @Nullable
    I truncate(I aItem, long aMaxBytes);

}
