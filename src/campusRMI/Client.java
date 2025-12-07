package campusRMI;

import java.io.Serializable;

public class Client implements Serializable {
    private static final long serialVersionUID = 1L;

    public int client_id;
    public String nom;
    public String email;
    public String telephone;
    public String type_client;
    public String organisation;

    // Constructeurs, getters, setters...
    public Client() {}

    public Client(int client_id, String nom, String email, String telephone,
                  String type_client, String organisation) {
        this.client_id = client_id;
        this.nom = nom;
        this.email = email;
        this.telephone = telephone;
        this.type_client = type_client;
        this.organisation = organisation;
    }
}