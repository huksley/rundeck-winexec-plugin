package com.wizecore.windows;

import java.util.Properties;

import jcifs.dcerpc.ndr.NdrLong;
import rpc.ProviderException;
import rpc.Transport;
import rpc.TransportFactory;
import rpc.ncacn_np.RpcTransport;
import com.wizecore.windows.svcctlWrapper.ServiceStatus;
import com.wizecore.windows.svcctl.service_status_ex;

/**
 * Cross platform, all java Windows service control.
 */
public class Win32ServiceControl {
	
	// NB: NO CONTROL_START -> use start() method
    public static final int CONTROL_STOP = 1;
    public static final int CONTROL_PAUSE = 2;
    public static final int CONTROL_CONTINUE = 3;
    public static final int CONTROL_INTERROGATE = 4;
    
	public final static int SERVICE_AUTO_START = 0x00000002;
	public final static int SERVICE_BOOT_START = 0x00000000;
	public final static int SERVICE_DEMAND_START = 0x00000003;
	public final static int SERVICE_DISABLED = 0x00000004;
	public final static int SERVICE_SYSTEM_START = 0x00000001;    
	
	protected svcctlWrapper stub;
	protected rpc.policy_handle scm_handle;
	protected String hostname;
	protected String domain;
	protected String username;
	protected String password;

	/**
	 * Connects to SCM (Service Control Manager) on remote Windows host.
	 * Windows must have firewall turned off (or rule added).
	 * <p>
	 * You must issue disconnect() after finishing operations. Failure to do so may create memory leaks at Windows side.
	 * 
	 * @param hostname Remote host.
	 * @param domain Domain to authenticate (or hostname or null).
	 * @param username User having administrative privileges on host.
	 * @param password User`s password.
	 * 
	 * @throws Win32Exception If something goes wrong (or service exists).
	 */
	public void connect(String hostname, String domain, String username, String password) throws Win32Exception { 
		this.hostname = hostname;
		this.domain = domain;
		this.username = username;
		this.password = password;
		
		Properties properties = null;
        properties = new Properties();
        properties.setProperty("rpc.ncacn_np.domain", domain);
        properties.setProperty("rpc.ncacn_np.username", username);
        properties.setProperty("rpc.ncacn_np.password", password);
        properties.setProperty("rpc.security.username", username);
        properties.setProperty("rpc.security.password", password);
		stub = new svcctlWrapper(hostname, properties);
		stub.setTransportFactory(new TransportFactory() {
			public Transport createTransport(String address, Properties properties)
							throws ProviderException {
				return new RpcTransport(address, properties);
			}
		});
		
		try {
			scm_handle = stub.svcctlOpenSCManager();
		} catch (Exception e) {
			throw e instanceof Win32Exception ? (Win32Exception) e : new Win32Exception(e);
		}
	}
	
	/**
	 * Disconnects from SCM.
	 * 
	 * @throws Win32Exception
	 */
	public void disconnect() throws Win32Exception {
		try {
			stub.svcctlCloseServiceHandle(scm_handle);
		} catch (Exception e) {
			throw e instanceof Win32Exception ? (Win32Exception) e : new Win32Exception(e);
		}
	}
	
	/**
	 * Enumerates registered services. This method is fairly slow.
	 * 
	 * @return Array of found Win32 services.
	 */
	public Win32Service[] enumerate() throws Win32Exception {
		ServiceStatus[] ss = null;
		
		try {
			ss = stub.svcctlEnumServicesStatus(scm_handle);
		} catch (Exception e) {
			throw e instanceof Win32Exception ? (Win32Exception) e : new Win32Exception(e);
		}
		
		Win32Service[] svc = new Win32Service[ss.length];
		for (int i = 0; i < ss.length; i++) {
			Win32Service s = new Win32Service();
			ServiceStatus os = ss[i];
			s.setHostname(hostname);
			s.setName(os.service_name);
			s.setDisplayName(os.display_name);
			s.setType(os.service_type);
			s.setState(os.current_state);
			s.setControlsAccepted(os.controls_accepted);
			s.setWin32ExitCode(os.win32_exit_code);
			s.setServiceSpecificExitCode(os.service_specific_exit_code);
			s.setCheckPoint(os.check_point);
			s.setWaitHint(os.wait_hint);
			svc[i] = s;
		}
		return svc;
	}
	
	/**
	 * Stops service.
	 * 
	 * @param name Name of service
	 * @throws Win32Exception
	 */
	public void stop(String name) throws Win32Exception {
		control(name, CONTROL_STOP);
	}
	
	/**
	 * Starts service.
	 * 
	 * @param name Name of service
	 * @param args Arguments to service (not persistent), don`t use.
	 */
	public void start(String name, String[] args) throws Win32Exception {
		try {
			rpc.policy_handle handle = stub.svcctlOpenService(scm_handle, name);
			stub.svcctlStartService(handle, args);
			stub.svcctlCloseServiceHandle( handle );
		} catch (Exception e) {
			throw e instanceof Win32Exception ? (Win32Exception) e : new Win32Exception(e);
		}
	}
	
