package com.wizecore.windows;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Installs service on remote host to execute commands.
 * Communicates with service using secret token.
 */
public class WindowsServiceExecutor {
	
	private final static Logger log = Logger.getLogger(WindowsServiceExecutor.class.getName());

	/**
	 * Destination host.
	 */
	protected String host;
	
	/**
	 * Domain to use. If not set, equals to host.
	 */
	protected String domain;
	
	/**
	 * Administrator user on remote host.
	 */
	protected String username = "Administrator";
	
	/**
	 * User`s password.
	 */
	protected String password;
	
	/**
	 * Root folder for service.
	 */
	protected String rootFolder = "C:/";
	
	/**
	 * Service name.
	 */
	protected String serviceName = "RemoteWinExec";
	
	/**
	 * Files to copy to remote side.
	 */
	protected String[] serviceFiles = {
		"wrapper.exe",
		"wrapper.conf",
		"wrapper.dll",
		"wrapper.jar",
		"com.wizecore.windows.jar"
	};
	
	/**
	 * HTTP port on remote service.
	 */
	protected int port = 1989;
	
	/**
	 * Secret token to use. 
	 * If not set, creates random generated one.
	 */
	protected String token;
	
	/**
	 * Additional space separated arguments to service wrapper, for example, direct path to Java executable.
	 * This can define new parameters or override declared in wrapper.conf.
	 * For more information see tanukisoftware.com.
	 * e.g. wrapper.java.command=C:/Java/jdk/bin/java.exe
	 */
	protected String serviceArgument;
	
	/**
	 * Number of tries for potentially failing operations.
	 * For example, because of locked files by antivirus.
	 */
	protected int tries = 3;
	
	/**
	 * Wait timeout for single operation (stop/start)
	 */
	protected int operationTimeout = 10000;
	
	/**
	 * Pattern format must be kept in sync with one in wrapper.conf, wrapper.logfile = ... 
	 */
	protected String getWrapperLog() {
		return "winexec-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".log";
	}
	
	protected Win32ServiceControl scm;
	protected NtlmPasswordAuthentication smbAuth;
	protected String smbPath;
		
