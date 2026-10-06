package com.acme.strilog.sender.task.batch;

import java.util.List;

public interface IBatchSenderClient<I> {

    BatchSendResult sendItems(List<I> aItems);

}
