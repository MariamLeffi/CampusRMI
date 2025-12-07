package campusRMI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServeurCampusRMI {
    // TCP notifications (identique à votre version CORBA)
    private static ServerSocket tcpServerSocket;
    private static ExecutorService clientThreadPool;
    private static final ConcurrentHashMap<String, PrintWriter> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try {
            System.out.println("Démarrage Serveur Campus RMI avec TCP...");

            // 1. Démarrer le registre RMI
            try {
                Registry registry = LocateRegistry.createRegistry(1099);
                System.out.println("Registre RMI démarré sur le port 1099");
            } catch (Exception e) {
                System.out.println("Registre RMI déjà démarré");
            }

            // 2. Créer et exporter l'objet RMI
            GestionCampusImpl service = new GestionCampusImpl();
            System.out.println("Service GestionCampus créé et exporté");

            // 3. Enregistrer dans le registre RMI
            Naming.rebind("rmi://localhost:1099/GestionCampus", service);
            System.out.println("Service enregistré: rmi://localhost:1099/GestionCampus");

            // 4. Démarrer serveur TCP pour notifications
            demarrerServeurTCP();

            System.out.println("SERVEUR CAMPUS RMI PRÊT À FONCTIONNER");
            System.out.println("RMI Registry: localhost:1099");
            System.out.println("TCP Notifications: port 9999");
            System.out.println("Base de données: SQL Server Campus");

            // Envoyer notification de démarrage
            broadcastNotification("SERVER_STARTED:Serveur RMI démarré avec succès");

            // Garder le serveur actif
            System.out.println(" En attente des connexions clients...");
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("ERREUR SERVEUR RMI: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // === MÉTHODES TCP (IDENTIQUES À VOTRE VERSION) ===

    private static void demarrerServeurTCP() {
        try {
            tcpServerSocket = new ServerSocket(9999);
            clientThreadPool = Executors.newCachedThreadPool();
            System.out.println("Serveur TCP démarré sur le port 9999");

            Thread acceptThread = new Thread(() -> {
                try {
                    while (!tcpServerSocket.isClosed()) {
                        Socket clientSocket = tcpServerSocket.accept();
                        String clientId = clientSocket.getInetAddress().getHostAddress() +
                                ":" + clientSocket.getPort();
                        System.out.println("🔗 Client TCP connecté: " + clientId);

                        clientThreadPool.execute(new ClientHandler(clientSocket, clientId));
                    }
                } catch (IOException e) {
                    if (!tcpServerSocket.isClosed()) {
                        System.err.println(" Erreur TCP: " + e.getMessage());
                    }
                }
            });

            acceptThread.setDaemon(true);
            acceptThread.start();

        } catch (IOException e) {
            System.err.println("Impossible de démarrer TCP: " + e.getMessage());
        }
    }

    public static void broadcastNotification(String message) {
        System.out.println("Broadcast TCP: " + message);
        for (PrintWriter out : clients.values()) {
            try {
                out.println(message);
                out.flush();
            } catch (Exception e) {
                System.err.println("Erreur envoi TCP: " + e.getMessage());
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private String clientId;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket, String clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                clients.put(clientId, out);
                out.println("BIENVENUE:Connecté au serveur de notifications Campus RMI");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("📨 Message TCP de " + clientId + ": " + inputLine);

                    // Diffuser les messages aux autres clients
                    if (inputLine.startsWith("CLIENT_CONNECTE:")) {
                        broadcastNotification("CLIENT_ARRIVE:" + inputLine.substring(15));
                    } else if (!inputLine.startsWith("TEST:")) {
                        broadcastNotification("CLIENT_MSG:" + clientId + ":" + inputLine);
                    }
                }
            } catch (IOException e) {
                System.err.println("Erreur client " + clientId + ": " + e.getMessage());
            } finally {
                clients.remove(clientId);
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Erreur fermeture socket: " + e.getMessage());
                }
                System.out.println("Client TCP déconnecté: " + clientId);
            }
        }
    }
}