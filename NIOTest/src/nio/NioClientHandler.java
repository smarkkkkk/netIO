package nio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioClientHandler implements Runnable{

	private String host;
	private int port;
	private Selector selector;
	private SocketChannel sc;
	private volatile boolean stop;
	
	public NioClientHandler(String host, int port) {
		
		this.host = host;
		this.port = port;
		try{
			selector = Selector.open();
			sc = SocketChannel.open();
			sc.configureBlocking(false);
		}catch (IOException e) {
			e.printStackTrace();
		}
	
	}
	
	
	@Override
	public void run() {
		try{
			doConnect();
		}catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		while(!stop){
			
			try{
				selector.select(1000);
				Set<SelectionKey> selectionKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectionKeys.iterator();
				SelectionKey curKey = null;
				while(it.hasNext()){
					curKey =  it.next();
					it.remove();
					try{
						handleInput(curKey);
					}catch (Exception e) {
						if(curKey!=null){
							curKey.cancel();
							if(curKey.channel()!=null){
								curKey.channel().close();
							}
						}
					}

				}
			}catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		if(selector!=null){
			try{
				selector.close();
			}catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
	}
	
	private void handleInput(SelectionKey key) throws IOException{
		
		if(key.isValid()){
			SocketChannel sc = (SocketChannel)key.channel();
			if(key.isConnectable()){
				if(sc.finishConnect()){
					sc.register(selector, SelectionKey.OP_READ);
					doWrite(sc);
				}else{
					System.exit(1);
				}
				
			}
			if(key.isReadable()){
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes = sc.read(readBuffer);
				if(readBytes>0){
					readBuffer.flip();
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String body = new String(bytes,"utf-8");
					System.out.println(body);
					this.stop = true;
				}else if(readBytes<0){
					key.cancel();
					sc.close();
				}
			}
			
		}

		
	}
	
	private void doConnect() throws IOException{
		
		if(sc.connect(new InetSocketAddress(host, port))){
			sc.register(selector, SelectionKey.OP_READ);
			doWrite(sc);
		}else{
			sc.register(selector, SelectionKey.OP_CONNECT);
		}
		
	}
	
	private void doWrite(SocketChannel sc) throws IOException{
		
		byte[] req = "Information Query order".getBytes();
		ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
		writeBuffer.put(req);
		writeBuffer.flip();
		sc.write(writeBuffer);
		if(!writeBuffer.hasRemaining()){
			System.out.println("Send order 2 server succeed!");
		}
		
				
	}
	
	public static void main(String[] args){
		
		String host = "127.0.0.1";
		int port = 9034;
		while(true){
			new Thread(new NioClientHandler(host, port)).start();
		}
	}
	
	

}
