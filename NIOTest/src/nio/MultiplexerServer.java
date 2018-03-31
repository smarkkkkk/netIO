package nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerServer  implements Runnable{
	
	private Selector selector;
	private ServerSocketChannel servChannel;
	
	private volatile boolean stop = false;
	
	public MultiplexerServer(int port) {
		try{
			selector = Selector.open();
			servChannel = ServerSocketChannel.open();
			servChannel.configureBlocking(false);
			servChannel.socket().bind(new InetSocketAddress(port), 1024);
			//ServerSocketChannel注册反应堆，监听连接事件
			servChannel.register(selector, SelectionKey.OP_ACCEPT);
			
		}catch (IOException e) {
			System.exit(1);
			e.printStackTrace();
		}

	}
	
	public void stop(){
		this.stop = true;
	}
	

	@Override
	public void run() {
		while(!stop){
			try{
				selector.select(1000);
				//Channel中的事件以SelectionKey的方式表示
				Set<SelectionKey> selectionKeys = selector.selectedKeys();
				//轮询集合中的keys
				Iterator<SelectionKey> it = selectionKeys.iterator();				
				SelectionKey curKey = null;
				while(it.hasNext()){
					curKey = it.next();
					it.remove();
					try{
						handleInput(curKey);
					}catch (IOException e) {
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
			}
		}
		
	}
	
	private void handleInput(SelectionKey key) throws IOException{
		
		if(key.isValid()){
			//当监听到连接请求事件，进行处理
			if(key.isAcceptable()){
				ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
				SocketChannel sc = ssc.accept();
				sc.configureBlocking(false);
				//连接建立后，对SocketChannel注册反应堆，监听读消息事件
				sc.register(selector, SelectionKey.OP_READ);
				System.out.println("已连接");
			}
			//当监听到读消息请求（收到数据），进行处理
			if(key.isReadable()){
				SocketChannel sc = (SocketChannel)key.channel();
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes = sc.read(readBuffer);
				if(readBytes>0){
					readBuffer.flip();
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String body = new String(bytes, "utf-8");
					System.out.println(new Date(System.currentTimeMillis())+":"+body);
					String response = new Date(System.currentTimeMillis())+" recepted!\r\n";
					doWrite(sc, response);
					System.out.println("Server: message send!");
				}

			}
		}
		
	}
	
	private void doWrite(SocketChannel sc, String response) throws IOException{
		
		if(response!=null){
			
			byte[] bytes = response.getBytes();
			ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
			writeBuffer.put(bytes);
			writeBuffer.flip();
			sc.write(writeBuffer);
			
		}

		
	}
	
	
	public static void main(String[] args){
		
		int port = 9034;
		MultiplexerServer multiplexerServer = new MultiplexerServer(port);
		new Thread(multiplexerServer).start();
	}

}
