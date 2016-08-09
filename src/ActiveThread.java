import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.security.*;
import java.security.interfaces.*;

import sun.misc.*;

import java.security.spec.*;

import javax.crypto.Cipher;

/* 
 * This is the thread used for client side 
 * It will connect to the server, and then will chat with the server 
 */
public class ActiveThread extends Thread {
	
	// So that we can call the function of the main frame 
	public MainFrame frame;
	
	public ActiveThread(MainFrame frame) {
		this.frame = frame;
	}
	
	// There is no accept event in client side 
	// Handle the read event 
	public void handleRead(SelectionKey key) throws Exception {
		ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
		SocketChannel socketChannel = (SocketChannel)key.channel(); 
		
		int readBytes = socketChannel.read(byteBuffer);
		if(readBytes > 0) {
			byteBuffer.flip();
			if (!getkey) {
				
				// Try to read the public key string first 
				String publickeystring = new String(byteBuffer.array());
				publickey = getPublicKey(publickeystring);
				// Already get the public key we can initialize the cipher here 
				initCipher();
				getkey = true;
				
			} else {
				
				// First receive the byte array, then do decode 
				byte[] content = new byte[readBytes];
				byteBuffer.get(content, 0, readBytes);
				String msg = new String(decode.doFinal(content));
				// Show received message 
				frame.addRecvMsg(msg);				
			}
		} else {
			// Can not be read any more, we need to close the socket 
			socketChannel.close();
		}
	}  
	
	// Handle the write event 
	public void handleWrite(SelectionKey key) throws Exception {  
		SocketChannel socketChannel = (SocketChannel)key.channel();  
		
		// Need to send message to this channel 
		String msg = frame.getNextSendMsg();
		if (msg != null) {
			// Need to encode before sending 
			ByteBuffer buf = ByteBuffer.wrap(encode.doFinal(msg.getBytes()));
			socketChannel.write(buf);  				
		}
	}		
	
	// The public key used by the client side 
	private RSAPublicKey publickey;
	private Cipher encode;
	private Cipher decode;
	private boolean getkey;		// already get the public key 
	
	// Initialize the cipher 
	public void initCipher() throws Exception {
		encode = Cipher.getInstance("RSA");
		decode = Cipher.getInstance("RSA");
		encode.init(Cipher.ENCRYPT_MODE, publickey);
		decode.init(Cipher.DECRYPT_MODE, publickey);
	}
	
	// Restore the public key from the content received from the server side 
	public static RSAPublicKey getPublicKey(String key) throws Exception {
        byte[] keyBytes;
        keyBytes = (new BASE64Decoder()).decodeBuffer(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey)keyFactory.generatePublic(keySpec);
    }
	
	public void run() {
		// Here we need to connect to server first, and then try to send or receive message 
		try {			
			InetSocketAddress inetSocketAddress = new InetSocketAddress(frame.getIP(), MainFrame.PORT);
			Selector selector = Selector.open();	// open the selector 
			SocketChannel socketChannel = SocketChannel.open(inetSocketAddress);  
			socketChannel.configureBlocking(false);
			socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE); 	// register read and write event 
			
			while(true) {	// main loop  
				int nKeys = selector.select();  
				if(nKeys > 0) {  
					Set<SelectionKey> selectedKeys = selector.selectedKeys();  
					Iterator<SelectionKey> it = selectedKeys.iterator();  
					while(it.hasNext()) {  
						SelectionKey key = it.next();  
						if(key.isReadable()) {  
							handleRead(key);  
						} else if(key.isWritable()) {  
							handleWrite(key);  
						}  
						it.remove();  
					}  
				}  
			}
		} catch (IOException e) {  
			e.printStackTrace();
			frame.setFatalErrMsg("Client communication error, need quit!");
		} catch (Exception e) {
			e.printStackTrace();
			frame.setFatalErrMsg("Get RSA public key failed, need quit!");
		}
	}
}
