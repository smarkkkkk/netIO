package bio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.Socket;


public class ClientSocket{

	public static void main(String[] args){
		
		String host = "127.0.0.1";
		int port = 9034;
		Socket socket = null;
		BufferedReader in = null;
		PrintWriter out = null;
		
		try{
			socket = new Socket(host, port);
			
			in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"utf-8"));
			out = new PrintWriter(socket.getOutputStream(),true);
			out.println("Client request order!");
			
			
			String response = in.readLine();
			System.out.println(response);
		}catch (IOException e) {
			e.printStackTrace();
		}finally {
			if(in!=null){
				try{
					in.close();
				}catch (IOException e) {
					e.printStackTrace();
				}
				in = null;
			}
			
			if(out!=null){
				out.close();
				out=null;
			}
			
			if(socket!=null){
				try{
					socket.close();
				}catch (IOException e) {
					e.printStackTrace();
				}
				socket = null;
			}

		}
	}
	
	
	

	

}
