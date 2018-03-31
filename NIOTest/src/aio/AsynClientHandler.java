package aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;


public class AsynClientHandler implements Runnable{
	
	public static void main(String[] args){

		//while(true){
			new Thread(new AsynClientHandler(9034)).start();
		//}
	}
	
	private String host = "127.0.0.1";
	private int port = 9034;
	private CountDownLatch latch;
	private AsynchronousSocketChannel asynchronousSocketChannel;
	
	public AsynClientHandler(int port) {
		this.port = port;
		
		try {
			asynchronousSocketChannel = AsynchronousSocketChannel.open();
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void run(){
		doConnect();
		latch = new CountDownLatch(1);
		try{
			latch.await();
		}catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
	}
	
	private void doConnect(){
		asynchronousSocketChannel.connect(new InetSocketAddress(host, port), asynchronousSocketChannel, new ConnectCompletionHandler(this));
	}
	
	public CountDownLatch getLatch(){
		return latch;
	}
	


}

class ConnectCompletionHandler implements CompletionHandler<Void, AsynchronousSocketChannel>{
	
	private AsynClientHandler client= null;
	
	public ConnectCompletionHandler(AsynClientHandler asynClientHandler) {
		this.client = asynClientHandler;
	}


	@Override
	public void completed(Void result, AsynchronousSocketChannel clientChannel) {
		byte[] reqest = "order from client".getBytes();
		ByteBuffer byteBuffer = ByteBuffer.allocate(reqest.length);
		byteBuffer.put(reqest);
		byteBuffer.flip();
		clientChannel.write(byteBuffer,byteBuffer,new CompletionHandler<Integer,ByteBuffer>(){

			@Override
			public void completed(Integer result, ByteBuffer buffer) {
				//写成功方法
				if(buffer.hasRemaining()){
					clientChannel.write(buffer,buffer,this);
				}else{
					//开始异步读
					ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
					clientChannel.read(byteBuffer, byteBuffer, new CompletionHandler<Integer, ByteBuffer>() {

						@Override
						public void completed(Integer result, ByteBuffer buffer) {
							//读成功方法
							buffer.flip();
							byte[] bytes = new byte[buffer.remaining()];
							buffer.get(bytes);
							try {
								String body = new String(bytes, "utf-8");
								System.out.println("Client recepted!\n"+body);
								client.getLatch().countDown();
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
							
							
						}

						@Override
						public void failed(Throwable exc, ByteBuffer buffer) {
							try {
								clientChannel.close();
								client.getLatch().countDown();
							} catch (IOException e) {
								e.printStackTrace();
							}
							exc.printStackTrace();
							
						}
					});
					
					
					
				}
				
			}

			@Override
			public void failed(Throwable exc, ByteBuffer buffer) {
				try {
					clientChannel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				exc.printStackTrace();
				
			}
			
		});
		
	}

	@Override
	public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
		System.out.println("连接失败");
		exc.printStackTrace();
		try {
			attachment.close();
			client.getLatch().countDown();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

	
}


