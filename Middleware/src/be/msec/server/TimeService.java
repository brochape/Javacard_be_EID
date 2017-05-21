package be.msec.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class TimeService {
	public final static byte REVALIDATION_REQUEST = 1;

	public static void main(String[] args) throws Exception {
		// TODO Reconstruct key
        ServerSocket welcomeSocket = new ServerSocket(8000);
		KeyStore keyStore = KeyStore.getInstance("JKS");
		String fileName = "/Users/Silke/Documents/workspaces/neon/project.jks";
		FileInputStream fis = new FileInputStream(fileName);
		keyStore.load(fis, "ThisIs4V3ryS4f3Pa$$w0rd".toCharArray());
		fis.close();

		PrivateKey timestampPrivateKey = (PrivateKey) keyStore.getKey("timestamp", "test".toCharArray());

        while(true) {
        	System.out.println("Waiting");
            Socket connectionSocket = welcomeSocket.accept();
        	System.out.println("Accept");
        	DataInputStream inFromClient = new DataInputStream(connectionSocket.getInputStream());
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            byte clientRequest = inFromClient.readByte();
            System.out.println(clientRequest);
            switch (clientRequest) {
				case REVALIDATION_REQUEST:
	            	System.out.println("Answering to client");
	            	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
	            	LocalDateTime now = LocalDateTime.now();
					String outputTime = dtf.format(now);
					String[] time = outputTime.split("-");
					int[] intTime = new int[time.length];
					for (int i = 0; i < intTime.length; i++) {
						intTime[i] = Integer.parseInt(time[i]);
					}
					byte[] yearBytes = ByteBuffer.allocate(4).putInt(intTime[0]).array();
					byte[] timeBytes = new byte[8];
					System.arraycopy(yearBytes, 0, timeBytes, 0, 4);
					for (int i = 4; i < timeBytes.length; i++) {
						timeBytes[i] = (byte) intTime[i-4];
					}

					MessageDigest md = MessageDigest.getInstance("SHA-256");

					byte[] hashedTime = md.digest(timeBytes);

					Signature signEngine = Signature.getInstance("SHA256withRSA");
					signEngine.initSign(timestampPrivateKey);
					signEngine.update(hashedTime);

					byte[] signature = signEngine.sign();
					int length = signature.length + timeBytes.length + 4;
					byte[] lenBytes = ByteBuffer.allocate(4).putInt(length).array();
					byte[] output = new byte[length];
					System.out.println(length);
					System.arraycopy(lenBytes, 0, output, 0, lenBytes.length);
					System.arraycopy(timeBytes, 0, output, lenBytes.length, timeBytes.length);
					System.arraycopy(signature, 0, output, timeBytes.length + lenBytes.length, signature.length);
					
		            outToClient.write(output);
					
					break;
	
				default:
					break;
			}
            if (clientRequest == REVALIDATION_REQUEST) {
			}
        }
	}

}
