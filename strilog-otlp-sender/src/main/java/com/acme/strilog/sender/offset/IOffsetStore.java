package com.acme.strilog.sender.offset;

public interface IOffsetStore {

    void saveOffset(String aFilename, long aOffset);

    /**
     * @return the number of lines of {@code aFilename} sent before, 0 when the saved offset is of another file
     */
    long getOffset(String aFilename);

}
