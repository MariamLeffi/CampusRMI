package campusRMI;

import java.io.Serializable;

public class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;

    public int reservation_id;
    public int client_id;
    public int chambre_id;
    public String date_debut;
    public String date_fin;
    public double total;
    public String statut;

    // Constructeurs, getters, setters...
    public Reservation() {}

    public Reservation(int reservation_id, int client_id, int chambre_id,
                       String date_debut, String date_fin, double total,
                       String statut) {
        this.reservation_id = reservation_id;
        this.client_id = client_id;
        this.chambre_id = chambre_id;
        this.date_debut = date_debut;
        this.date_fin = date_fin;
        this.total = total;
        this.statut = statut;
    }
}