package com.wizecore.rundeck.windows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.service.FileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.wizecore.windows.WindowsServiceExecutor;

/**
 * File copier using SmbFile.
 */
public class Win32CifsCopier implements FileCopier {

	@Override
	public String copyFile(ExecutionContext context, File file, INodeEntry node) throws FileCopierException {
		try {
			FileInputStream fis = new FileInputStream(file);
			try {
				return copyFileStreamEx(context, fis, file.getName(), node);
			} finally {
				fis.close();
			}
		} catch (IOException e) {
			throw new FileCopierException(e);
		}
	}

	public String copyFileStreamEx(ExecutionContext context, InputStream input, String name, INodeEntry node) throws FileCopierException {
		try {
			Properties p = new Properties();
			FileInputStream is = new FileInputStream(new File(context.getFramework().getConfigDir(), Win32NodeExecutor.SERVICE_PROVIDER_NAME + ".properties"));
			try {
				p.load(is);
			} finally {
				is.close();
			}
			
			String hostname = node.getHostname();
			String username = node.getUsername();
			if (username == null) {
				username = p.getProperty("host." + hostname + ".username", p.getProperty("default.username"));
			}
			
			String password = (String) node.getAttributes().get("password");
			if (password == null) {
				password = p.getProperty("user." + username + ".password", p.getProperty("default.password"));
			}
	
			String domain = (String) node.getAttributes().get("domain");
			if (domain == null) {
				domain = p.getProperty("host." + hostname + ".domain", p.getProperty("default.domain"));
			}
			
			if (username == null) {
				throw new NullPointerException("No username!");
			}
			if (password == null) {
				throw new NullPointerException("No password!");
			}
			if (hostname == null) {
				throw new NullPointerException("No hostname!");
			}
			
			if (name == null) {
				name = "rundeck-" + input.hashCode() + ".bin";
			}
			String rootPath = "smb://" + node.getHostname() + "/C$";
			String[] tries = new String[] {
				"Windows/Temp",
				"Temp",
				"Tmp",
				""
			};
			
			
			NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(domain, username, password);
			SmbFile tmp = null;
			for (int i = 0; i < tries.length; i++) {
				SmbFile f = new SmbFile(rootPath + "/" + tries[i] + "/");
				f.connect();
				if (f.exists()) {
					tmp = f;
					break;
				}
			}
			
			if (tmp != null) {
				try {
					SmbFile ff = new SmbFile(tmp, name);
					OutputStream os = ff.getOutputStream();
					try {
						copyStreamEx(input, os, 8192);
					} finally {
						os.close();
					}
				} finally {
					is.close();
				}
			} else {
				throw new FileNotFoundException("Can`t find temp dir on " + node);
			}
			
			return null;
		} catch (IOException e) {
			throw new FileCopierException(e);
		}
	}

	protected static void copyStreamEx(InputStream is, OutputStream os, int bufSize) throws IOException {
        byte[] buf = new byte[bufSize];
        int c = 0;
        while ((c = is.read(buf, 0, buf.length)) != -1) {
            os.write(buf, 0, c);
        }
    }
	
	@Override
	public String copyFileStream(ExecutionContext context, InputStream input, INodeEntry node) throws FileCopierException {
		return copyFileStreamEx(context, input, null, node);
	}
	
	@Override
	public String copyScriptContent(ExecutionContext context, String script, INodeEntry node) throws FileCopierException {
		try {
			return copyFileStreamEx(context, new ByteArrayInputStream(script.getBytes("Cp866")), "rundeck-" + script.hashCode() + ".bat", node);
		} catch (IOException e) {
			throw new FileCopierException(e);
		}
	}
}
