import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ofir on 03-Jun-18.
 */
public class server
{
	private JButton connect;
	private JTextPane textPane1;
	private JPanel mainPanel;
	private JButton disconnect;
	private JLabel ip_label;

	private static final int CONNECT = 0;
	private static final int NORMAL_MESSAGE = 1;
	private static final int PRIVATE_MESSAGE = 2;
	private static final int DISCONNECT = 3;
	private static final int STILL_ALIVE = 4;
	private static final int USER_LIST = 5;
	private static final int ERROR = 6;
	private Document doc;
	private int userCount = 0;
	private ArrayList<ClientTask> users = new ArrayList<>();
	private ServerSocket serverSocket;

	public server()
	{
		connect.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				connect();
			}
		});
		disconnect.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				disconnectAll();
			}
		});
	}

	public static void main(String[] args)
	{
		JFrame frame = new JFrame("App");
		frame.setContentPane(new server().mainPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private void connect()
	{
		doc = textPane1.getDocument();
		getIPv4();
		disconnect.setEnabled(true);
	}

	private void disconnectAll()
	{
		try
		{
			connect.setEnabled(false);
			doc.insertString(0, "disconnect", null);
			for (ClientTask client : users)
			{
				client.sendMessage(DISCONNECT + "");
			}
			serverSocket.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void getIPv4()
	{
		try
		{
			InetAddress address = InetAddress.getLocalHost();
			doc.insertString(0, "IPv4: " + address.getHostAddress() + "\nHex: " + parseAddressToHex(address.getHostAddress()), null);
			ip_label.setText("The Hex IPv4 is: " + parseAddressToHex(address.getHostAddress()));
			startServer();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private String parseAddressToHex(String address)
	{

		int result = 0;
		String[] str = address.split("\\.");
		for (int i = 0; i < str.length; i++)
		{
			int j = Integer.parseInt(str[i]);
			result = result << 8 | (j & 0xFF);
		}
		return Integer.toHexString(result);
	}

	private void startServer()
	{
		final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);

		Runnable serverTask = new Runnable()
		{
			@Override public void run()
			{
				try
				{
					serverSocket = new ServerSocket(9153);
					doc.insertString(doc.getLength(), "\nWaiting for clients to connect...", null);
					while (true)
					{
						Socket clientSocket = serverSocket.accept();
						clientProcessingPool.submit(new ClientTask(clientSocket));
					}
				}
				catch (Exception e)
				{
					System.err.println("Unable to process client request");
					e.printStackTrace();
				}
			}
		};
		Thread serverThread = new Thread(serverTask);
		serverThread.start();

	}

	private class ClientTask implements Runnable
	{
		private final Socket clientSocket;
		public String name;
		boolean keepListening;

		private ClientTask(Socket clientSocket)
		{
			this.clientSocket = clientSocket;
		}

		private void connect()
		{
			users.add(this);
			updateClientsUserList();
		}

		private void disconnect()
		{
			users.remove(this);
			keepListening = false;
			try
			{
				clientSocket.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			updateClientsUserList();
		}

		private void sendMessage(String msg)
		{
			try
			{
				DataOutputStream DOS = new DataOutputStream(clientSocket.getOutputStream());
				DOS.writeUTF(msg);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		private void updateClientsUserList()
		{
			String userList = USER_LIST + "";
			for (ClientTask client : users)
			{
				userList += client.name + "-";
			}
			userList = userList.substring(0, userList.length() - 1);
			for (ClientTask client : users)
				client.sendMessage(userList);
		}

		@Override public void run()
		{
			keepListening = true;
			String msg_received;
			try
			{
				doc.insertString(doc.getLength(), "\nGot a client !", null);

				while (keepListening)
				{
					DataInputStream DIS = new DataInputStream(clientSocket.getInputStream());
					msg_received = DIS.readUTF();
					doc.insertString(doc.getLength(), "\nin: " + msg_received, null);
					DataOutputStream DOS = new DataOutputStream(clientSocket.getOutputStream());

					switch (Character.getNumericValue(msg_received.charAt(0)))
					{
					case CONNECT:
						this.name = msg_received.substring(1) + "#" + userCount++;
						DOS.writeUTF(CONNECT + this.name);
						doc.insertString(doc.getLength(), "\nout: " + "0" + this.name, null);
						connect();
						break;

					case NORMAL_MESSAGE:
						for (ClientTask client : users)
							client.sendMessage(msg_received);
						break;

					case PRIVATE_MESSAGE:
						int firstDash = msg_received.indexOf("-");
						int secondDash = msg_received.indexOf("-", firstDash + 1);
						String recipient = msg_received.substring(firstDash + 1, secondDash);
						boolean found = false;
						for (ClientTask client : users)
							if (client.name.equals(recipient))
							{
								found = true;
								client.sendMessage(msg_received);
								break;
							}
						if (!found)
						{
							DOS.writeUTF(ERROR + recipient + " doesn't exist");
						}
						break;

					case DISCONNECT:
						disconnect();
						break;

					case STILL_ALIVE:
						break;

					case USER_LIST:
						updateClientsUserList();
						break;

					case ERROR:
						try
						{
							StyleContext context = new StyleContext();
							// build a style
							Style style = context.addStyle("test", null);
							// set some style properties
							StyleConstants.setForeground(style, Color.RED);

							doc.insertString(doc.getLength(), msg_received.substring(1) + "\n", style);
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
						break;

					default:
						doc.insertString(doc.getLength(), "\nMessage not up to code: " + msg_received, null);
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
