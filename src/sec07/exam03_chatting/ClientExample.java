package sec07.exam03_chatting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class ClientExample {
	Socket socket;

//	클라이언트 시작 기능
	void startClient() {
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					socket = new Socket();
//					서버에게 연결 요청
					socket.connect(new InetSocketAddress("localhost", 5001));
					String message = "[연결 완료: " + socket.getRemoteSocketAddress() + "]";
					System.out.println(message);
//				만약에 서버가 실행중이지 않을 경우	
				} catch (Exception e) {
					System.out.println("[서버 통신 안됨]");
					if (!socket.isClosed()) {
						stopClient();
					}
					return;
				}
//				정상적으로 서버와 연결이 되었다면 데이터 받는 기능 호출
				receive();
			}
		};
		thread.start();
		inputSendMessage();
	}

//	클라이언트 접속 끊는 기능
	void stopClient() {
		System.out.println("[연결 끊음]");
//		소켓 객체가 있고 소켓이 열려 있다면
		if (socket != null && !socket.isClosed()) {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

//	서버가 보낸 데이터를 읽는 기능
	void receive() {
		while(true) {
			try {
				byte[] byteArr = new byte[100];
				InputStream inputStream = socket.getInputStream();
				int readByteCount = inputStream.read(byteArr);
				
//				서버가 정상적으로 종료 했을 경우
				if(readByteCount == -1) {
					throw new IOException();
				}
				
//				정상적으로 데이터를 받을 경우
				String data = new String(byteArr, 0, readByteCount, "UTF-8");
				System.out.println(data);
//				서버가 비정상적으로 종료 했을 경우
			} catch (Exception e) {
				System.out.println("[서버 통신 안됨]");
				stopClient();
				break;
			}
		}
	}

//	서버에게 데이터를 보내는 기능
	void send(String data) {
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					byte[] byteArr = data.getBytes("UTF-8");
					OutputStream outputStream = socket.getOutputStream();
					outputStream.write(byteArr);
					outputStream.flush();
				} catch (Exception e) {
					System.out.println("[서버 통신 안됨]");
					stopClient();
				}
			}
		};
		thread.start();
	}

	void inputSendMessage() {
		Scanner scanner = new Scanner(System.in);
		while(true) {
			String message = scanner.nextLine();
			if(message.equals("그만")) {
				stopClient();
				break;
			}
			send(message);
		}
		scanner.close();
	}

	public static void main(String[] args) {
		ClientExample client = new ClientExample();
		client.startClient();
	}
}
