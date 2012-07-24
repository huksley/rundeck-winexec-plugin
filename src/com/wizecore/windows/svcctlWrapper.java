package com.wizecore.windows;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import jcifs.dcerpc.ndr.NdrLong;
import jcifs.util.Encdec;
import jcifs.util.Hexdump;
import rpc.ProviderException;
import rpc.Transport;
import rpc.TransportFactory;
import rpc.ncacn_np.RpcTransport;

/**
 * Usefull wrappers for SCM operations.
 */
public class svcctlWrapper extends svcctl {

	private final static Logger log = Logger.getLogger(svcctlWrapper.class.getName());
	
	static final int BUFSIZ = 16384;

	String servername;

	svcctlWrapper(String servername, Properties properties) {
		this.servername = servername;
		setAddress("ncacn_np:" + servername + "[\\PIPE\\svcctl]");
		setProperties(properties);
	}
	
	rpc.policy_handle svcctlOpenSCManager() throws Win32Exception, IOException {
		rpc.policy_handle handle = new rpc.policy_handle();
		handle.uuid = new rpc.uuid_t();

		OpenSCManager req = new OpenSCManager( "\\\\" + servername, null, SC_MANAGER_ALL_ACCESS, handle);
		call(0, req);
		if( req.retval != 0 ) {
			throw new Win32Exception(req.retval);
		}

		return handle;
	}
	
    public static class ServiceStatus extends service_status {

		public String service_name;
		public String display_name;

        public int decode( byte[] src, int si, int rel_start, char[] buf ) throws IOException {
			int start = si, rel;

			rel = Encdec.dec_uint32le( src, si ); si += 4;
			service_name = Encdec.dec_ucs2le( src, rel_start + rel, src.length, buf );
			rel = Encdec.dec_uint32le( src, si ); si += 4;
			display_name = Encdec.dec_ucs2le( src, rel_start + rel, src.length, buf );

            service_type = Encdec.dec_uint32le( src, si ); si += 4;
            current_state = Encdec.dec_uint32le( src, si ); si += 4;
            controls_accepted = Encdec.dec_uint32le( src, si ); si += 4;
            win32_exit_code = Encdec.dec_uint32le( src, si ); si += 4;
            service_specific_exit_code = Encdec.dec_uint32le( src, si ); si += 4;
            check_point = Encdec.dec_uint32le( src, si ); si += 4;
            wait_hint = Encdec.dec_uint32le( src, si ); si += 4;

			return si - start;
        }
    }
    
    public static class ServiceStatusEx extends service_status_ex {
    	
    	public int decode( byte[] src, int si, int rel_start, char[] buf ) throws IOException {
    		int start = si, rel;
    		service_type = Encdec.dec_uint32le( src, si ); si += 4;
            current_state = Encdec.dec_uint32le( src, si ); si += 4;
            controls_accepted = Encdec.dec_uint32le( src, si ); si += 4;
            win32_exit_code = Encdec.dec_uint32le( src, si ); si += 4;
            service_specific_exit_code = Encdec.dec_uint32le( src, si ); si += 4;
            check_point = Encdec.dec_uint32le( src, si ); si += 4;
            wait_hint = Encdec.dec_uint32le( src, si ); si += 4;
            process_id = Encdec.dec_uint32le( src, si ); si += 4;
            service_flags = Encdec.dec_uint32le( src, si ); si += 4;

			return si - start;
        }
    }

    ServiceStatus[] svcctlEnumServicesStatus(rpc.policy_handle scm_handle) throws Win32Exception, IOException {
		byte[] service = new byte[BUFSIZ];
		EnumServicesStatus req = new EnumServicesStatus(scm_handle,
				0x30, 0x3, BUFSIZ, service, 0, 0, 0);
		call(0, req);
		if( req.retval != 0 ) {
			throw new Win32Exception(req.retval);
		}

		log.fine( "bytes_needed=" + req.bytes_needed + ",services_returned=" + req.services_returned + ",resume_handle=" + req.resume_handle );

		char[] buf = new char[BUFSIZ / 2];
		int off = 0;
		ServiceStatus[] ss = new ServiceStatus[req.services_returned];
		for( int i = 0; i < req.services_returned; i++ ) {
			ServiceStatus status = new ServiceStatus();
			ss[i] = status;
			off += status.decode( req.service, off, 0, buf );
			log.fine( "service_name=" + status.service_name + ",display_name=" + status.display_name + ",service_type=0x" + Hexdump.toHexString( status.service_type, 4 ) + ",current_state=" + status.current_state + ",controls_accepted=" + status.controls_accepted + ",win32_exit_code=0x" + Hexdump.toHexString( status.win32_exit_code, 8 ) + ",service_specific_exit_code=" + status.service_specific_exit_code + ",check_point=" + status.check_point + ",wait_hint=" + status.wait_hint );
		}
		return ss;
	}
	rpc.policy_handle svcctlOpenService(rpc.policy_handle scm_handle, String name) throws Win32Exception, IOException {
		rpc.policy_handle handle = new rpc.policy_handle();
		handle.uuid = new rpc.uuid_t();

		OpenService req = new OpenService( scm_handle, name, SERVICE_ALL_ACCESS, handle);
		call(0, req);
		if( req.retval != 0 ) {
			throw new Win32Exception(req.retval);
		}

		return handle;
	}
	
