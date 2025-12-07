package campusRMI;

import java.io.Serializable;

public class Chambre implements Serializable {
    private static final long serialVersionUID = 1L;

    public int chambre_id;
    public String numero;
    public String type_chambre;
    public int capacite;
    public double prix_par_jour;
    public String equipements;
    public String statut;
    public String description;

    // Constructeurs, getters, setters...
    public Chambre() {}

    public Chambre(int chambre_id, String numero, String type_chambre, int capacite,
                   double prix_par_jour, String equipements, String statut,
                   String description) {
        this.chambre_id = chambre_id;
        this.numero = numero;
        this.type_chambre = type_chambre;
        this.capacite = capacite;
        this.prix_par_jour = prix_par_jour;
        this.equipements = equipements;
        this.statut = statut;
        this.description = description;
    }
}