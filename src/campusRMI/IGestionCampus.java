package campusRMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IGestionCampus extends Remote {
    // Chambres
    int ajouterChambre(String numero, String type_chambre, int capacite,
                       double prix_par_jour, String equipements,
                       String description, String statut) throws RemoteException;

    void modifierChambre(int id, String numero, String type_chambre, int capacite,
                         double prix_par_jour, String equipements,
                         String description) throws RemoteException;

    void supprimerChambre(int id) throws RemoteException;

    Chambre[] listerToutesChambres() throws RemoteException;

    Chambre rechercherChambre(int id) throws RemoteException;

    void majStatutChambre(int chambre_id, String nouveau_statut) throws RemoteException;

    // Clients
    int creerClient(String nom, String email, String telephone,
                    String type_client, String organisation) throws RemoteException;

    void modifierClient(int id, String nom, String email, String telephone,
                        String type_client, String organisation) throws RemoteException;

    void supprimerClient(int id) throws RemoteException;

    Client rechercherClient(int id) throws RemoteException;

    Client[] listerTousClients() throws RemoteException;

    // Réservations
    int creerReservation(int client_id, int chambre_id,
                         String date_debut, String date_fin) throws RemoteException;

    void modifierReservation(int reservation_id, int client_id, int chambre_id,
                             String date_debut, String date_fin) throws RemoteException;

    void annulerReservation(int reservation_id) throws RemoteException;

    boolean majStatutReservation(int reservation_id, String nouveau_statut) throws RemoteException;

    Reservation[] historiqueClient(int client_id) throws RemoteException;

    Reservation[] reservationsChambre(int chambre_id) throws RemoteException;

    Reservation[] toutesLesReservations() throws RemoteException;
}