	public void start() throws Win32Exception, IOException, InterruptedException {
		// Supprress debug
		Logger.getLogger("org.jinterop").setLevel(Level.WARNING);
		
		if (domain == null) {
			domain = host;
		}
		
		if (token == null) {
			token = String.valueOf(System.currentTimeMillis());
		}
		
		String args = "wrapper.app.parameter.2=" + port + " wrapper.app.parameter.3=" + token;
		if (serviceArgument != null) {
			args += " ";
			args += serviceArgument.trim();
		}
		
		String serviceFolder = rootFolder;
		serviceFolder = serviceFolder.replace("/", "\\");
		if (!serviceFolder.endsWith("\\")) {
			serviceFolder += "\\";
		}
		serviceFolder += serviceName;
		smbPath = "smb://" + host + "/" + rootFolder.replace('\\', '/').replaceAll("\\:", "\\$") + "/";	
		while (smbPath.endsWith("/")) {
			smbPath = smbPath.substring(0, smbPath.length() - 1);
		}
		smbPath += "/";
		
		log.info("Checking " + smbPath);
		smbAuth = new NtlmPasswordAuthentication(domain, username, password);
		SmbFile rf = new SmbFile(smbPath, smbAuth);
		rf.connect();
		if (!rf.exists()) {
			throw new FileNotFoundException("Unable to find root folder " + rootFolder);
		}
		
		final SmbFile sf = new SmbFile(rf, serviceName + "/");
		if (!sf.exists()) {
			sf.mkdir();
		}
		
		try {	
			scm = new Win32ServiceControl();		
			scm.connect(host, domain, username, password);		
			
			boolean start = false;
			try {
				start = scm.isStarted(serviceName);
				if (start) {
					log.info("Stopping service " + serviceName);
					scm.control(serviceName, Win32ServiceControl.CONTROL_STOP);
					tryAgainTimeout(new Callable<Object>() {
						public Object call() throws Exception {
							return scm.isStopped(serviceName) ? serviceName : null;
						}
						public String toString() { return "isStopped"; }
					}, null, 100, operationTimeout, 3);
				}
				
				log.info("Deleting service " + serviceName);
				tryAgain(new Callable<Object>() { public Object call() throws Exception {
					scm.delete(serviceName);
					return null;
				} }, tries);
			} catch (Exception e) {
				// Don`t care
			}
			
			tryAgain(new Callable<Object>() { public Object call() throws Exception {
				deleteResources(serviceFiles, sf);
				return null;
			} }, tries);
			
			copyResources("service", serviceFiles, sf);
			
			try { 
				int st = scm.getState(serviceName);
				log.info("Service state: " + st);
			} catch (Win32Exception e) {
				if (e.getCode() == 0x00000424) { // The specified service does not exist as an installed service.
					log.info("Creating service " + serviceName);
					String serviceExe = serviceFolder + "\\wrapper.exe -s " + serviceFolder + "\\wrapper.conf " + args;
					serviceExe = serviceExe.replace('/', '\\');
					scm.create(serviceName, 
						serviceName, 
						Win32ServiceControl.SERVICE_DEMAND_START,
						serviceExe,
						null, null);				
				}
			}
			
			log.info("Starting service " + serviceName);
			tryAgain(new Callable<Object>() { public Object call() throws Exception {
				scm.start(serviceName);	
				return null;
			} }, tries);
			
			tryAgainTimeout(new Callable<Object>() {
				public Object call() throws Exception {
					return scm.isStarted(serviceName) ? serviceName : null;
				}
				public String toString() { return "isStarted"; }
			}, null, 100, operationTimeout, tries);
		} catch (Exception e) {
			String log = getWrapperLog();
			SmbFile lf = new SmbFile(sf, log);
			if (lf.exists()) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				InputStream is = lf.getInputStream();
				try {
					copyStreamEx(is, bos, 2048);
					String msg = new String(bos.toByteArray(), "ISO-8859-1");
					msg = msg.replace("\n", " ");
					msg = msg.replace("\r", "");
					msg = msg.replaceAll("[ ]+", " ");
					IOException ee = new IOException("Failed to start: " + msg + " (" + e.getMessage() + ")");
					ee.initCause(e);
					throw ee;
				} finally {
					is.close();
				}
			} else {
				IOException ee = new IOException(e.getMessage());
				ee.initCause(e);
				throw ee;
			}
		}
	} 
	
	public void stop() throws Win32Exception, InterruptedException, IOException {
		if (scm == null || !scm.isConnected()) {
			throw new NullPointerException("not connected");
		}
		
		// Stop service
		log.info("Stopping service " + serviceName);
		if (scm.isStarted(serviceName)) {
			scm.control(serviceName, Win32ServiceControl.CONTROL_STOP);			
		}
		tryAgainTimeout(new Callable<Object>() {
			public Object call() throws Exception {
				return scm.isStopped(serviceName) ? serviceName : null;
			}
			public String toString() { return "isStopped"; }
		}, null, 100, operationTimeout, 3);
		
		log.info("Deleting service " + serviceName);
		
		tryAgain(new Callable<Object>() { public Object call() throws Exception {
			scm.delete(serviceName);
			return null;
		} }, tries);
		
		scm.disconnect();
		
		SmbFile rf = new SmbFile(smbPath, smbAuth);
		final SmbFile sf = new SmbFile(rf, serviceName + "/");
		
		tryAgain(new Callable<Object>() { public Object call() throws Exception {
			deleteResources(serviceFiles, sf);
			return null;
		} }, tries);
		
		tryAgain(new Callable<Object>() { public Object call() throws Exception {
			deleteFiles(sf);
			return null;
		} }, tries);
		sf.delete();
	}

	protected void deleteFiles(SmbFile sf) throws SmbException {
		SmbFile[] l = sf.listFiles();
		for (int i = 0; i < l.length; i++) {
			log.fine("Deleting " + l[i].getName());
			l[i].delete();
		}
	}
	
	protected <T> T timeout(Callable<T> waitFor, Callable<Object> run, int sleep, int operationTimeout) {
		try {
			long start = System.currentTimeMillis();
			boolean timeout = false;
			T value = null;
			do {
				value = waitFor.call();
				
				if (run != null) {
					run.call();
				} 
				
				if (sleep > 0) {
					try {
						Thread.sleep(sleep);
					} catch (InterruptedException e) {
						break;
					}
				}
				
				timeout = System.currentTimeMillis() - start > operationTimeout;
			} while (!Thread.interrupted() && value == null && !timeout);
			if (timeout) {
				throw new RuntimeException("Timed out waiting for result: " + waitFor);
			}
			return value;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	protected <T> T tryAgainTimeout(final Callable<T> waitFor, final Callable<Object> run, final int sleep, final int timeout, int tries) {
		return tryAgain(new Callable<T>() { public T call() throws Exception {
			return timeout(waitFor, run, sleep, timeout);
		} }, tries);
	}
	
	protected <T> T tryAgain(Callable<T> call, int tries) {
		for (int i = 0; i < tries - 1; i++) {
			try {
				return call.call();
			} catch (Exception e) {
				// Ignore
			}
		}
		
		try {
			return call.call();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public ProcessResult exec(String cmd) throws IOException {
		ProcessResult r = new ProcessResult();
		HttpURLConnection conn = (HttpURLConnection) new URL("http://" + host + ":" + port + "/" + token + "/" + URLEncoder.encode(cmd, "UTF-8")).openConnection();
		InputStream is = conn.getInputStream();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		copyStreamEx(is, bos, 1024);
		is.close();
		r.stdout = bos.toString("UTF-8");
		r.returnCode = conn.getHeaderFieldInt("X-Return-Code", 0);
		return r;
	}
	
	protected void deleteResources(String[] l, SmbFile sf) throws IOException {
		for (int i = 0; i < l.length; i++) {
			log.fine("Delete " + l[i]);
			SmbFile ff = new SmbFile(sf, l[i]);
			try {
				if (ff.exists()) {
					ff.delete();
				}
			} catch (Exception e) {
				IOException ee = new IOException("Failed to delete " + ff + ": " + e.getMessage());
				ee.initCause(e);
				throw ee;
			}
		}
	}

	protected void copyResources(String path, String[] l, SmbFile sf) throws IOException {
		for (int i = 0; i < l.length; i++) {
			try {
				URL res = getClass().getResource(path.replace('.', '/') + "/" + l[i]);
				if (res != null) {
					log.fine("Copy " + l[i]);
					InputStream is = res.openStream();
					try {
						SmbFile ff = new SmbFile(sf, l[i]);
						OutputStream os = ff.getOutputStream();
						try {
							copyStreamEx(is, os, 8192);
						} finally {
							os.close();
						}
					} finally {
						is.close();
					}
				}
			} catch (Exception e) {
				IOException ee = new IOException("Failed to copy " + l[i] + ": " + e.getMessage());
				ee.initCause(e);
				throw ee;
			}
		}
	}

	protected static void copyStreamEx(InputStream is, OutputStream os, int bufSize) throws IOException {
        byte[] buf = new byte[bufSize];
        int c = 0;
        while ((c = is.read(buf, 0, buf.length)) != -1) {
            os.write(buf, 0, c);
        }
    }

	/**
	 * Getter for {@link WindowsServiceExecutor#host}.
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Setter for {@link WindowsServiceExecutor#host}.
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Getter for {@link WindowsServiceExecutor#domain}.
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * Setter for {@link WindowsServiceExecutor#domain}.
	 */
	public void setDomain(String domain) {
		this.domain = domain;
	}

	/**
	 * Getter for {@link WindowsServiceExecutor#username}.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Setter for {@link WindowsServiceExecutor#username}.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Getter for {@link WindowsServiceExecutor#password}.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Setter for {@link WindowsServiceExecutor#password}.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Getter for {@link WindowsServiceExecutor#rootFolder}.
	 */
	public String getRootFolder() {
		return rootFolder;
	}

	/**
	 * Setter for {@link WindowsServiceExecutor#rootFolder}.
	 */
	public void setRootFolder(String rootFolder) {
		this.rootFolder = rootFolder;
	}

	/**
	 * Getter for {@link WindowsServiceExecutor#serviceName}.
	 */
	public String getServiceName() {
		return serviceName;
	}

	/**
	 * Setter for {@link WindowsServiceExecutor#serviceName}.
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * Getter for {@link WindowsServiceExecutor#port}.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Setter for {@link WindowsServiceExecutor#port}.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Getter for {@link WindowsServiceExecutor#token}.
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Setter for {@link WindowsServiceExecutor#token}.
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * Getter for {@link WindowsServiceExecutor#serviceArgument}.
	 */
	public String getServiceArgument() {
		return serviceArgument;
	}

	/**
	 * Setter for {@link WindowsServiceExecutor#serviceArgument}.
	 */
	public void setServiceArgument(String serviceArgument) {
		this.serviceArgument = serviceArgument;
	}
}
