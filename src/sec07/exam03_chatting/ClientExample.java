package sec07.exam03_chatting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class ClientExample {
	Socket socket;

//	Ŭ���̾�Ʈ ���� ���
	void startClient() {
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					socket = new Socket();
//					�������� ���� ��û
					socket.connect(new InetSocketAddress("localhost", 5001));
					String message = "[���� �Ϸ�: " + socket.getRemoteSocketAddress() + "]";
					System.out.println(message);
//				���࿡ ������ ���������� ���� ���	
				} catch (Exception e) {
					System.out.println("[���� ��� �ȵ�]");
					if (!socket.isClosed()) {
						stopClient();
					}
					return;
				}
//				���������� ������ ������ �Ǿ��ٸ� ������ �޴� ��� ȣ��
				receive();
			}
		};
		thread.start();
		inputSendMessage();
	}

//	Ŭ���̾�Ʈ ���� ���� ���
	void stopClient() {
		System.out.println("[���� ����]");
//		���� ��ü�� �ְ� ������ ���� �ִٸ�
		if (socket != null && !socket.isClosed()) {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

//	������ ���� �����͸� �д� ���
	void receive() {
		while(true) {
			try {
				byte[] byteArr = new byte[100];
				InputStream inputStream = socket.getInputStream();
				int readByteCount = inputStream.read(byteArr);
				
//				������ ���������� ���� ���� ���
				if(readByteCount == -1) {
					throw new IOException();
				}
				
//				���������� �����͸� ���� ���
				String data = new String(byteArr, 0, readByteCount, "UTF-8");
				System.out.println(data);
//				������ ������������ ���� ���� ���
			} catch (Exception e) {
				System.out.println("[���� ��� �ȵ�]");
				stopClient();
				break;
			}
		}
	}

//	�������� �����͸� ������ ���
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
					System.out.println("[���� ��� �ȵ�]");
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
			if(message.equals("�׸�")) {
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
