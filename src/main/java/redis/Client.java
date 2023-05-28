package redis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws UnknownHostException, IOException {
        boolean isConnected = true;
        int PORT = 6397;//Numéro du port 
        String address = "127.0.0.1";// address Ip =localhost
        do {
            Socket client = new Socket(address, PORT);
            Scanner scan = new Scanner(System.in);
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter writer = new PrintWriter(client.getOutputStream(), true);

            System.out.print(address +":" + PORT + "> ");


            String input = scan.nextLine();
            writer.println(input);

            //partie du pipeline
            if(input.equalsIgnoreCase("MULTI")) {
                System.out.println("OK");
                while(true) {
                    System.out.print(address +":"+ PORT + "(TX)> ");
                    String data = scan.nextLine();
                    writer.println(data);
                    if(!data.equalsIgnoreCase("EXEC")) {
                        System.out.println("QUEUED");
                    }
                    if(data.equalsIgnoreCase("EXEC")) {
                        int siz=Integer.parseInt(reader.readLine());
                        for (int i =0 ; i<siz;i++) {
                            System.out.println(i+1+")"+reader.readLine());
                        }

                        System.out.print(address +":" + PORT + "> ");
                        input = scan.nextLine();
                        writer.println(input);
                        //System.out.println(reader.readLine());
                        break;
                    }
                }
            }

            System.out.println(reader.readLine());


            if (input.equals("exit")) {
                isConnected = false;
            }

            client.close();
        } while (isConnected);

    }

}