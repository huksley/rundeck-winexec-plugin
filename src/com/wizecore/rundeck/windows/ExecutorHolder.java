package com.wizecore.rundeck.windows;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.wizecore.windows.WindowsServiceExecutor;

public class ExecutorHolder {

	protected List<WindowsServiceExecutor> executors;
	protected static ExecutorHolder executorHolder = new ExecutorHolder();  
	
	public static ExecutorHolder getInstance() {
		return executorHolder;
	}
	
	public synchronized void add(WindowsServiceExecutor exec) {
		if (executors == null) {
			executors =  new ArrayList<WindowsServiceExecutor>();
			Thread rr = new Thread(new Runnable() {			
				@Override
				public void run() {
					closeAll();
				}
			});
			Runtime.getRuntime().addShutdownHook(rr);
		}
		
		executors.add(exec);
	}
	
	protected synchronized void closeAll() {
		for (Iterator<WindowsServiceExecutor> it = executors.iterator(); it.hasNext();) {
			WindowsServiceExecutor exec = it.next();
			it.remove();
			try {
				exec.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized WindowsServiceExecutor get(String hostname) {
		if (executors == null) {
			return null;
		}
		
		for (int i = 0; i < executors.size(); i++) {
			WindowsServiceExecutor exec = executors.get(i);
			if (exec.getHost().equals(hostname)) {
				return exec;
			}
		}
		
		return null;
	}
}
