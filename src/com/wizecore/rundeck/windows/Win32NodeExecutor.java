/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * StubNodeExecutor.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 3/31/11 3:14 PM
 * 
 */
package com.wizecore.rundeck.windows;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.AbstractBaseDescription;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.wizecore.windows.ProcessResult;
import com.wizecore.windows.Win32Exception;
import com.wizecore.windows.WindowsServiceExecutor;

/**
 * Uses j-interop svcctl wrapper library for starting remote service.
 * @link {@link WindowsServiceExecutor}
 * 
 * <p>
 * Based on StubNodeExecutor by Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * 
 * @author Ruslan Gainutdinov <a href="mailto:ruslan@wizecore.com">ruslan@wizecore.com</a>
 */
@Plugin(name = Win32NodeExecutor.SERVICE_PROVIDER_NAME, service = "NodeExecutor")
public class Win32NodeExecutor implements NodeExecutor, Describable {
	public static final String SERVICE_PROVIDER_NAME = "winexec";
	
	private final static Logger log = Logger.getLogger(Win32NodeExecutor.class.getName());
	
	public Win32NodeExecutor() {
		log.info("I`ve been created!");
	}
	
	public NodeExecutorResult executeCommand(ExecutionContext context, String[] command, INodeEntry node) throws ExecutionException {
		try {
			WindowsServiceExecutor exec = open(context, node);
			String cmd = "";
			for (int i = 0; i < command.length; i++) {
				if (!cmd.equals("")) {
					cmd += " ";
				}
				cmd += command[i];				
			}
			 
			final ProcessResult r = exec.exec(cmd);
			
			BufferedReader br = new BufferedReader(new StringReader(r.stdout));
			String l = null;
			while ((l = br.readLine()) != null) {
				context.getExecutionListener().log(1, l);
			}
			
			return new NodeExecutorResult() {			
				@Override
				public boolean isSuccess() {
					return r.returnCode == 0;
				}
				
				@Override
				public int getResultCode() {
					return r.returnCode;
				}
			};
		} catch (Exception e) {
			throw new ExecutionException(e);
		}
	}

	protected WindowsServiceExecutor open(ExecutionContext context, INodeEntry node) throws IOException, Win32Exception, InterruptedException {
		WindowsServiceExecutor exec = ExecutorHolder.getInstance().get(node.getHostname());
		if (exec != null) {
			return exec;
		}
		
		Properties p = new Properties();
		FileInputStream is = new FileInputStream(new File(context.getFramework().getConfigDir(), SERVICE_PROVIDER_NAME + ".properties"));
		try {
			p.load(is);
		} finally {
			is.close();
		}
		
		String hostname = node.getHostname();		
		hostname = hostname.toLowerCase();
		String nodeName = node.getNodename();
		nodeName = nodeName.toLowerCase();
		
		String username = node.getUsername();
		if (username == null) {
			username = p.getProperty("node." + nodeName + ".username", p.getProperty("default.username"));
		}
		username = username.toLowerCase();
		
		String password = (String) node.getAttributes().get("password");
		if (password == null) {
			password = p.getProperty("user." + username + ".password", p.getProperty("default.password"));
		}

		String domain = (String) node.getAttributes().get("domain");
		if (domain == null) {
			domain = p.getProperty("node." + nodeName + ".domain", p.getProperty("default.domain"));
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
		
		String rootFolder = p.getProperty("node." + nodeName + ".rootFolder", p.getProperty("default.rootFolder"));
		String serviceName = p.getProperty("node." + nodeName + ".serviceName", p.getProperty("serviceName"));
		String port = p.getProperty("node." + nodeName + ".port", p.getProperty("default.port"));
		String token = p.getProperty("node." + nodeName + ".token", p.getProperty("default.token"));
		String serviceArgument = p.getProperty("node." + nodeName + ".serviceArgument", p.getProperty("default.serviceArgument"));
		
		exec = new WindowsServiceExecutor();
		exec.setHost(hostname);
		exec.setPassword(password);
		exec.setUsername(username);
		exec.setDomain(domain);
		if (rootFolder != null) {
			exec.setRootFolder(rootFolder);
		}
		if (serviceName == null) {
			serviceName = "RundeckWinExec";
		}
		exec.setServiceName(serviceName);
		if (port != null) {
			exec.setPort(Integer.parseInt(port));
		}
		if (token != null) {
			exec.setToken(token);
		}
		if (serviceArgument != null) {
			exec.setServiceArgument(serviceArgument);
		}
		exec.start();
		ExecutorHolder.getInstance().add(exec);
		return exec;
	}

	private static final Description DESC = new AbstractBaseDescription() {
		public String getName() {
			return SERVICE_PROVIDER_NAME;
		}

		public String getTitle() {
			return "Windows Executor";
		}

		public String getDescription() {
			return "Windows all java execution";
		}
	};

	public Description getDescription() {
		return DESC;
	}
}
