package com.wizecore.windows;

/**
 * Collects output from process.
 */
public interface ProcessListener {

    void stdout(String line);
    void stderr(String line);
}
