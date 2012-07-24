package com.wizecore.windows;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Runs remote console
 */
public class RemoteWindowsConsole {
	
	private final static Logger log = Logger.getLogger(RemoteWindowsConsole.class.getName());
	
	public static void main(String[] args) throws Win32Exception, IOException, InterruptedException {
		if (args.length < 4) {
			System.err.println("Usage: " + RemoteWindowsConsole.class.getSimpleName() + " <host> <domain> <user> <pass> [cmd] [arg1..]");
			return;
		}
		WindowsServiceExecutor exec = new WindowsServiceExecutor();
		exec.setHost(args[0]);
		exec.setDomain(args[1]);
		exec.setUsername(args[2]);
		exec.setPassword(args[3]);
		exec.start();
		
		if (args.length > 4) {
			String cmd = "";
			for (int i = 4; i < args.length; i++) {
				if (!cmd.equals("")) {
					cmd += " ";
				}
				cmd += args[i];				
			}
			ProcessResult r = exec.exec(cmd);
			dump(r);
		} else {
			log.info("Ready>");
			BufferedReader buf = new BufferedReader(new InputStreamReader(System.in));
			String s = null;
			while ((s = buf.readLine()) != null) {
				s = s.trim();
				if (s.equals("exit")) {
					break;
				} else {
					try {
						ProcessResult r = exec.exec(s);
						dump(r);
					} catch (Exception e) {
						System.err.println(e);
					}
				}
			}
		}
		
		exec.stop();
	}

	private static void dump(ProcessResult r) {
		System.out.println(r.stdout);
		System.err.println(r.stderr);
		System.out.println("Return code: " + r.returnCode);
	}
}