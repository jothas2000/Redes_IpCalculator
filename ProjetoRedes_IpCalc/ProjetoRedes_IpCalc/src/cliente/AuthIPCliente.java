package cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class AuthIPCliente {
    public static void main(String[] args) {
        String serverAddress = "127.0.0.1";
        int port = 12345;

        try (Socket socket = new Socket(serverAddress, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println(in.readLine()); // Login
            out.println(userInput.readLine());

            System.out.println(in.readLine()); // Senha
            out.println(userInput.readLine());

            String authResponse = in.readLine();
            System.out.println(authResponse);
            if (authResponse.equals("Autenticação falhou!")) return;

            System.out.println(in.readLine()); // Menu
            out.println(userInput.readLine());

            System.out.println(in.readLine()); // CIDR Prompt
            out.println(userInput.readLine());

            String response;
            while ((response = in.readLine()) != null) {
                System.out.println(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
