package com.wizecore.windows;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

/**
 * Monitors process for console output and waits for <code>maxTime</code> milliseconds for process to terminate.
 */
public class ProcessMonitor {
    /**
     * Splits string.
     */
    @SuppressWarnings("unchecked")
	protected static String[] split(String str, String sep) {
        if (str == null) {
            throw new NullPointerException("str");
        }
        
        if (sep == null) {
            throw new NullPointerException("sep");
        }
        
        Vector l = new Vector();
        int beginIndex = str.indexOf(sep);
        String part;
        
        while (beginIndex >= 0) {
            if (beginIndex == 0) {
                str = str.substring(beginIndex + sep.length());
                
            } else            
            if (beginIndex > 0) {
                part = str.substring(0, beginIndex);
                if (part.length() != 0) {
                    l.add(part);
                }
                str = str.substring(beginIndex + sep.length());
            }
            
            beginIndex = str.indexOf(sep);
        }
        part = str;
        if (part.length() != 0) {
            l.add(part);
        }
        String[] aa = new String[l.size()];
        for (int i = 0; i < l.size(); i++) {
        	aa[i] = (String) l.elementAt(i);
        }
        return aa;
    }
    
    /**
     * Monitors process.
     * 
     * @param proc Executed process to wait for. Use {@link Runtime#exec(String)} methods to execute.
     * @param listen Listener for console 
     * @param sleep Sleep between iterations (default 100)
     * @param timeout Maximum time to wait for process (default 60000)
     * @param encoding Console encoding, e.g. Cp850, Cp866 (default Cp866)
     * @return Return code from process or -1 if it was killed by timeout.
     * @throws IOException
     */
    public static int monitor(Process proc, ProcessListener listen, long sleep, long timeout, String encoding) throws IOException {
    	if (sleep <= 0) {
    		sleep = 100;
    	}
    	
    	if (timeout <= 0) {
    		timeout = 60000;
    	}
    	
    	if (encoding == null) {
    		encoding = "Cp866";
    	}
    	
    	if (listen == null) {
    		throw new NullPointerException("listen");
    	}
    	
    	if (proc == null) {
    		throw new NullPointerException("proc");
    	}
    	
        InputStream outis = proc.getInputStream();
        InputStream erris = proc.getErrorStream();
        InputStreamReader outisr = new InputStreamReader(outis, encoding);
        InputStreamReader errisr = new InputStreamReader(erris, encoding);
        String line = null;
        long start = System.currentTimeMillis();
        char[] buf = new char[2048];
        String leftoverOut = "";
        String leftoverErr = "";
        int exit = 0;
        boolean doExit = false;
        while (true) {
            try {            
                exit =  proc.exitValue();
                doExit = true;
            } catch (IllegalThreadStateException e) {
            	// Ignore 
            }
            
            if (outis.available() != 0) {
                int len = outis.available() > buf.length ? buf.length : outis.available();
                outisr.read(buf, 0, len);
                line = leftoverOut + new String(buf, 0, len);
                leftoverOut = "";
                if (line != null && line.length() > 0) {
                    String[] lines = split(line, "\n");
                    for (int i = 0; i < lines.length; i++)
                    	if (i < lines.length - 1 && lines[i].endsWith("\r")) {
                            listen.stdout(lines[i]);
                        } else {
                        	leftoverOut = lines[i];
                        }
                }
            }
            
            while (erris.available() != 0) {
                int len = erris.available() > buf.length ? buf.length : erris.available();
                errisr.read(buf, 0, len);
                line = leftoverErr + new String(buf, 0, len);
                leftoverErr = "";
                if (line != null && line.length() > 0) {
                    String[] lines = split(line, "\n");
                    for (int i = 0; i < lines.length; i++)
                        if (i < lines.length - 1 && lines[i].endsWith("\r")) {
                            listen.stderr(lines[i]);
                        } else {
                        	leftoverErr = lines[i];
                        }
                }
            }
            
            long current = System.currentTimeMillis();
            if (timeout != -1 && (current - start) > timeout) {
            	proc.destroy();
                return -1;
            }
            
            if (doExit) {
                return exit;
            }
            
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {  
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Monitors process.
     * 
     * See {@link ProcessMonitor#monitor(Process, ProcessListener, long, long, String)} for parameters.
     */
    public static int monitor(Process proc, ProcessListener listen) throws IOException {
    	return monitor(proc, listen, 0, 0, null);
    }
}
