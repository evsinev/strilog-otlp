package com.acme.strilog.sender.offset;

public interface IOffsetStore {

    void saveOffset(String aFilename, long aOffset);

}
