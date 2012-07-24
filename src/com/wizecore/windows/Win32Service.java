package com.wizecore.windows;

/**
 * Information about Win32 service.
 */
public class Win32Service {

	/**
	 * Service host.
	 */
	String hostname;
	
	/**
	 * Name of service.
	 */
	String name;
	
	/**
	 * Title of service.
	 */
	String displayName;
	
	/**
	 * Service type, for example 0x00000010; // SERVICE_WIN32_OWN_PROCESS
	 */
    int type;
    
    /**
     * Service state, for example CONTROL_PAUSE = 2;
     */
    int state;
    
    /**
     * FIXME: see windows doc.
     */
    int controlsAccepted;
    
    /**
     * Exit code if service stopped.
     */
    int win32ExitCode;
    
    /**
     * FIXME: no data
     */
    int serviceSpecificExitCode;
    
    /**
     * FIXME: no data
     */
    int checkPoint;
    
    /**
     * FIXME: no data
     */
    int waitHint;
    
    /**
     * Process id of service, only valid if it is started.
     */
    long processId;
    
	/**
	 * Getter for {@link Win32Service#name}.
	 */
	public String getName() {
		return name;
	}
	/**
	 * Setter for {@link Win32Service#name}.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * Getter for {@link Win32Service#displayName}.
	 */
	public String getDisplayName() {
		return displayName;
	}
	/**
	 * Setter for {@link Win32Service#displayName}.
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	/**
	 * Getter for {@link Win32Service#type}.
	 */
	public int getType() {
		return type;
	}
	/**
	 * Setter for {@link Win32Service#type}.
	 */
	public void setType(int type) {
		this.type = type;
	}
	/**
	 * Getter for {@link Win32Service#state}.
	 */
	public int getState() {
		return state;
	}
	/**
	 * Setter for {@link Win32Service#state}.
	 */
	public void setState(int state) {
		this.state = state;
	}
	/**
	 * Getter for {@link Win32Service#controlsAccepted}.
	 */
	public int getControlsAccepted() {
		return controlsAccepted;
	}
	/**
	 * Setter for {@link Win32Service#controlsAccepted}.
	 */
	public void setControlsAccepted(int controlsAccepted) {
		this.controlsAccepted = controlsAccepted;
	}
	/**
	 * Getter for {@link Win32Service#win32ExitCode}.
	 */
	public int getWin32ExitCode() {
		return win32ExitCode;
	}
	/**
	 * Setter for {@link Win32Service#win32ExitCode}.
	 */
	public void setWin32ExitCode(int win32ExitCode) {
		this.win32ExitCode = win32ExitCode;
	}
	/**
	 * Getter for {@link Win32Service#serviceSpecificExitCode}.
	 */
	public int getServiceSpecificExitCode() {
		return serviceSpecificExitCode;
	}
	/**
	 * Setter for {@link Win32Service#serviceSpecificExitCode}.
	 */
	public void setServiceSpecificExitCode(int serviceSpecificExitCode) {
		this.serviceSpecificExitCode = serviceSpecificExitCode;
	}
	/**
	 * Getter for {@link Win32Service#checkPoint}.
	 */
	public int getCheckPoint() {
		return checkPoint;
	}
	/**
	 * Setter for {@link Win32Service#checkPoint}.
	 */
	public void setCheckPoint(int checkPoint) {
		this.checkPoint = checkPoint;
	}
	/**
	 * Getter for {@link Win32Service#waitHint}.
	 */
	public int getWaitHint() {
		return waitHint;
	}
	/**
	 * Setter for {@link Win32Service#waitHint}.
	 */
	public void setWaitHint(int waitHint) {
		this.waitHint = waitHint;
	}
	/**
	 * Getter for {@link Win32Service#hostname}.
	 */
	public String getHostname() {
		return hostname;
	}
	/**
	 * Setter for {@link Win32Service#hostname}.
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	/**
	 * Getter for {@link Win32Service#processId}.
	 */
	public long getProcessId() {
		return processId;
	}
	/**
	 * Setter for {@link Win32Service#processId}.
	 */
	public void setProcessId(long processId) {
		this.processId = processId;
	}
}
