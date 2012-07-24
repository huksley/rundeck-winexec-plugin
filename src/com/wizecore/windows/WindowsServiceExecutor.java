package com.wizecore.windows;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
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
						
		String serviceFolder = rootFolder + serviceName;
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
				
		SmbFile sf = new SmbFile(rf, serviceName + "/");
		sf.connect();
		if (!sf.exists()) {
			sf.mkdir();
		}
		
		scm = new Win32ServiceControl();		
		scm.connect(host, domain, username, password);		
		
		boolean start = false;
		try {
			start = scm.isStarted(serviceName);
			if (start) {
				log.info("Stopping service " + serviceName);
				scm.control(serviceName, Win32ServiceControl.CONTROL_STOP);
				while (!scm.isStopped(serviceName)) {
					Thread.sleep(100);
				}
			}
			
			log.info("Deleting service " + serviceName);
			scm.delete(serviceName);
		} catch (Exception e) {
			// Don`t care
		}
		
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
		scm.start(serviceName);
		while (!scm.isStarted(serviceName)) {
			Thread.sleep(100);
		}
	} 
	
	public void stop() throws Win32Exception, InterruptedException, IOException {
		if (scm == null || !scm.isConnected()) {
			throw new NullPointerException("not connected");
		}
		
		// Stop service
		log.info("Stopping service " + serviceName);
		scm.control(serviceName, Win32ServiceControl.CONTROL_STOP);
		while (!scm.isStopped(serviceName)) {
			Thread.sleep(100);
		}
		
		log.info("Deleting service " + serviceName);
		scm.delete(serviceName);
		scm.disconnect();
		
		SmbFile rf = new SmbFile(smbPath, smbAuth);
		SmbFile sf = new SmbFile(rf, serviceName + "/");
		sf.connect();
		deleteResources(serviceFiles, sf);
		deleteFiles(sf);
		sf.delete();
	}

	protected void deleteFiles(SmbFile sf) throws SmbException {
		SmbFile[] l = sf.listFiles();
		for (int i = 0; i < l.length; i++) {
			log.info("Deleting " + l[i].getName());
			l[i].delete();
		}
	}
	
	protected <T> T tryAgain(Callable<T> call, int tries) throws Exception {
		for (int i = 0; i < tries - 1; i++) {
			try {
				return call.call();
			} catch (Exception e) {
				// Ignore
			}
		}
		
		return call.call();
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
			log.info("Delete " + l[i]);
			SmbFile ff = new SmbFile(sf, l[i]);
			ff.delete();
		}
	}

	protected void copyResources(String path, String[] l, SmbFile sf) throws IOException {
		for (int i = 0; i < l.length; i++) {
			URL res = getClass().getResource(path.replace('.', '/') + "/" + l[i]);
			if (res != null) {
				log.info("Copy " + l[i]);
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
}
