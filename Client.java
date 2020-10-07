package com.chatroom;

import java.net.Socket;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.Font;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
import java.io.IOException;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.text.StyleConstants;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.text.SimpleAttributeSet;

public class Client extends JFrame implements ActionListener {
	private static final long serialVersionUID = 1L;
	
	JTextPane text_history;
	Document chatting_history;
	JLabel label_online;
	JTextPane online_users;
	Document list_of_users;
	JTextArea input_area;
	JButton button_send;
	JLabel label_sendto;
	JComboBox<String> box_users;
	
	SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss");		// show current time
	
	SimpleAttributeSet attr_cmd = new SimpleAttributeSet();
	SimpleAttributeSet attr_notice = new SimpleAttributeSet();
	SimpleAttributeSet attr_time = new SimpleAttributeSet();
	SimpleAttributeSet attr_username = new SimpleAttributeSet();
	SimpleAttributeSet attr_msg = new SimpleAttributeSet();
	SimpleAttributeSet attr_users = new SimpleAttributeSet();

	private Socket socket;
	private String user_name;
	PrintWriter out = null;
	
	public static void main(String args[]) {
		try {
			Locale.setDefault(new Locale("en","US"));
			String username = JOptionPane.showInputDialog(null,"User name：","Login",JOptionPane.PLAIN_MESSAGE);
			if(username == null || username.length() == 0)	return;		// invalid input
			Socket socket = new Socket("127.0.0.1", 5050);
			new Client(username, socket);		// create new client
			
		} catch (Exception e) {		// if the server is disconnected, return error message
			JOptionPane.showMessageDialog(null, "Connection Refused", "Error - 503", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/* client interface */
	public Client(String username, Socket skt) throws IOException {
		super("Chatroom Port 5050 - user : " + username);		// set title
		
		user_name = username;
		socket = skt;

		OutputStreamWriter outstream = new OutputStreamWriter(socket.getOutputStream());
		BufferedWriter bw = new BufferedWriter(outstream);
		out = new PrintWriter(bw,true);
		
		this.setLayout(null);
		this.setBounds(600, 300, 700, 450);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.addWindowListener(new CloseHandler());	
		this.setResizable(false);
		
		/* text history:
		 * not editable
		 * scroll
		 *  */
		text_history = new JTextPane();
		text_history.setBounds(10, 5, 480, 290);
		chatting_history = text_history.getDocument();
		JScrollPane scroll_text_history = new JScrollPane(text_history);
		scroll_text_history.setBounds(10, 5, 480, 290);
		text_history.setEditable(false);

		/* user list */
		label_online = new JLabel("Chatroom Users");
		label_online.setFont(new Font(null,1,14));
		label_online.setBounds(520, 5, 120, 15);
		online_users = new JTextPane();
		online_users.setBounds(500, 25, 180, 270);
		online_users.setForeground(Color.DARK_GRAY);
		list_of_users = online_users.getDocument();
		JScrollPane scroll_users = new JScrollPane(online_users);
		scroll_users.setBounds(500, 25, 180, 270);
		online_users.setEditable(false);

		/* input message 
		 * scroll
		 * shouldn't be enabled until user logged in
		 * */
		input_area = new JTextArea();
		input_area.setBounds(10, 300, 480, 100);
		input_area.setFont(new Font(null,0,16));
		JScrollPane scroll_text_in = new JScrollPane(input_area);
		scroll_text_in.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll_text_in.setBounds(10, 300, 480, 100);
		input_area.setLineWrap(true);

		/* "send" button
		 * to allow new-line inside text area,
		 * set alt+enter to send message
		 *  */
		button_send = new JButton("Send (alt+Enter)");
		button_send.setBounds(520, 370, 130, 25);
		button_send.addActionListener(this);
		button_send.setMnemonic(KeyEvent.VK_ENTER);		// set Alt+Enter to send message

		/* "send to" label and select destination user */
		label_sendto = new JLabel("Send to:");
		label_sendto.setFont(new Font(null,1,15));
		label_sendto.setBounds(520, 300, 100, 25);
		box_users = new JComboBox<String>();
		box_users.setBounds(520, 330, 130, 25);

		button_send.setEnabled(false);
		this.add(scroll_text_history);
		this.add(scroll_text_in);
		this.add(label_online);
		this.add(scroll_users);
		this.add(label_sendto);
		this.add(box_users);
		this.add(button_send);
		
		this.setVisible(true);
		
		/* set up attributes */
		StyleConstants.setForeground(attr_cmd, Color.GRAY);	// information
		StyleConstants.setBold(attr_cmd, true);
		StyleConstants.setForeground(attr_notice, Color.RED);	// notice
		StyleConstants.setFontSize(attr_notice, 14);
		StyleConstants.setBold(attr_notice, true);
		StyleConstants.setForeground(attr_time, Color.GRAY);	// time
		StyleConstants.setFontSize(attr_time, 10);
		StyleConstants.setForeground(attr_username, Color.BLUE);	// user name
		StyleConstants.setFontSize(attr_username, 12);
		StyleConstants.setBold(attr_username, true);
		StyleConstants.setForeground(attr_msg, Color.BLACK);	// message
		StyleConstants.setFontSize(attr_msg, 15);
		StyleConstants.setForeground(attr_users, Color.DARK_GRAY);	// user list
		StyleConstants.setFontSize(attr_users, 14);
		
		new listenServer(user_name, socket).start();	// keep listening from and sending to server
	}
	
	class CloseHandler extends WindowAdapter {		// when the window is closed
		public void windowClosing(final WindowEvent event) {
			try {
				out.println("LOGOUT");		// send logout information to others
				out.flush();
				out.close();
				socket.close();
				Server.sktList.remove(socket);
				Server.nameList.remove(user_name);
			} catch (IOException e) {	}
		}
	}
	
	/* put message onto chat history board */
	public void updateChat(String message, SimpleAttributeSet attrset) {
		try {
			chatting_history.insertString(chatting_history.getLength(), message, attrset);
		} catch (Exception e) { }
	}

	/* maintain the user list board */
	public void maintainUsers(String username, SimpleAttributeSet attrset) {
		try {
			list_of_users.insertString(list_of_users.getLength(), username + " - online\n", attrset);
		} catch (Exception e) { }
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {		// add button listener event
		if(e.getActionCommand().equals("Send (alt+Enter)")) {
			String user_sendto = (String)box_users.getSelectedItem();		// get destination user's name
			
			if(user_sendto == null) return;
			else if(user_sendto.equals("All")) {
				updateChat("You", attr_username);
				updateChat(" : ", attr_cmd);
			} else {
				updateChat("You", attr_username);
				updateChat(" speak to ", attr_cmd);
				updateChat(user_sendto, attr_username);
				updateChat(": ", attr_cmd);
			}
			String message = input_area.getText();		// get the text user input
			input_area.setText("");		// clear up input area
			updateChat(message + " - ", attr_msg);
			updateChat("(" + df.format(new Date()) + ")\n", attr_time);
			
			out.println("SEND_MESSAGE");		// command: to send message
			out.println(user_sendto);		// the destination to send to
			out.println(message.replaceAll("\\n", "\\\\n"));		// in order to enable new-line
			out.flush();
		}
	}

	/* listen and send info messages from and to server */
	class listenServer extends Thread {
		private Socket socket;
		private String user_name;
		private BufferedReader in;
		private PrintWriter out;
		
		public listenServer(String username, Socket skt) {
			try {
				initial(username, skt);
			} catch (IOException e) { }
		}
		
		private void initial(String username, Socket skt) throws IOException {
			this.user_name = username;
			this.socket = skt;
			InputStreamReader instream = new InputStreamReader(skt.getInputStream());
			this.in = new BufferedReader(instream);
			OutputStreamWriter outstream = new OutputStreamWriter(skt.getOutputStream());
			BufferedWriter bw = new BufferedWriter(outstream);
			this.out = new PrintWriter(bw,true);
		}
		
		public void run() {
			try {
				String info = "LOGIN" + "\n" + user_name + "\n";
				out.print(info);
				out.flush();		// inform the user to log in
				while(!socket.isClosed()) {
					info = in.readLine();
					if(info == null)	continue;
					switch(info) {
						case "MESSAGE":{
							String user_from = in.readLine();
							String message = in.readLine().replaceAll("\\\\n", "\n");
							if(!user_from.equals(user_name)) {		// doesn't need to send to himself
								updateChat(user_from, attr_username);
								updateChat(" speaks to", attr_cmd);
								updateChat(" You", attr_username);
								updateChat(" : ", attr_cmd);
								updateChat(message + " - ", attr_msg);
								updateChat("(" + df.format(new Date()) + ")\n", attr_time);
							}
							break;
						}
						case "MESSAGE_ALL":{		// send to all
							String user_from = in.readLine();
							String msg = in.readLine().replaceAll("\\\\n", "\n");
							if(!user_from.equals(user_name)) {
								updateChat(user_from, attr_username);
								updateChat(" : ", attr_cmd);
								updateChat(msg + " - ", attr_msg);
								updateChat("(" + df.format(new Date()) + ")\n", attr_time);
							}
							break;
						}
						case "BROADCAST":{
							updateChat("[Notice] " + in.readLine() + " ", attr_notice);
							updateChat("(" + df.format(new Date()) + ")\n", attr_time);
							input_area.setEnabled(false);
							button_send.setEnabled(false);
							break;
						}
						case "USER_LIST":{		// update user list
							String user_list = in.readLine();
							String users[] = user_list.split("\\+");
							
							int len = list_of_users.getLength();
							box_users.removeAllItems();
							box_users.addItem("All");
							try {
								list_of_users.remove(0, len);
							} catch (Exception e) { }		// an option of sending message to all users
							for(int i = 0; i < users.length; i++) {
								maintainUsers(users[i], attr_users);		// maintain the list
								if(!users[i].equals(user_name))	box_users.addItem(users[i]);
							}
							break;
						}
						case "LOGINED":{		// log in information
							System.out.println("Client created successfully");
							String name = in.readLine();		// get the new user's name
							updateChat("[INFO] ", attr_notice);
							updateChat(name, attr_username);
							updateChat(" entered the room. - ", attr_cmd);
							updateChat("(" + df.format(new Date()) + ")\n", attr_time);
							input_area.setEnabled(true);
							button_send.setEnabled(true);
							break;
						}
						case "LOGOUT":{		// receiving leaving of other users
							String name = in.readLine();		// get the user's name
							updateChat("[INFO] ", attr_notice);
							updateChat(name, attr_username);
							updateChat(" left the room. - ", attr_cmd);
							updateChat("(" + df.format(new Date()) + ")\n", attr_time);
							break;
						}
						case "LOGIN_FAIL_INVALID":{		// log in failed information, return error message
							updateChat(user_name + ": Login Failed\n", attr_cmd);
							JOptionPane.showMessageDialog(null, "Invalid Username!", "Error - 401", JOptionPane.ERROR_MESSAGE);
							System.exit(0);
							break;
						}
						case "LOGIN_FAIL_EXIST":{		// log in failed information, return error message
							updateChat(user_name + ": Login Failed\n", attr_cmd);
							JOptionPane.showMessageDialog(null, "Username Occupied!", "Error - 401", JOptionPane.ERROR_MESSAGE);
							System.exit(0);
							break;
						}
					}
				}
			} catch (IOException e) {
				try {
					this.socket.close();		// close socket
				} catch (IOException e1) { }
			}
		}
	}
}
