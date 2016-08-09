import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class MainFrame extends JFrame {

	// The fixed port use for this project 
	public final static int PORT = 2001;
	// The maximum length of message is 117 bytes, if the message to be sent is longer than that, we will split it into 2 messages or more 
	// Because RSA encryption and decryption can not deal with message longer than that. we make it 100 here 
	public final static int MAX_MSG_LEN = 100;
	
	// Some control units in the UI interface 
	private JTextField ipfield;
	private JTextArea msghistory;
	private JButton ipbtn;
	private JTextField msgfield;
	private JButton sendbtn;
	
	// Create a lock, because there are 2 threads in the application, we use lock to synchronize 
	private Object lock = new Object();
	// To save all the send messages, we send the message one by one 
	private Vector<String> sendmsg = new Vector<String> ();

	// The input IP address 
	private String ipadd;
	
	// Get IP address 
	public String getIP() {
		return ipadd;
	}
	
	// Get the next message need to be sent 
	public String getNextSendMsg() {
		String msg = null;
		synchronized(lock) {
			if (sendmsg.size() > 0) {
				msg = sendmsg.elementAt(0);
				// Now need to remove it, so that it can not get again 
				sendmsg.remove(0);
			}
		}
		return msg;
	}
	
	// Add message to send message list 
	public void addSendMsg(String msg) {
		msg = msg.trim();
		synchronized(lock) {
			int index = 0; 
			int msglen = msg.length();
			while (index < msglen) {
				// Here we try to check whether the send message is long enough, if too long, we will split it into several sub messages 
				int needsend = (msglen - index > MAX_MSG_LEN) ? MAX_MSG_LEN : (msglen - index);
				String submsg = msg.substring(index, index + needsend);
				sendmsg.add(submsg);
				index += needsend;
				
				System.out.println("Send msg length: " + needsend);
				
				msghistory.append("Self : " + submsg + "\r\n");
			}
		}		
	}
	
	// Show message to message history 
	public void addRecvMsg(String msg) {
		synchronized(lock) {
			msghistory.append("Other: " + msg + "\r\n");
		}
	}
	
	// Clear the message history and show the fatal error message 
	public void setFatalErrMsg(String msg) {
		synchronized(lock) {
			msghistory.setText(msg);
		}
	}
	
	// Check whether the input is valid IP address 
	public static boolean isValidIP(String ip) {
	    ip = ip.trim();
		String[] subs = ip.split("\\.");
		// IPv4 or IPv6
		if (subs.length != 4 && subs.length != 6)
			return false;
		try {
			for (int x = 0; x < subs.length; x ++) {
				int a=Integer.valueOf(subs[x]);
				// Each number must be in range of [0,255]
				if (!(a >= 0 && a <= 255))	
					return false;
			}
		} catch (Exception e) {
			return false;
		}
	    return true;
	}
		
	// Start another thread to deal with the IO operation 
	public void startThread() {
		if (ipadd.equals("0.0.0.0")) {	// act as server side 
			new PassiveThread(this).start();
		} else {						// act as client side 
			new ActiveThread(this).start();
		}
	}
	
	public MainFrame() {
		this.setSize(450, 450);
		this.getContentPane().setLayout(null);	// do not use any layout 

		// we add IP label 
		JLabel label0 = new JLabel();
		label0.setBounds(40, 40, 370, 20);
		label0.setText("Input IP address(If you are connected, please input 0.0.0.0):");
		this.add(label0, null);	
		
		// we add the text field for input IP
		ipfield = new JTextField();
		ipfield.setBounds(40, 70, 370, 20);
		this.add(ipfield, null);
		
		// we add the button to confirm the IP address   
		ipbtn = new JButton();
		ipbtn.setBounds(180, 100, 90, 30);
		ipbtn.setText("Set");
		ipbtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Here we will test whether the input IP is ok, then try to start a new thread to act as server or client 
				String ip = ipfield.getText().trim();
				if (isValidIP(ip)) {
						ipadd = ip;
						
						// clear the message 
						msghistory.setText("");	
						// disable the IP address input 
						ipfield.setEnabled(false);
						ipbtn.setEnabled(false);
						// now let the message can be send 
						msgfield.setEnabled(true);
						sendbtn.setEnabled(true);
						
						// now we start a new thread to do the IO operation
						startThread();
				} else {
					// should hint wrong IP, need re-input 
					setFatalErrMsg("Invalid ip address, Please re input.");
					// clear the IP address for re-input 
					ipfield.setText("");
				}
			}
		}); // bind the action to this button 
		this.add(ipbtn, null);	
		
		// here we add some label 
		JLabel label1 = new JLabel();
		label1.setBounds(40, 140, 370, 20);
		label1.setText("Input send message:");
		this.add(label1, null);	
		
		// here we add the text field to input message 
		msgfield = new JTextField();
		msgfield.setBounds(40, 170, 370, 20);
		this.add(msgfield, null);
		msgfield.setEnabled(false);
		
		// here we add the button to send message 
		sendbtn = new JButton();
		sendbtn.setBounds(180, 200, 90, 30);
		sendbtn.setText("Send");
		sendbtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String content = msgfield.getText();
				msgfield.setText("");
				addSendMsg(content);
			}
		}); // bind the action to this button 
		this.add(sendbtn, null);	
		sendbtn.setEnabled(false);
		
		// here we add some label 
		JLabel label2 = new JLabel();
		label2.setBounds(40, 240, 370, 20);
		label2.setText("Message history:");
		this.add(label2, null);	
		
		// here we add the text field to show the message history  
		msghistory = new JTextArea();
		JScrollPane scroll = new JScrollPane(msghistory);		// we make it can scroll 
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setBounds(40, 270, 370, 120);
		msghistory.setBackground(Color.GRAY);
		// make it can not be editable 
		msghistory.setEnabled(false);
		this.add(scroll, null);
		
		this.setTitle("SecureMessage");
		this.setResizable(false); 	// can not change the size 
	}

	public static void main(String args[])throws Exception {
		MainFrame frame = new MainFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	// make the program exit when close 
		frame.setVisible(true);
	}
}
