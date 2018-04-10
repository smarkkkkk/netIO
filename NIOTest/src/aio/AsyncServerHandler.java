package aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Date;
import java.util.concurrent.CountDownLatch;


public class AsyncServerHandler implements Runnable{
	
	public static void main(String[] args){
		
		AsyncServerHandler asyncServerHandler = new AsyncServerHandler(9034);
		new Thread(asyncServerHandler).start();

	}

	private int port;
	CountDownLatch latch;
	AsynchronousServerSocketChannel asynchronousServerSocketChannel;
	
	public AsyncServerHandler(int port) {
		this.port = port;
		try{
			asynchronousServerSocketChannel = AsynchronousServerSocketChannel.open();
			asynchronousServerSocketChannel.bind(new InetSocketAddress(port));
			System.out.println("server start at port "+this.port);
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run(){
		
		latch = new CountDownLatch(1);
		doAccept();
		try{
			latch.await();
		}catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	public void doAccept(){
		//回调，把this当做attachment传入，在CompletionHandler中处理
		asynchronousServerSocketChannel.accept(this, new AcceptCompletionHandler());
		System.out.println("doAccept");
		//while(true);
	}
}

class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, AsyncServerHandler>{

	@Override
	public void completed(AsynchronousSocketChannel result, AsyncServerHandler attachment) {
		System.out.println("accept completed.");
		//调用回调,继续监听
		attachment.asynchronousServerSocketChannel.accept(attachment, this);
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		//buffer作为读入的数据流，又作为attacment传入ReadCompletionHandler.
		//此处对连接成功的结果result而言，是回调，把result传入RCH对象，在RCH中调用result实现写操作
		result.read(buffer,buffer,new ReadCompletionHandler(result));
	}

	@Override
	public void failed(Throwable exc, AsyncServerHandler attachment) {
		exc.printStackTrace();
		System.out.println("accept failed.");
		attachment.latch.countDown();
		
	}
	
}

class ReadCompletionHandler implements CompletionHandler<Integer, ByteBuffer>{
	
	private AsynchronousSocketChannel channel;
	
	public ReadCompletionHandler(AsynchronousSocketChannel channel) {
		this.channel = channel;
	}

	@Override
	public void completed(Integer result, ByteBuffer attachment) {
		attachment.flip();
		byte[] body = new byte[attachment.remaining()];
		attachment.get(body);
		try{
			String req = new String(body,"utf-8");
			System.out.println(new Date(System.currentTimeMillis())+":"+req);
			String response = new Date(System.currentTimeMillis()) + "recepted!";
			doWrite(response);
		}catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void failed(Throwable exc, ByteBuffer attachment) {
		try{
			this.channel.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void doWrite(String response){
		
		if(response!=null && response.length()>0){
			
			byte[] bytes = response.getBytes();
			ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
			writeBuffer.put(bytes);
			writeBuffer.flip();
			channel.write(writeBuffer,writeBuffer,new CompletionHandler<Integer, ByteBuffer>() {

				@Override
				public void completed(Integer result, ByteBuffer attachment) {
					if(attachment.hasRemaining()){
						channel.write(attachment,attachment,this);
					}
					
				}

				@Override
				public void failed(Throwable exc, ByteBuffer attachment) {
					try{
						channel.close();
					}catch (IOException e) {
						e.printStackTrace();
					}
					
				}
				
				
			});
			
			
		}
		
	}

	
}
