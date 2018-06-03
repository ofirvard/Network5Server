import java.io.*;
import java.net.*;

public class Main
{
	static int users = 0;

	public static void main(String[] args)
	{
		String msg_received = "5david-ofir-ytzhak-ana-anna-";
		System.out.println(msg_received);
		System.out.println(msg_received.substring(0, msg_received.length() - 1));
		//		getIPv4();
		//		new Server2().startServer();
		//		getMsg();
	}

	public static void getIPv4()
	{
		try
		{
			InetAddress address = InetAddress.getLocalHost();
			System.out.println(address.getHostName());
			System.out.println("IPv4: " + address.getHostAddress());
			System.out.println("Hex:  " + parseAddressToHex(address.getHostAddress()));
		}
		catch (UnknownHostException e)
		{
		}
	}

	private static String parseAddressToHex(String address)
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

	public static void getMsg()
	{
		try
		{
			String msg_received;

			ServerSocket serverSocket = new ServerSocket(9153);
			Socket clientSocket = serverSocket.accept();       //This is blocking. It will wait.
			DataInputStream DIS = new DataInputStream(clientSocket.getInputStream());

			msg_received = DIS.readUTF();
			System.out.println(msg_received);
			DataOutputStream DOS = new DataOutputStream(clientSocket.getOutputStream());
			DOS.writeUTF("0" + msg_received.substring(1) + "#" + users++);

			DOS.writeUTF("5david-ofir-ytzhak-ana-anna");

			DOS.writeUTF("3");
			clientSocket.close();
			serverSocket.close();
		}
		catch (Exception e)
		{
		}
	}
}
