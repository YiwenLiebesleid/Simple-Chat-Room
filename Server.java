package com.chatroom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

class Server {
	private ServerSocket serverSocket;
	private int port = 5050;
	private boolean running = false;		// the status of server
	static ArrayList<Socket> sktList = new ArrayList<Socket>();		// keep all the sockets for future reference
	static ArrayList<String> nameList = new ArrayList<String>();	// keep all the names for future reference
	
	/* run server: create and wait for clients */
	public void run() {
		new Thread(() ->{		// use thread here to keep the server GUI listening
			try {
			    serverSocket = new ServerSocket(port);
				running = true;
			    System.out.println("Server created successfully");
			    while(this.isRunning()) {		// keep accepting incoming client connections until closed by administrator
			    	new listenClients(serverSocket.accept()).start();		// open a listen service for this client
			    }
			} catch (IOException e) {
				if(this.sktList != null) {
					try {
						this.serverSocket.close();
					} catch (IOException e1) {	}
				}
			}
		}).start();
	}
	
	/* broadcast message to all clients in client_map */
	public void broadcast(String message) {
		try {
			for (int i = 0; i < sktList.size(); i++) {		// for all sockets in sktList
				Socket skt = sktList.get(i);
				OutputStreamWriter outstream = new OutputStreamWriter(skt.getOutputStream());
				BufferedWriter bw = new BufferedWriter(outstream);
				PrintWriter out = new PrintWriter(bw,true);
				out.println(message);
				out.flush();		// send to one client
			}
		} catch (Exception e) {	}
	}
	
	/* check status of server */
	public boolean isRunning() {
		return this.running;
	}
	
	/* stop the server */
	public void stop() {
		String info = "BROADCAST" + "\n" + "Server disconnected, please reconnect later" + "\n";		// if the server is shut down by administrator
		broadcast(info);
		this.running = false;
		try {
			for (int i = 0; i < sktList.size(); i++) {		// remove all the client records
	            sktList.remove(i);
	            nameList.remove(i);
	        }
			serverSocket.close();		// shut down server
		} catch (Exception e) {	}
	}
	
	/* listen and send info messages from and to clients */
	class listenClients extends Thread {
		private Socket clientSocket;		// client socket
		private String user_name;		// client user name
		private BufferedReader in;

		public listenClients(Socket socket_connection) {		// initialization
			try {
				clientSocket = socket_connection;
				InputStreamReader instream = new InputStreamReader(clientSocket.getInputStream());
				in = new BufferedReader(instream);
			} catch (IOException e) {	}
		}
		
		private boolean loginStatus;
		
		/* exchange messages with clients */
		public void run() {
			try {
				loginStatus = false;
				String info = "";
				String message = "";
				while(true) {		// keep listening from client until client is closed
					info = in.readLine();
					if(info == null)	continue;
					if(loginStatus == false) {		// user hasn't logged in
						
						/* the user is trying to login */
						if(info.equals("LOGIN")) {
							user_name = in.readLine();		// get user name
							if(user_name.contains("+") || user_name.equals("All")) {		// invalid user names
								sendMsg2user(clientSocket, "LOGIN_FAIL_INVALID");
								in.close();		// shut down and send notification
								clientSocket.close();
								break;
							} else if (nameList.contains(user_name)) {
								sendMsg2user(clientSocket, "LOGIN_FAIL_EXIST");		// user name already exists
								in.close();
								clientSocket.close();
								break;
							} else {		// if successfully logged in
								sktList.add(clientSocket);
								nameList.add(user_name);
								message = "LOGINED" + "\n" + user_name;
								sendMsg2All(message);		// inform all the users someone logged in
								refreshUserlist();		// update the user list
								loginStatus = true;
							}
						}
					} else {		// the user has already logged in
						if(info.equals("SEND_MESSAGE")) {
					 		String dest_username = in.readLine();		// get the destination user
					 		if(dest_username.equals("All")) {		// sending message to all other users
					 			message = "MESSAGE_ALL" + "\n" + user_name + "\n" + in.readLine();		// content of message
					 			sendMsg2All(message);
					 		} else {		// sending message to one specific user
					 			message = "MESSAGE" + "\n" + user_name + "\n" + in.readLine();		// content of message
					 			int dest = nameList.indexOf(dest_username);
					 			sendMsg2user(sktList.get(dest),message);
					 		}
					 	} else if(info.equals("LOGOUT")) {		// user logs out
					 		in.close();
					 		clientSocket.close();
					 		sktList.remove(clientSocket);
							nameList.remove(user_name);
					 		message = "LOGOUT" + "\n" + user_name;
					 		sendMsg2All(message);		// inform all the users
					 		refreshUserlist();		// update the user list
					 		break;
					 	}
					}

				}
			} catch (IOException e) {
				sktList.remove(clientSocket);
				nameList.remove(user_name);
		 		refreshUserlist();
			}
		}

		/* to update GUI user list */
		public void refreshUserlist() {
			String message = "USER_LIST" + "\n";
			for (int i = 0; i < nameList.size(); i++) {		// for all sockets in sktList
				String user = nameList.get(i);
				message += user + "+";		// get all user names in message
			}
			broadcast(message);
		}
		
		/* the destination socket to send message */
		public void sendMsg2user(Socket destination, String message) {
			try {
				OutputStreamWriter outstream = new OutputStreamWriter(destination.getOutputStream());
				BufferedWriter bw = new BufferedWriter(outstream);
				PrintWriter out = new PrintWriter(bw,true);
				out.println(message);
				out.flush();
			} catch (Exception e) {	}
		}
		
		/* send message to all other users */
		public void sendMsg2All(String message) {
			try {
				for (int i = 0; i < sktList.size(); i++) {		// for all sockets in sktList
					Socket skt = sktList.get(i);
					sendMsg2user(skt,message);		// call function sent2user above
				}
			} catch (Exception e) {	}
		}
	}
}
