package servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class AuthIPServer {
    private static final String USERNAME = "admin"; // Login do servidor
    private static final String PASSWORD = "password"; // Senha do servidor

    public static void main(String[] args) {
        int port = 12345;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor iniciado na porta " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress().getHostAddress());

                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Autenticação
            out.println("Digite o login:");
            String login = in.readLine();
            out.println("Digite a senha:");
            String password = in.readLine();

            if (!USERNAME.equals(login) || !PASSWORD.equals(password)) {
                out.println("Autenticação falhou!");
                clientSocket.close();
                return;
            }
            out.println("Autenticação bem-sucedida!");

            // Menu de seleção
            out.println("Escolha o tipo de IP (1 - IPv4, 2 - IPv6):");
            String choice = in.readLine();
            if (!choice.equals("1") && !choice.equals("2")) {
                out.println("Escolha inválida. Conexão encerrada.");
                clientSocket.close();
                return;
            }

            out.println("Digite o endereço no formato CIDR (exemplo: 192.168.1.10/24 ou 2001:db8:baba::11/48):");
            String cidr = in.readLine();

            // Realiza o cálculo baseado na escolha
            String result = choice.equals("1") ? calculateIPv4(cidr) : calculateIPv6(cidr);

            out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String calculateIPv4(String cidr) {
        try {
            String[] parts = cidr.split("/");
            String ip = parts[0];
            int prefix = Integer.parseInt(parts[1]);

            InetAddress inet = InetAddress.getByName(ip);
            byte[] ipBytes = inet.getAddress();

            int mask = 0xffffffff << (32 - prefix);
            byte[] maskBytes = new byte[4];
            for (int i = 0; i < 4; i++) {
                maskBytes[i] = (byte) (mask >>> (24 - i * 8));
            }

            byte[] networkBytes = new byte[4];
            for (int i = 0; i < 4; i++) {
                networkBytes[i] = (byte) (ipBytes[i] & maskBytes[i]);
            }

            InetAddress networkAddress = InetAddress.getByAddress(networkBytes);

            byte[] broadcastBytes = new byte[4];
            for (int i = 0; i < 4; i++) {
                broadcastBytes[i] = (byte) (networkBytes[i] | ~maskBytes[i]);
            }

            InetAddress broadcastAddress = InetAddress.getByAddress(broadcastBytes);

            int totalHosts = (int) Math.pow(2, 32 - prefix);
            int usableHosts = totalHosts - 2;

            byte[] firstUsableBytes = networkBytes.clone();
            firstUsableBytes[3] += 1;
            InetAddress firstUsableAddress = InetAddress.getByAddress(firstUsableBytes);

            byte[] lastUsableBytes = broadcastBytes.clone();
            lastUsableBytes[3] -= 1;
            InetAddress lastUsableAddress = InetAddress.getByAddress(lastUsableBytes);

            return "IPv4 CIDR: " + cidr +
                   "\nEndereço de Rede: " + networkAddress.getHostAddress() +
                   "\nEndereço de Broadcast: " + broadcastAddress.getHostAddress() +
                   "\nQuantidade de Endereços Úteis: " + usableHosts +
                   "\nEndereço Útil Inicial: " + firstUsableAddress.getHostAddress() +
                   "\nEndereço Útil Final: " + lastUsableAddress.getHostAddress();
        } catch (Exception e) {
            return "Erro no cálculo IPv4: " + e.getMessage();
        }
    }

    private static String calculateIPv6(String cidr) {
        try {
            String[] parts = cidr.split("/");
            String ip = parts[0];
            int prefix = Integer.parseInt(parts[1]);

            InetAddress inet = InetAddress.getByName(ip);
            BigInteger ipValue = new BigInteger(1, inet.getAddress());
            BigInteger prefixMask = BigInteger.ONE.shiftLeft(128 - prefix).subtract(BigInteger.ONE).not();

            BigInteger networkAddress = ipValue.and(prefixMask);
            BigInteger broadcastAddress = networkAddress.add(BigInteger.ONE.shiftLeft(128 - prefix).subtract(BigInteger.ONE));

            BigInteger usableHosts = BigInteger.ONE.shiftLeft(128 - prefix).subtract(BigInteger.TWO);
            BigInteger firstUsableAddress = networkAddress.add(BigInteger.ONE);
            BigInteger lastUsableAddress = broadcastAddress.subtract(BigInteger.ONE);

            return "IPv6 CIDR: " + cidr +
                   "\nQuantidade de Endereços Úteis: " + usableHosts +
                   "\nEndereço Útil Inicial: " + formatIPv6(firstUsableAddress) +
                   "\nEndereço Útil Final: " + formatIPv6(lastUsableAddress);
        } catch (Exception e) {
            return "Erro no cálculo IPv6: " + e.getMessage();
        }
    }

    private static String formatIPv6(BigInteger address) {
        try {
            byte[] addressBytes = address.toByteArray();
            if (addressBytes.length > 16) {
                addressBytes = java.util.Arrays.copyOfRange(addressBytes, addressBytes.length - 16, addressBytes.length);
            }
            return InetAddress.getByAddress(addressBytes).getHostAddress();
        } catch (Exception e) {
            return "Erro ao formatar IPv6: " + e.getMessage();
        }
    }
}
