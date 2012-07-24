package com.wizecore.windows;

import jcifs.util.Hexdump;

/**
 * Decodes some Win32 error codes in human readable message.
 */
public class Win32Exception extends Exception {

	private static final long serialVersionUID = 1L;	
	protected int code = 0;

	public Win32Exception(String message, Throwable cause) {
		super(message, cause);
	}

	public Win32Exception(String message) {
		super(message != null && message.startsWith("0x") ? findWindowsMessage(Integer.parseInt(message.substring(2), 16), message) : message);
		code = Integer.parseInt(message.substring(2), 16);
	}
	
	public Win32Exception(int code, String message) {
		super(findWindowsMessage(code, message));
	}
	
	public Win32Exception(int code) {
		super(findWindowsMessage(code, null));
		this.code = code;
	}

	public Win32Exception(Throwable cause) {
		this(cause.getMessage());
		initCause(cause);		
		if (cause instanceof Win32Exception) {
			code = ((Win32Exception) cause).getCode(); 
		}
	}

	protected static String findWindowsMessage(int err, String message) {
		String msg = null;
		switch (err) {
			case 0x00000001: msg = "The service runs in a system process that must always be running. "; break;
			case 0x00000005: msg = "The handle to the SCM database does not have the appropriate access rights."; break;
			case 0x00000423: msg = "A circular service dependency was specified."; break;
			case 0x00000429: msg = "The specified database does not exist."; break;
			case 0X0000041B: msg = "The service cannot be stopped because other running services are dependent on it."; break;
			case 0X00000436: msg = "The display name already exists in the service control manager database either as a service name or as another display name."; break;
			case 0X0000007A: msg = "The buffer is too small for the SERVICE_STATUS_PROCESS structure. Nothing was written to the structure."; break;
			case 0X0000000D: msg = "The specified service status structure is invalid."; break;
			case 0X00000006: msg = "The handle to the specified service control manager database is invalid."; break;
			case 0X0000007C: msg = "The InfoLevel parameter contains an unsupported value."; break;
			case 0X0000007B: msg = "The specified service name is invalid."; break;
			case 0X00000057: msg = "A parameter that was specified is invalid (CreateService) or the cbSize member of SERVICE_STATUS_PROCESS is not valid (QueryServiceStatusEx)."; break;
			case 0X00000421: msg = "The user account name specified in the lpServiceStartName parameter does not exist."; break;
			case 0X0000041C: msg = "The requested control code is not valid, or it is unacceptable to the service."; break;
			case 0X00000003: msg = "The service binary file could not be found."; break;
			case 0X00000420: msg = "An instance of the service is already running."; break;
			case 0X00000425: msg = "The requested control code cannot be sent to the service because the state of the service is SERVICE_STOPPED, SERVICE_START_PENDING, or SERVICE_STOP_PENDING."; break;
			case 0X0000041F: msg = "The database is locked."; break;
			case 0X00000433: msg = "The service depends on a service that does not exist or has been marked for deletion."; break;
			case 0X0000042C: msg = "The service depends on another service that has failed to start."; break;
			case 0X00000422: msg = "The service has been disabled."; break;
			case 0X00000431: msg = "The specified service already exists in this database."; break;
			case 0X0000042D: msg = "The service did not start due to a logon failure. This error occurs if the service is configured to run under an account that does not have the \"Log on as a service\" right."; break;
			case 0X00000430: msg = "The specified service has already been marked for deletion."; break;
			case 0X0000041E: msg = "A thread could not be created for the service."; break;
			case 0X00000426: msg = "The service has not been started."; break;
			case 0X0000041D: msg = "The process for the service was started, but it did not call StartServiceCtrlDispatcher, or the thread that called StartServiceCtrlDispatcher may be blocked in a control handler function."; break;
			case 0X0000045B: msg = "The system is shutting down; this function cannot be called. "; break;
			case 0X00000424: msg = "The specified service does not exist as an installed service."; break; 
		}
		
		if (message == null && msg != null) { 
			message = msg;
		} else
		if (msg != null) {
			message = msg + " (" + message + ")";
		} else
		if (message == null && err > 0) {
			message = "0x" + Integer.toHexString(err);
		} else {
			message = "Unknown error";
		}
		
		return message;
	}

	/**
	 * Getter for {@link Win32Exception#code}.
	 */
	public int getCode() {
		return code;
	}
}
