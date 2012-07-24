package com.wizecore.windows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * Listens on port and executes commands.
 * Expects correct secret token to be bassed in URL.
 */
public class WindowsServiceRunner {
	
	private final static Logger log = Logger.getLogger(WindowsServiceRunner.class.getName());
	
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Usage: " + WindowsServiceRunner.class.getSimpleName() + " <port> <token>");
			return;
		}
		
		WindowsServiceRunner r = new WindowsServiceRunner();
		r.run(Integer.parseInt(args[0]), args[1]);
	}

	protected int port = 1989;
	protected String token = String.valueOf(System.currentTimeMillis());
	protected volatile boolean running = true;
	
	public void run(int port, String token) throws IOException {
		this.port = port;
		this.token = token;
		
		log.info("Starting on port " + port + ", token " + token);
		final Thread main = Thread.currentThread();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {			
			@Override
			public void run() {
				running = false;
				main.interrupt();
			}
		}));
		
		ServerSocket serverSocket = new ServerSocket(port);
		while (running) {
			Socket clientSocket = serverSocket.accept();
			if (clientSocket != null) {
				handleSocket(clientSocket);
			}
		}
	}

	protected void handleSocket(Socket clientSocket) {
		try {
			OutputStream os = clientSocket.getOutputStream();
			InputStream is = clientSocket.getInputStream();
			InputStreamReader isr = new InputStreamReader(is, "UTF-8");
			BufferedReader in = new BufferedReader(isr);
			String s;

			String path = null;
			while ((s = in.readLine()) != null) {
				s = s.trim();
				if (s.isEmpty()) {
					break;
				} else
				if (s.startsWith("GET /")) {
					path = s.split("[ ]+")[1];
				}
			}

			OutputStreamWriter out = new OutputStreamWriter(os, "UTF-8");
			if (path.startsWith("/" + token + "/")) {
				try {					
					path = path.substring(path.indexOf("/") + 1);
					path = path.substring(path.indexOf("/") + 1);
					path = URLDecoder.decode(path, "UTF-8");
					//path = path.replace('+', ' ');
					log.info("Launching " + path);
					Process p = Runtime.getRuntime().exec(path);
					final StringWriter sw = new StringWriter();
					int res = ProcessMonitor.monitor(p, new ProcessListener() {
						@Override
						public void stdout(String line) {
							try {
								sw.write(line.trim() + "\r\n");
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						
						@Override
						public void stderr(String line) {
							try {
								sw.write(line.trim() + "\r\n");
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
					log.info("Complete " + path + " " + res);
					out.write("HTTP/1.0 200 OK\r\n");
					out.write("Server: WindowsSeviceRunner\r\n");
					out.write("Content-Type: text/plain;charset=UTF-8\r\n");
					out.write("X-Return-Code: " + res + "\r\n");
					out.write("\r\n");
					out.write(sw.toString());
				} catch (Exception e) {
					System.err.println(e);
					int code = 500;
					if (e.getMessage() != null && e.getMessage().indexOf("=2") > 0) {
						code = 404;
					}
					out.write("HTTP/1.0 " + code + " Error\r\n");
					out.write("Server: WindowsSeviceRunner\r\n");
					out.write("Content-Type: text/plain;charset=UTF-8\r\n");
					out.write("\r\n");
					out.write(e.toString());
				}
			} else {
				out.write("HTTP/1.0 500 Error\r\n");
				out.write("Server: WindowsSeviceRunner\r\n");
				out.write("\r\n");
				out.write("Invalid token or path: " + path);				
			}
			out.close();
			os.close();
			clientSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}