	/**
	 * Starts service.
	 * 
	 * @param name Name of service
	 */
	public void start(String name) throws Win32Exception {
		start(name, new String[0]);
	}

	/**
	 * Sends control code to service. 
	 * You can use one of registered {@link #CONTROL_CONTINUE} constants,
	 * or use your own. 
	 * <p>
	 * For example, sending to Apache httpd 2.2 control code = 128 
	 * forces it to do graceful restart.  
	 * 
	 * @param name Name of service
	 * @param controlCode Control code to send
	 */
	public void control(String name, int controlCode) throws Win32Exception {
		try {
			rpc.policy_handle handle = stub.svcctlOpenService(scm_handle, name);
			stub.svcctlControlService(handle, controlCode);	
			stub.svcctlCloseServiceHandle(handle);
		} catch (Exception e) {
			throw e instanceof Win32Exception ? (Win32Exception) e : new Win32Exception(e);
		}
	}
	
	/**
	 * Query status and all known information about service,
	 * including ProcessID.
	 * 
	 * @param name Name of service
	 * @return Information about service.
	 */
	public Win32Service status(String name) throws Win32Exception {
		try {
			rpc.policy_handle handle = stub.svcctlOpenService(scm_handle, name);
			service_status_ex os = stub.svcctlQueryServiceStatusEx(handle);
			Win32Service s = new Win32Service();
			s.setHostname(hostname);
			s.setName(name);
			s.setDisplayName(null);
			s.setType(os.service_type);
			s.setState(os.current_state);
			s.setControlsAccepted(os.controls_accepted);
			s.setWin32ExitCode(os.win32_exit_code);
			s.setServiceSpecificExitCode(os.service_specific_exit_code);
			s.setCheckPoint(os.check_point);
			s.setWaitHint(os.wait_hint);		
			s.setProcessId(os.process_id);
			stub.svcctlCloseServiceHandle(handle);
			return s;
		} catch (Exception e) {
			throw e instanceof Win32Exception ? (Win32Exception) e : new Win32Exception(e);
		}
	}
	
	/**
	 * Deletes service.
	 * Service must be stopped.
	 * 
	 * @param name Name of service.
	 */
	public void delete(String name) throws Win32Exception {
		try {
			rpc.policy_handle handle = stub.svcctlOpenService(scm_handle, name);
			stub.svcctlDeleteService(handle);
			stub.svcctlCloseServiceHandle(handle);
		} catch (Exception e) {
			throw e instanceof Win32Exception ? (Win32Exception) e : new Win32Exception(e);
		}
	}
	
	/**
	 * Registers new service.
	 * 
	 * @param name Name of service
	 * @param displayName Title of service
	 * @param startType Start type, one of {@link #SERVICE_DEMAND_START} constants.
	 * @param execPath Executable together with parameters. Don`t use / slashes.
	 * @param username User to run service. FIXME: Specifiying this causes 0x419 ERROR.
	 * @param password User`s password.
	 */
	public void create(String name, String displayName, int startType, String execPath, String username, String password) throws Win32Exception {
		int type = 0x00000010; // SERVICE_WIN32_OWN_PROCESS
		int desired_access = 0xF01FF; // SERVICE_ALL_ACCESS
		int error_control = 0x00000001; // SERVICE_ERROR_NORMAL 0x00000001 
		String loadOrderGroupKey = null;
		NdrLong tagId = null;
		String dependencies = null;
		try {
			stub.svcctlCreateService(scm_handle, 
						name,
	                    displayName,
	                    desired_access,
	                    type,
	                    startType,
	                    error_control,
	                    execPath,
	                    loadOrderGroupKey,
	                    tagId,
	                    dependencies,
	                    username,
	                    password);
		} catch (Exception e) {
			throw e instanceof Win32Exception ? (Win32Exception) e : new Win32Exception(e);
		}
	}
	
	/**
	 * Return one of {@link #CONTROL_STOP} and others constansts.
	 * @param name Name of service.
	 * @throws Win32Exception
	 */
	public int getState(String name) throws Win32Exception {
		return status(name).state;
	}
	
	/**
	 * Returns true if started, false otherwise or trasitional.
	 * 
	 * @param name Name of service.
	 */
	public boolean isStarted(String name) throws Win32Exception {
		return getState(name) == 4;
	}
	
	/**
	 * Returns true if stopped, false otherwise or trasitional.
	 * 
	 * @param name Name of service.
	 */
	public boolean isStopped(String name) throws Win32Exception {
		return getState(name) == CONTROL_STOP;
	}
	
	/**
	 * Returns true if connected to SCM.
	 */
	public boolean isConnected() {
		return scm_handle != null;
	}
}
