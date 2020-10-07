package com.chatroom;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class ServerView {

    private Server cserver;		// server
    private JFrame jf;		// server interface
    private JButton button_start;

    public static void main(String[] args) {
        ServerView widget = new ServerView();
        widget.showUI();
    }

    /* UI panel setup */
    public void showUI() {
        jf = new JFrame("Server System");
        jf.setSize(400, 200);
        jf.setLayout(null);
		jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
        JLabel la_port = new JLabel("Server Port: 5050");	// panel and title
        la_port.setFont(new Font(null,1,14));
        la_port.setBounds(120, 20, 150, 20);
        jf.add(la_port);
        
        button_start = new JButton("Start server");		// button for starting and stopping server
        button_start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                serverControl();
            }
        });
		button_start.setBounds(120, 60, 120, 30);
        jf.add(button_start);
        
        jf.setLocation(600,400);		// locate widget
        jf.setVisible(true);
        jf.setDefaultCloseOperation(3);		// close and leave the program

    }

    /* Server control button */
    private void serverControl() {
        if (cserver == null || cserver.isRunning() == false) {		// if the server is shut down or is not set up
            cserver = new Server();
			jf.setTitle("Server: running");		// change title message
			button_start.setText("Stop server");		// change the control button
			cserver.run();
            
        } else if (cserver.isRunning()) {		// if the server is running
            jf.setTitle("Server: halt");		// change title message
            button_start.setText("Start server");		// change the control button
        	cserver.stop();
            cserver = null;
        }

    }

}
