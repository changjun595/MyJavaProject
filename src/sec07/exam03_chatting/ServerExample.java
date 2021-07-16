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
//	스레드 풀 생성
	ExecutorService executorService;
//	클라이언트의 연결 요청 수락하기 위한 서버 소켓 생성
	ServerSocket serverSocket;
//	동기화된 List 컬렉션을 이용하여 스레드에 안전한 Vector 컬렉션 사용
	List<Client> connections = new Vector<Client>();

//	서버 시작 하는 기능
	synchronized void startServer() {
//		현재 사용하고 있는 PC의 코어 수 만큼 스레드 풀에 작성 스레드 생성
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		try {
//			서버 소켓의 객체를 생성
			serverSocket = new ServerSocket();
//			클라이언트에서 요청 시 넘어온 소켓과 연결하기 위한 기능
			serverSocket.bind(new InetSocketAddress("localhost", 5001));
		} catch (Exception e) {
			if (!serverSocket.isClosed()) {
				stopServer();
			}
			return;
		}

//		작업 생성
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				System.out.println("[서버 시작]");
				int MemberNo = 1;
				while (true) {
					Socket socket;
					try {
//						accept()는 클라이언트의 연결을 수락해주는 기능(블로킹)
						socket = serverSocket.accept();
//						getRemoteSocketAddress()는 클라이언트의 정보를 가져온다.
						String message = "[연결 수락: " + socket.getRemoteSocketAddress() + ": "
								+ Thread.currentThread().getName() + "]";
						System.out.println(message);
//						클라이언트의 ID 생성
						String id = "Client(" + MemberNo + ")";
//						클라이언트의 객체를 생성하는 기능
						Client client = new Client(socket, id);
						MemberNo++;
//						List 컬렉션에 클라이언트 객체 저장
						connections.add(client);
						System.out.println("[연결 개수: " + connections.size() + "]");
					} catch (Exception e) {
//						서버 소켓이 열려 있다면 서버 종료 기능
						if (!serverSocket.isClosed()) {
							stopServer();
						}
						break;
					}
				}
			}
		};
//		작업 큐에 작업을 저장
		executorService.submit(runnable);
	}

//	서버 정지 하는 기능
	void stopServer() {
		try {
//			List 컬렉션(Vector - 동기화) 안에 있는 Client 객체를 가져온다.
			Iterator<Client> iterator = connections.iterator();
			while (iterator.hasNext()) {
				Client client = iterator.next();
//				클라이언트 소켓을 닫아 주는 기능
				client.socket.close();
//				List 컬렉션에서 객체를 삭제
				iterator.remove();
			}
//			서버 소켓이 열려 있고 서버 소켓이 닫혀 있다면
			if (serverSocket != null && serverSocket.isClosed()) {
//				서버를 종료
				serverSocket.close();
			}
//			스레드 풀이 제거되지 않고 종료되지 않았다면
			if (executorService != null && !executorService.isShutdown()) {
				executorService.shutdown();
			}
			System.out.println("[서버 멈춤]");
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

//		서버에서 클라이언트에게 데이터를 보내는 기능
//		데이터를 보낸 클라이언트를 제외한 모든 클라이언트에게 데이터를 전송하는 기능
		synchronized void send(String data, String id) {
//			작성 생성
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
//						데이터를 보낸 클라이언트를 확인하기 위한 기능
						if (!Client.this.id.equals(id)) {
							String fullData = id + ": " + data;
							byte[] byteArr = fullData.getBytes("UTF-8");
							OutputStream outputStream = socket.getOutputStream();
							outputStream.write(byteArr);
							outputStream.flush();
						}
					} catch (Exception e) {
						try {
							String message = "[클라이언트 통신 안됨: " + socket.getRemoteSocketAddress() + ": "
									+ Thread.currentThread().getName() + "]";
							System.out.println(message);
//							List 컬렉션에 있는 객체 중에 현재 통신 안되는 클라이언트의 객체를 삭제
							connections.remove(Client.this);
							socket.close();
						} catch (Exception e1) {
						}
					}
				}
			};
//			작업 큐에 작업을 넣는 기능
			executorService.submit(runnable);
		}

//		클라이언트가 보낸 데이터를 받기 위한 기능
		synchronized void receive() {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						while(true) {
							byte[] byteArr = new byte[100];
//							클라이언트가 보낸 데이터를 받기 위한 스트림
							InputStream inputStream = socket.getInputStream();
							int readByteCount = inputStream.read(byteArr);
//							클라이언트가 정상적으로 종료 했을 경우
							if(readByteCount == -1) {
								throw new IOException();
							}
							
//							클라이언트가 보낸 데이터를 정상적으로 받았을 경우
							String message = "[요청 처리]: " + socket.getRemoteSocketAddress()
							+ ": " + Thread.currentThread().getName() + "]";
							System.out.println(message);
							String data = new String(byteArr, 0, readByteCount, "UTF-8");
							
//							모든 데이터를 클라이언트에게 전송
							for(Client client : connections) {
								client.send(data, id);
							}
						}
//					클라이언트가 비정상 종료 했을 경우 및 정상적으로 종료 했을 경우 예외 발생
					}catch (Exception e) {
						try {
							connections.remove(Client.this);
							String message = "[클라이언트 통신 안됨: " + socket.getRemoteSocketAddress()
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
