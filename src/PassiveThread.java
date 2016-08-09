import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.security.*;
import java.security.interfaces.*;
import sun.misc.*;
import javax.crypto.Cipher;

/*
 *  This is the thread used for server side 
 *  It will bind the given port and then listen to it. After the active user connection, it can communicate with the active user  
 */

public class PassiveThread extends Thread {
	
	// So that we can call the function of the main frame 
	public MainFrame frame;
	
	public PassiveThread(MainFrame frame) {
		this.frame = frame;
	}
	
	private SocketChannel curchannel;	// Record the current channel 
	
	// Handle the accept event of server 
	public void handleAccept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();  
		SocketChannel socketChannel = serverSocketChannel.accept();  
		
		// We do not allow 2 clients connect to the same user 
		if (curchannel != null) {
			throw new IOException("Do not allow 2 clients connect to same server!");
		}
		curchannel = socketChannel;		// Just need to send message to this channel 
		// Coz we use NIO, so need to set the non-block mode for each socket 
		socketChannel.configureBlocking(false);
		socketChannel.register(key.selector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		
		// Always try to send the public key string to the client 
		ByteBuffer buf = ByteBuffer.wrap(publickeystring.getBytes());
		socketChannel.write(buf);
	}  
	
	// Handle the read event
	public void handleRead(SelectionKey key) throws IOException {
		// always read the real content of send message, that is a bit different from the client side
		ByteBuffer byteBuffer = ByteBuffer.allocate(128);
		SocketChannel socketChannel = (SocketChannel)key.channel(); 

		if (socketChannel != curchannel) {
			throw new IOException("Fatal IO error!");
		}
		
		// If there is some exception, we need to close this connection 
		boolean needclose = false;
		try { 
			int readBytes = socketChannel.read(byteBuffer);
			if(readBytes > 0) {
				// To reset the right position of the Bytebuffer
				byteBuffer.flip();
				// First get the array content, then decode the array, we should  
				byte[] content = new byte[readBytes];
				byteBuffer.get(content, 0, readBytes);
				String msg = new String(decode.doFinal(content));
				// Show this message 
				frame.addRecvMsg(msg);
			} else { 
				needclose = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			needclose = true;
		}
		if (needclose ) { 
			socketChannel.close();
			curchannel = null;		// clear the current channel 						
		}
	}  
	
	// Handle the write event 
	public void handleWrite(SelectionKey key) throws IOException {  
		SocketChannel socketChannel = (SocketChannel)key.channel();  
		
		if (socketChannel == curchannel) {
			// Need to send message to this channel 
			String msg = frame.getNextSendMsg();
			if (msg != null) {
				try {
					// First the the write content then encode the content 
					ByteBuffer buf = ByteBuffer.wrap(encode.doFinal(msg.getBytes()));
					socketChannel.write(buf);  	
				} catch (Exception e) {
					e.printStackTrace();
					socketChannel.close();
					curchannel = null;		// clear the current channel 				
				}
			}
		}		
	}  
	
	// Send to client so that it can construct the public key 
	private String publickeystring;	
	// The private key used by the server side 
	private RSAPrivateKey privatekey;
	
	// Server is responsible for generate the RSA key pair, and the public key used for client side, while the private key used by self
	public void generateKey() throws Exception { 
		KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");		// RSA key generator 
        // the length of keys 
		keyPairGen.initialize(1024);
        // the key pair 
        KeyPair keyPair = keyPairGen.generateKeyPair();
        publickeystring = getKeyString(keyPair.getPublic());
        privatekey = (RSAPrivateKey)keyPair.getPrivate();
	}
	
	// Get the key string from public key or private key
	public static String getKeyString(Key key) throws Exception {
        byte[] keyBytes = key.getEncoded();
        String s = (new BASE64Encoder()).encode(keyBytes);		// base64 encode 
        return s;
	}
	
	private Cipher encode;
	private Cipher decode;
	
	// Initialize the cipher 
	public void initCipher() throws Exception {
		encode = Cipher.getInstance("RSA");
		decode = Cipher.getInstance("RSA");
		encode.init(Cipher.ENCRYPT_MODE, privatekey);
		decode.init(Cipher.DECRYPT_MODE, privatekey);
	}
	
	public void run() {
		// Here this program will act as server, create socket, bind port, and listen to it
		try {		
			// Server generate the key pair first, and then initialize the cipher 
			generateKey();
			initCipher();
			
			// Bind the port, and then listen to it 
			InetSocketAddress inetSocketAddress = new InetSocketAddress(MainFrame.PORT);
			Selector selector = Selector.open();	// open the selector 
			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();	// open the channel 
			serverSocketChannel.socket().bind(inetSocketAddress);  
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); // register accept event 
			
			while(true) {	// main loop  
				int nKeys = selector.select();  
				if(nKeys > 0) {  
					Set<SelectionKey> selectedKeys = selector.selectedKeys();  
					Iterator<SelectionKey> it = selectedKeys.iterator();
					while(it.hasNext()) {  
						SelectionKey key = it.next();  
						if(key.isAcceptable()) {  
							handleAccept(key);  
						} else if(key.isReadable()) {  
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
			frame.setFatalErrMsg("Server communication error, need quit!");
		} catch (Exception e) {
			e.printStackTrace();
			frame.setFatalErrMsg("Can not generate RSA keys, need quit!");
		}
	}
}
