package ma.projet.restclient;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ma.projet.restclient.adapter.CompteAdapter;
import ma.projet.restclient.entities.Compte;
import ma.projet.restclient.repository.CompteRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité principale de l'application de gestion de comptes bancaires.
 * Cette activité gère l'affichage de la liste des comptes et les interactions utilisateur
 * pour ajouter, modifier ou supprimer des comptes.
 * 
 * Implémente les interfaces OnDeleteClickListener et OnUpdateClickListener pour gérer
 * les clics sur les boutons de suppression et de mise à jour dans la liste des comptes.
 */
public class MainActivity extends AppCompatActivity implements CompteAdapter.OnDeleteClickListener, CompteAdapter.OnUpdateClickListener {
    // Composants de l'interface utilisateur
    private RecyclerView recyclerView;      // Liste défilante pour afficher les comptes
    private CompteAdapter adapter;          // Adaptateur pour la RecyclerView
    private RadioGroup formatGroup;         // Groupe de boutons pour sélectionner le format (JSON/XML)
    private FloatingActionButton addbtn;     // Bouton flottant pour ajouter un nouveau compte

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation des vues et configuration des écouteurs
        initViews();
        setupRecyclerView();
        setupFormatSelection();
        setupAddButton();

        // Chargement initial des données au format JSON par défaut
        loadData("JSON");
    }

    /**
     * Initialise les références aux vues du layout
     */
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);  // Référence à la RecyclerView
        formatGroup = findViewById(R.id.formatGroup);    // Groupe de boutons radio pour le format
        addbtn = findViewById(R.id.fabAdd);              // Bouton d'ajout flottant
    }

    /**
     * Configure la RecyclerView avec son layout manager et son adaptateur
     */
    private void setupRecyclerView() {
        // Utilisation d'un LinearLayoutManager pour une liste verticale simple
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Création de l'adaptateur en lui passant le contexte et cette activité comme écouteur
        adapter = new CompteAdapter(this, this);
        
        // Association de l'adaptateur à la RecyclerView
        recyclerView.setAdapter(adapter);
    }

    /**
     * Configure l'écouteur pour la sélection du format de données (JSON/XML)
     */
    private void setupFormatSelection() {
        formatGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // Détermine le format sélectionné (JSON ou XML) en fonction du bouton radio coché
            String format = checkedId == R.id.radioJson ? "JSON" : "XML";
            // Recharge les données avec le format sélectionné
            loadData(format);
        });
    }

    /**
     * Configure le clic sur le bouton d'ajout
     */
    private void setupAddButton() {
        addbtn.setOnClickListener(v -> showAddCompteDialog());
    }

    /**
     * Affiche une boîte de dialogue pour ajouter un nouveau compte
     */
    private void showAddCompteDialog() {
        // Création d'un constructeur de boîte de dialogue
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        
        // Récupération de la vue personnalisée pour la boîte de dialogue
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_compte, null);

        // Récupération des vues du formulaire
        EditText etSolde = dialogView.findViewById(R.id.etSolde);
        RadioGroup typeGroup = dialogView.findViewById(R.id.typeGroup);

        // Configuration des boutons de la boîte de dialogue
        builder.setView(dialogView)
                .setTitle("Ajouter un compte")
                .setPositiveButton("Ajouter", (dialog, which) -> {
                    // Récupération des valeurs du formulaire
                    String solde = etSolde.getText().toString();
                    // Détermination du type de compte en fonction du bouton radio sélectionné
                    String type = typeGroup.getCheckedRadioButtonId() == R.id.radioCourant
                            ? "COURANT" : "EPARGNE";

                    // Création d'un nouvel objet Compte avec les données du formulaire
                    String formattedDate = getCurrentDateFormatted();
                    Compte compte = new Compte(null, Double.parseDouble(solde), type, formattedDate);
                    
                    // Appel de la méthode pour ajouter le compte
                    addCompte(compte);
                })
                .setNegativeButton("Annuler", null); // Bouton Annuler sans action

        // Affichage de la boîte de dialogue
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Retourne la date courante au format "yyyy-MM-dd"
     * @return La date courante formatée en chaîne de caractères
     */
    private String getCurrentDateFormatted() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(calendar.getTime());
    }

    /**
     * Ajoute un nouveau compte via le repository
     * @param compte Le compte à ajouter
     */
    private void addCompte(Compte compte) {
        // Création d'une instance du repository avec le format JSON
        CompteRepository compteRepository = new CompteRepository("JSON");
        
        // Appel asynchrone pour ajouter le compte
        compteRepository.addCompte(compte, new Callback<Compte>() {
            @Override
            public void onResponse(Call<Compte> call, Response<Compte> response) {
                if (response.isSuccessful()) {
                    // Affichage d'un message de succès et rechargement des données
                    showToast("Compte ajouté avec succès");
                    loadData("JSON");
                } else {
                    // Gestion des erreurs de l'API
                    showToast("Erreur lors de l'ajout du compte: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Compte> call, Throwable t) {
                // Gestion des échecs de connexion
                showToast("Erreur de connexion: " + t.getMessage());
            }
        });
    }

    /**
     * Charge la liste des comptes depuis le serveur
     * @param format Le format de données à utiliser (JSON ou XML)
     */
    private void loadData(String format) {
        // Création d'une instance du repository avec le format spécifié
        CompteRepository compteRepository = new CompteRepository(format);
        
        // Appel asynchrone pour récupérer la liste des comptes
        compteRepository.getAllCompte(new Callback<List<Compte>>() {
            @Override
            public void onResponse(Call<List<Compte>> call, Response<List<Compte>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Récupération de la liste des comptes
                    List<Compte> comptes = response.body();
                    
                    // Mise à jour de l'interface utilisateur sur le thread principal
                    runOnUiThread(() -> {
                        adapter.updateData(comptes);
                        showToast("Données chargées avec succès (" + format + ")");
                    });
                } else {
                    showToast("Erreur lors du chargement des données: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<Compte>> call, Throwable t) {
                // En cas d'échec de la requête
                showToast("Erreur de connexion: " + t.getMessage());
            }
        });
    }

    /**
     * Gère le clic sur le bouton de mise à jour d'un compte
     * @param compte Le compte à mettre à jour
     */
    @Override
    public void onUpdateClick(Compte compte) {
        showUpdateCompteDialog(compte);
    }

    /**
     * Affiche une boîte de dialogue pour modifier un compte existant
     * @param compte Le compte à modifier
     */
    private void showUpdateCompteDialog(Compte compte) {
        // Création d'un constructeur de boîte de dialogue
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        
        // Récupération de la vue personnalisée pour la boîte de dialogue
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_compte, null);

        // Récupération des vues du formulaire
        EditText etSolde = dialogView.findViewById(R.id.etSolde);
        RadioGroup typeGroup = dialogView.findViewById(R.id.typeGroup);
        
        // Pré-remplissage des champs avec les valeurs actuelles du compte
        etSolde.setText(String.valueOf(compte.getSolde()));
        
        // Sélection du bon bouton radio en fonction du type de compte
        if (compte.getType().equalsIgnoreCase("COURANT")) {
            typeGroup.check(R.id.radioCourant);
        } else if (compte.getType().equalsIgnoreCase("EPARGNE")) {
            typeGroup.check(R.id.radioEpargne);
        }

        // Configuration des boutons de la boîte de dialogue
        builder.setView(dialogView)
                .setTitle("Modifier le compte")
                .setPositiveButton("Modifier", (dialog, which) -> {
                    // Récupération des valeurs modifiées
                    String solde = etSolde.getText().toString();
                    String type = typeGroup.getCheckedRadioButtonId() == R.id.radioCourant
                            ? "COURANT" : "EPARGNE";
                    
                    // Mise à jour de l'objet compte avec les nouvelles valeurs
                    compte.setSolde(Double.parseDouble(solde));
                    compte.setType(type);
                    
                    // Appel de la méthode pour mettre à jour le compte
                    updateCompte(compte);
                })
                .setNegativeButton("Annuler", null); // Bouton Annuler sans action

        // Affichage de la boîte de dialogue
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Met à jour un compte existant via le repository
     * @param compte Le compte avec les modifications
     */
    private void updateCompte(Compte compte) {
        // Création d'une instance du repository avec le format JSON
        CompteRepository compteRepository = new CompteRepository("JSON");
        
        // Appel asynchrone pour mettre à jour le compte
        compteRepository.updateCompte(compte.getId(), compte, new Callback<Compte>() {
            @Override
            public void onResponse(Call<Compte> call, Response<Compte> response) {
                if (response.isSuccessful()) {
                    // Affichage d'un message de succès et rechargement des données
                    showToast("Compte modifié avec succès");
                    loadData("JSON");
                } else {
                    // Gestion des erreurs de l'API
                    showToast("Erreur lors de la modification: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Compte> call, Throwable t) {
                // Gestion des échecs de connexion
                showToast("Erreur de connexion: " + t.getMessage());
            }
        });
    }

    /**
     * Gère le clic sur le bouton de suppression d'un compte
     * @param compte Le compte à supprimer
     */
    @Override
    public void onDeleteClick(Compte compte) {
        showDeleteConfirmationDialog(compte);
    }

    /**
     * Affiche une boîte de dialogue de confirmation avant la suppression
     * @param compte Le compte à supprimer
     */
    private void showDeleteConfirmationDialog(Compte compte) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmation de suppression")
                .setMessage("Êtes-vous sûr de vouloir supprimer ce compte ?\nCette action est irréversible.")
                .setPositiveButton("Oui", (dialog, which) -> deleteCompte(compte))
                .setNegativeButton("Non", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Supprime un compte via le repository
     * @param compte Le compte à supprimer
     */
    private void deleteCompte(Compte compte) {
        // Création d'une instance du repository avec le format JSON
        CompteRepository compteRepository = new CompteRepository("JSON");
        
        // Appel asynchrone pour supprimer le compte
        compteRepository.deleteCompte(compte.getId(), new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Affichage d'un message de succès et rechargement des données
                    showToast("Compte supprimé avec succès");
                    loadData("JSON");
                } else {
                    // Gestion des erreurs de l'API
                    showToast("Erreur lors de la suppression: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Gestion des échecs de connexion
                showToast("Erreur de connexion: " + t.getMessage());
            }
        });
    }

    /**
     * Affiche un message Toast à l'écran
     * @param message Le message à afficher
     */
    private void showToast(String message) {
        // S'assure que le Toast est affiché sur le thread principal
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
    }
}