	void svcctlCreateService(rpc.policy_handle scm_handle, 
                    String ServiceName,
                    String DisplayName,
                    int desired_access,
                    int type,
                    int start_type,
                    int error_control,
                    String binary_path,
                    String LoadOrderGroupKey,
                    // int TagId,
                    NdrLong TagId,
                    String dependencies,
                    String service_start_name,
                    String password) throws Win32Exception, IOException {
		CreateServiceW req = new CreateServiceW( scm_handle, ServiceName, DisplayName, desired_access, type, start_type, error_control, binary_path, LoadOrderGroupKey, TagId, dependencies, service_start_name, password);
		call(0, req);
		if( req.retval != 0 ) {
			throw new Win32Exception(req.retval);
		}
	}
	
	void svcctlStartService( rpc.policy_handle handle, String[] args ) throws Win32Exception, IOException {
		StartService req = new StartService( handle, args.length, args );
		call(0, req);
		if( req.retval != 0 ) {
			throw new Win32Exception(req.retval);
		}
	}
	
	void svcctlControlService( rpc.policy_handle handle, int control ) throws Win32Exception, IOException {
		service_status status = new service_status();
		ControlService req = new ControlService( handle, control, status );
		call(0, req);
		if( req.retval != 0 ) {
			throw new Win32Exception(req.retval);
		}
	}
	
	service_status svcctlQueryServiceStatus( rpc.policy_handle handle) throws Win32Exception, IOException {
		service_status status = new service_status();
		QueryServiceStatus req = new QueryServiceStatus( handle, status );
		call(0, req);
		if( req.retval != 0 ) {
			throw new Win32Exception(req.retval);
		}		
		return req.status;
	}
	
	void svcctlDeleteService( rpc.policy_handle handle) throws Win32Exception, IOException {
		DeleteService req = new DeleteService( handle );
		call(0, req);
		if( req.retval != 0 ) {
			throw new Win32Exception(req.retval);
		}		
	}
	
	service_status_ex svcctlQueryServiceStatusEx( rpc.policy_handle handle) throws Win32Exception, IOException {
		byte[] sbuf = new byte[4096];
		QueryServiceStatusEx req = new QueryServiceStatusEx( handle, 0, sbuf, 4096, 0);
		call(0, req);
		if( req.retval != 0 ) {
			throw new Win32Exception(req.retval);
		}
		ServiceStatusEx status = new ServiceStatusEx();
		status.decode(req.status, 0, 0, null);
		return status;
	}
	
	void svcctlCloseServiceHandle(rpc.policy_handle handle) throws Win32Exception, IOException {
		CloseServiceHandle req = new CloseServiceHandle(handle);
		call(0, req);
		if( req.retval != 0 ) {
			throw new Win32Exception(req.retval);
		}
	}
	public void doAll() throws Win32Exception, IOException {
		
		rpc.policy_handle scm_handle = svcctlOpenSCManager();
		ServiceStatus[] l = svcctlEnumServicesStatus(scm_handle);
		for (int i = 0; i < l.length; i++) {
			System.out.println(l[i].service_name);
		}
		rpc.policy_handle handle = svcctlOpenService( scm_handle, "TlntSvr" );
		String[] args = { "one", "two", "three" };
		// svcctlStartService( handle, args );
// System.out.println( "Network DDE DSDM service started." );
// Thread.sleep( 1000 );
service_status_ex svcctlQueryServiceStatusEx = svcctlQueryServiceStatusEx(handle);
		// svcctlControlService( handle, SERVICE_CONTROL_STOP );
// System.out.println( "Network DDE DSDM service stopped." );
		svcctlCloseServiceHandle( handle );
		svcctlCloseServiceHandle( scm_handle );
	}
}
