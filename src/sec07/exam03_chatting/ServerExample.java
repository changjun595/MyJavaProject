package sec07.exam03_chatting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerExample {
//	������ Ǯ ����
	ExecutorService executorService;
//	Ŭ���̾�Ʈ�� ���� ��û �����ϱ� ���� ���� ���� ����
	ServerSocket serverSocket;
//	����ȭ�� List �÷����� �̿��Ͽ� �����忡 ������ Vector �÷��� ���
	List<Client> connections = new Vector<Client>();

//	���� ���� �ϴ� ���
	synchronized void startServer() {
//		���� ����ϰ� �ִ� PC�� �ھ� �� ��ŭ ������ Ǯ�� �ۼ� ������ ����
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		try {
//			���� ������ ��ü�� ����
			serverSocket = new ServerSocket();
//			Ŭ���̾�Ʈ���� ��û �� �Ѿ�� ���ϰ� �����ϱ� ���� ���
			serverSocket.bind(new InetSocketAddress("localhost", 5001));
		} catch (Exception e) {
			if (!serverSocket.isClosed()) {
				stopServer();
			}
			return;
		}

//		�۾� ����
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				System.out.println("[���� ����]");
				int MemberNo = 1;
				while (true) {
					Socket socket;
					try {
//						accept()�� Ŭ���̾�Ʈ�� ������ �������ִ� ���(���ŷ)
						socket = serverSocket.accept();
//						getRemoteSocketAddress()�� Ŭ���̾�Ʈ�� ������ �����´�.
						String message = "[���� ����: " + socket.getRemoteSocketAddress() + ": "
								+ Thread.currentThread().getName() + "]";
						System.out.println(message);
//						Ŭ���̾�Ʈ�� ID ����
						String id = "Client(" + MemberNo + ")";
//						Ŭ���̾�Ʈ�� ��ü�� �����ϴ� ���
						Client client = new Client(socket, id);
						MemberNo++;
//						List �÷��ǿ� Ŭ���̾�Ʈ ��ü ����
						connections.add(client);
						System.out.println("[���� ����: " + connections.size() + "]");
					} catch (Exception e) {
//						���� ������ ���� �ִٸ� ���� ���� ���
						if (!serverSocket.isClosed()) {
							stopServer();
						}
						break;
					}
				}
			}
		};
//		�۾� ť�� �۾��� ����
		executorService.submit(runnable);
	}

//	���� ���� �ϴ� ���
	void stopServer() {
		try {
//			List �÷���(Vector - ����ȭ) �ȿ� �ִ� Client ��ü�� �����´�.
			Iterator<Client> iterator = connections.iterator();
			while (iterator.hasNext()) {
				Client client = iterator.next();
//				Ŭ���̾�Ʈ ������ �ݾ� �ִ� ���
				client.socket.close();
//				List �÷��ǿ��� ��ü�� ����
				iterator.remove();
			}
//			���� ������ ���� �ְ� ���� ������ ���� �ִٸ�
			if (serverSocket != null && serverSocket.isClosed()) {
//				������ ����
				serverSocket.close();
			}
//			������ Ǯ�� ���ŵ��� �ʰ� ������� �ʾҴٸ�
			if (executorService != null && !executorService.isShutdown()) {
				executorService.shutdown();
			}
			System.out.println("[���� ����]");
		} catch (Exception e) {
		}
	}

	class Client {
		Socket socket;
		String id;

		public Client(Socket socket, String id) {
			this.socket = socket;
			this.id = id;
			receive();
		}

//		�������� Ŭ���̾�Ʈ���� �����͸� ������ ���
//		�����͸� ���� Ŭ���̾�Ʈ�� ������ ��� Ŭ���̾�Ʈ���� �����͸� �����ϴ� ���
		synchronized void send(String data, String id) {
//			�ۼ� ����
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
//						�����͸� ���� Ŭ���̾�Ʈ�� Ȯ���ϱ� ���� ���
						if (!Client.this.id.equals(id)) {
							String fullData = id + ": " + data;
							byte[] byteArr = fullData.getBytes("UTF-8");
							OutputStream outputStream = socket.getOutputStream();
							outputStream.write(byteArr);
							outputStream.flush();
						}
					} catch (Exception e) {
						try {
							String message = "[Ŭ���̾�Ʈ ��� �ȵ�: " + socket.getRemoteSocketAddress() + ": "
									+ Thread.currentThread().getName() + "]";
							System.out.println(message);
//							List �÷��ǿ� �ִ� ��ü �߿� ���� ��� �ȵǴ� Ŭ���̾�Ʈ�� ��ü�� ����
							connections.remove(Client.this);
							socket.close();
						} catch (Exception e1) {
						}
					}
				}
			};
//			�۾� ť�� �۾��� �ִ� ���
			executorService.submit(runnable);
		}

//		Ŭ���̾�Ʈ�� ���� �����͸� �ޱ� ���� ���
		synchronized void receive() {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						while(true) {
							byte[] byteArr = new byte[100];
//							Ŭ���̾�Ʈ�� ���� �����͸� �ޱ� ���� ��Ʈ��
							InputStream inputStream = socket.getInputStream();
							int readByteCount = inputStream.read(byteArr);
//							Ŭ���̾�Ʈ�� ���������� ���� ���� ���
							if(readByteCount == -1) {
								throw new IOException();
							}
							
//							Ŭ���̾�Ʈ�� ���� �����͸� ���������� �޾��� ���
							String message = "[��û ó��]: " + socket.getRemoteSocketAddress()
							+ ": " + Thread.currentThread().getName() + "]";
							System.out.println(message);
							String data = new String(byteArr, 0, readByteCount, "UTF-8");
							
//							��� �����͸� Ŭ���̾�Ʈ���� ����
							for(Client client : connections) {
								client.send(data, id);
							}
						}
//					Ŭ���̾�Ʈ�� ������ ���� ���� ��� �� ���������� ���� ���� ��� ���� �߻�
					}catch (Exception e) {
						try {
							connections.remove(Client.this);
							String message = "[Ŭ���̾�Ʈ ��� �ȵ�: " + socket.getRemoteSocketAddress()
							+ ": " + Thread.currentThread().getName() + "]";
							System.out.println(message);
							socket.close();
						} catch (Exception e1) {
						}
					}
				}
			};
			executorService.submit(runnable);
		}
	}
	
	public static void main(String[] args) {
		ServerExample server = new ServerExample();
		server.startServer();
	}
}
