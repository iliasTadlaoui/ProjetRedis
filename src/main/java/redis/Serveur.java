package redis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Serveur {
    private static Map<String, String> dataRecue = new HashMap<>();
    private static Map<String, String> datafinal = new HashMap<>();
    public static void main(String[] args) throws IOException {
        // Création de la socket pour écouter les connexions
        ServerSocket server = new ServerSocket(6397);
        System.out.println("Serveur en écoute ...");

        while (true) {
            Socket socket = server.accept();


            Thread clientThread = new Thread(new ClientHandler(socket));
            clientThread.start();
        }

        // Fermeture du serveur (cette partie ne sera jamais atteinte dans ce cas)
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                // Récupérer l'input du client
                String input = reader.readLine();
                String[] parties = input.trim().split("\\s+");
                String commande = lireCommande(parties, dataRecue);
                System.out.println("La commande : " + commande);
                System.out.println("Données reçues : " + dataRecue.toString());
                System.out.println("Données enregistree pour le client  : " + datafinal.toString());

                //list pour enregistrer les commands dans le mode MULTI
                List<String> commands = new ArrayList<>();

                if(commande.equalsIgnoreCase("MULTI")) {
                    String dat;

                    List<String> multiCommands = new ArrayList<>();
                    while(true) {
                        dat=reader.readLine();
                        String[] parties1 = dat.trim().split("\\s+");
                        String commande1= lireCommande(parties1, dataRecue);
                        System.out.println("La commande : " + commande1);
                        System.out.println("Données reçues : " + dataRecue.toString());

                        if(dat.equalsIgnoreCase("EXEC")) {
                            break;
                        }
                        multiCommands.add(dat);
                    }
                    commands.addAll(multiCommands);
                    if(dat.equalsIgnoreCase("EXEC")) {
                        System.out.println("sss");
                        writer.println(commands.size());
                        for(String s: commands) {
                            String[] parties2 = s.trim().split("\\s+");
                            String commande2= lireCommande(parties2, dataRecue);
                            traiterPipeline(commande2, parties2, dataRecue, writer, reader);

                        }

                        input = reader.readLine();
                        parties = input.trim().split("\\s+");
                        commande = lireCommande(parties, dataRecue);
                        System.out.println("La commande : " + commande);
                        System.out.println("Données reçues : " + dataRecue.toString());
                    }

                }

                traiterCommande(commande, parties, dataRecue, writer,reader);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        private static String lireCommande(String[] parties, Map<String, String> data) {
            String commande = null;

            if (parties.length >= 1) {
                commande = parties[0].toUpperCase();

                if (parties.length >= 2) {
                    String clef = parties[1];

                    if (parties.length >= 3) {
                        String valeur = parties[2];
                        data.put(clef, valeur);
                    }
                }
            }

            return commande;
        }



        // Méthode de traitement des différentes commandes
        private static void traiterCommande(String commande, String[] parties, Map<String, String> data, PrintWriter writer,BufferedReader reader) {
            boolean TX=false;
            switch (commande) {
                case "SET":
                    String clefSet = null;
                    String valeurSet = null;

                    if (parties.length >= 2) {
                        clefSet = parties[1];

                        if (parties.length >= 3) {
                            valeurSet = parties[2];
                        }
                    }
                    if(clefSet==null||valeurSet==null) {
                        writer.println("missing parameters");
                    }else {
                        datafinal.put(clefSet, valeurSet);
                        writer.println("OK");
                    }

                    break;

                case "STRLEN":
                    String clefStrlen = null;
                    String valeurStrlen = null;

                    if (parties.length >= 2) {
                        clefStrlen = parties[1];

                        if (parties.length >= 3) {
                            valeurStrlen = parties[2];
                        }
                    }

                    String value = datafinal.get(clefStrlen);
                    if (value != null) {
                        writer.println(value.length());
                    } else {
                        writer.println(0);
                    }
                    break;

                case "DEL":
                    String clefDel = null;
                    String valeurDel = null;

                    if (parties.length >= 2) {
                        clefDel = parties[1];

                        if (parties.length >= 3) {
                            valeurDel = parties[2];
                        }
                    }

                    boolean isRemoved = datafinal.remove(clefDel) != null;
                    writer.println(isRemoved);
                    break;

                case "GET":
                    String clefGet = null;
                    String valeurGet = null;

                    if (parties.length >= 2) {
                        clefGet = parties[1];
                    }

                    // Envoi de la réponse au client
                    String valueGet = datafinal.get(clefGet);
                    if (valueGet != null) {
                        writer.println(valueGet);
                    } else {
                        writer.println("(nil)");
                    }
                    // Ajout d'une indication de fin de réponse
                    writer.println("END");
                    break;
                case "INCR":
                    String clefIncr = null;
                    String valeurIncr = null;

                    if (parties.length >= 2) {
                        clefIncr = parties[1];
                        valeurIncr = data.get(clefIncr);
                    }

                    if (valeurIncr != null) {
                        try {
                            int nombre = Integer.parseInt(valeurIncr);
                            nombre++;
                            valeurIncr = Integer.toString(nombre);
                            datafinal.put(clefIncr, valeurIncr);
                            writer.println("(integer) " + valeurIncr);
                        } catch (NumberFormatException e) {
                            writer.println("(error) ERR value is not an integer or out of range");
                        }
                    } else {
                        valeurIncr = "0";
                        int nombre = Integer.parseInt(valeurIncr);
                        nombre++;
                        valeurIncr = Integer.toString(nombre);
                        datafinal.put(clefIncr, valeurIncr);
                        writer.println("(integer) " + valeurIncr);
                    }
                    break;

                case "DECR":
                    // Récupération de la clé
                    String clefDecr = null;
                    if (parties.length >= 2) {
                        clefDecr = parties[1];
                    }

                    // Récupération de la valeur correspondant à la clé
                    String valeurDecr = datafinal.get(clefDecr);

                    if (valeurDecr != null) {
                        try {
                            // Conversion de la valeur en entier
                            int nombre = Integer.parseInt(valeurDecr);
                            // Décrémentation de la valeur
                            nombre--;
                            // Mise à jour de la valeur dans le dictionnaire
                            valeurDecr = Integer.toString(nombre);
                            datafinal.put(clefDecr, valeurDecr);
                            // Affichage de la nouvelle valeur
                            writer.println(valeurDecr);
                        } catch (NumberFormatException e) {
                            // Gestion de l'erreur si la valeur n'est pas un entier valide
                            writer.println("(error) ERR value is not an integer or out of range");
                        }
                    } else {
                        // Si la valeur est absente, initialisation à -1 et enregistrement dans le dictionnaire
                        valeurDecr = "-1";
                        datafinal.put(clefDecr, valeurDecr);
                        // Affichage de la valeur initialisée
                        writer.println("(integer) " + valeurDecr);
                    }
                    break;

                case "SETNX":
                    if (parties.length == 3) {
                        String key = parties[1];
                        String valu = parties[2];

                        if (!datafinal.containsKey(key)) {
                            datafinal.put(key, valu);
                            writer.println("(integer) " +1);
                        } else {

                            writer.println("(integer) " +0);
                        }
                    } else {
                        writer.println("Invalid SETNX command");
                    }
                    break;


                case "APPEND":
                    String clefAppend = null;
                    String valeurAppend = null;

                    if (parties.length >= 2) {
                        clefAppend = parties[1];

                        if (parties.length >= 3) {
                            valeurAppend = parties[2];
                        }
                    }

                    String valueAppend = datafinal.get(clefAppend);
                    if (valueAppend != null) {

                        String nouvelleValeur = valueAppend + valeurAppend;
                        datafinal.put(clefAppend, nouvelleValeur);
                        writer.println("(integer) " +1);
                    } else {
                        datafinal.put(clefAppend, valeurAppend);
                        writer.println("(integer) " +0);
                    }
                    break;
                case "EXIT":
                    writer.println("");
                    break;

                default:
                    writer.println("(error) ERR unknown command 'dzdf', with args beginning with:");
            }
        }



        // Méthode de traitement des différentes commandes mais pour le mode MULTI
        private static void traiterPipeline(String commande, String[] parties, Map<String, String> data, PrintWriter writer,BufferedReader reader) {
            switch (commande) {
                case "SET":
                    String clefSet = null;
                    String valeurSet = null;

                    if (parties.length > 2) {
                        clefSet = parties[1];

                        if (parties.length >= 3) {
                            valeurSet = parties[2];
                        }
                    }else {
                        writer.println("(error) ERR wrong number of arguments for 'SET' command");
                        break;
                    }
                    if(clefSet==null||valeurSet==null) {
                        writer.println("(integer) " + 0);
                    }else {
                        datafinal.put(clefSet, valeurSet);
                        writer.println("OK");
                    }


                    break;

                case "STRLEN":
                    String clefStrlen = null;
                    String valeurStrlen = null;

                    if (parties.length >= 2) {
                        clefStrlen = parties[1];

                        if (parties.length >= 3) {
                            valeurStrlen = parties[2];
                        }
                    }else {
                        writer.println("(error) ERR wrong number of arguments for 'STRLEN' command");
                        break;
                    }

                    String value = datafinal.get(clefStrlen);
                    if (value != null) {

                        writer.println("(integer) " +value);

                    } else {

                        writer.println("(integer) " +0);

                    }
                    break;

                case "DEL":
                    String clefDel = null;
                    String valeurDel = null;

                    if (parties.length >= 2) {
                        clefDel = parties[1];

                        if (parties.length >= 3) {
                            valeurDel = parties[2];
                        }
                    } else {
                        writer.println("(error) ERR wrong number of arguments for 'DEL' command");
                        break;
                    }


                    boolean isRemoved = datafinal.remove(clefDel) != null;

                    writer.println("(integer) " +1);
                    break;

                case "GET":
                    String clefGet = null;
                    String valeurGet = null;

                    if (parties.length >= 2) {
                        clefGet = parties[1];
                    }else {
                        writer.println("(error) ERR wrong number of arguments for 'GET' command");
                        break;
                    }

                    // Envoi de la réponse au client
                    String valueGet = datafinal.get(clefGet);
                    if (valueGet != null) {

                        writer.println("" +valueGet);

                    } else {

                        writer.println("" +valueGet);

                    }

                    break;
                case "INCR":
                    String clefIncr = null;
                    String valeurIncr = null;

                    if (parties.length >= 2) {
                        clefIncr = parties[1];
                        valeurIncr = data.get(clefIncr);
                    }else {
                        writer.println("(error) ERR wrong number of arguments for 'INCR' command");
                        break;
                    }

                    if (valeurIncr != null) {
                        try {
                            int nombre = Integer.parseInt(valeurIncr);
                            nombre++;
                            valeurIncr = Integer.toString(nombre);
                            datafinal.put(clefIncr, valeurIncr);
                            writer.println("(integer) " +valeurIncr);

                        } catch (NumberFormatException e) {
                            writer.println("(error) ERR value is not an integer or out of range");
                        }
                    } else {
                        valeurIncr = "0";
                        int nombre = Integer.parseInt(valeurIncr);
                        nombre++;
                        valeurIncr = Integer.toString(nombre);
                        datafinal.put(clefIncr, valeurIncr);

                        writer.println("(integer) " +valeurIncr);
                    }

                    break;

                case "DECR":
                    // Récupération de la clé
                    String clefDecr = null;
                    if (parties.length >= 2) {
                        clefDecr = parties[1];
                    }else {
                        writer.println("(error) ERR wrong number of arguments for 'DECR' command");
                        break;
                    }

                    // Récupération de la valeur correspondant à la clé
                    String valeurDecr = data.get(clefDecr);

                    if (valeurDecr != null) {
                        try {
                            // Conversion de la valeur en entier
                            int nombre = Integer.parseInt(valeurDecr);
                            // Décrémentation de la valeur
                            nombre--;
                            // Mise à jour de la valeur dans le dictionnaire
                            valeurDecr = Integer.toString(nombre);
                            datafinal.put(clefDecr, valeurDecr);
                            // Affichage de la nouvelle valeur
                            // results.add(" DECR ");
                            writer.println("(integer) " +valeurDecr);
                        } catch (NumberFormatException e) {
                            // Gestion de l'erreur si la valeur n'est pas un entier valide
                            writer.println("(error) ERR value is not an integer or out of range");
                        }
                    } else {
                        // Si la valeur est absente, initialisation à -1 et enregistrement dans le dictionnaire
                        valeurDecr = "-1";
                        datafinal.put(clefDecr, valeurDecr);
                        writer.println("(integer) " +valeurDecr);


                    }

                    break;

                case "SETNX":

                    if (parties.length == 3) {
                        String key = parties[1];
                        String valu = parties[2];


                        if (!datafinal.containsKey(key)) {
                            datafinal.put(key, valu);
                            writer.println("OK ");
                        } else {
                            writer.println("(integer) " +0);
                        }
                    } else {
                        writer.println("(error) ERR wrong number of arguments for 'setnx' command");
                    }


                    break;


                case "APPEND":
                    String clefAppend = null;
                    String valeurAppend = null;

                    if (parties.length >= 2) {
                        clefAppend = parties[1];

                        if (parties.length >= 3) {
                            valeurAppend = parties[2];
                        }
                    }

                    String valueAppend = datafinal.get(clefAppend);

                    if(clefAppend!=null||valeurAppend!=null) {
                        if (valueAppend != null) {

                            String nouvelleValeur = valueAppend + valeurAppend;
                            datafinal.put(clefAppend, nouvelleValeur);
                            writer.println("(integer) " +1);
                        } else {
                            datafinal.put(clefAppend, valeurAppend);
                            writer.println("(integer) " +0);
                        }
                    }else {
                        writer.println("(error) ERR wrong number of arguments for 'APPEND' command");
                    }

                    break;
                case "EXEC":
                    writer.println(" ");
                    break;

                default:
                    writer.println("(error) ERR unknown command 'dzdf', with args beginning with:");
            }
        }

    }
}