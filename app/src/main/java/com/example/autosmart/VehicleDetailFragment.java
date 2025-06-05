package com.example.autosmart;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VehicleDetailFragment extends Fragment {

    private static final int RC_TAKE_PHOTO = 1001;
    private static final int RC_PICK_IMAGE = 1002;
    private static final int RC_PICK_PDF   = 1003;
    private static final String PREFS_NAME = "vehicle_docs_prefs";
    private static final String PREFS_KEY_PREFIX = "docs_";

    // Vistas
    private TextView tvBrandModel, tvPlate, tvYear, tvNoDocs;
    private ImageView imgBrandLogoDetail;
    private RecyclerView rvDocs;
    private MaterialButton btnAddDoc;

    // Adapter
    private DocumentAdapter docAdapter;
    private List<Document> documents = new ArrayList<>();
    private List<Document> localDocuments = new ArrayList<>();

    // Firebase
    private String vehicleId;
    private DatabaseReference docsRef;
    private StorageReference storageRef;

    // Permisos
    private Runnable pendingAction;
    private ActivityResultLauncher<String[]> permissionsLauncher;
    private Gson gson = new Gson();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prepara el launcher de permisos múltiples
        permissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted && pendingAction != null) {
                        pendingAction.run();
                    } else {
                        Toast.makeText(getContext(),
                                "Permiso requerido para continuar",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup  container,
            @Nullable Bundle     savedInstanceState
    ) {
        View root = inflater.inflate(R.layout.fragment_vehicle_detail, container, false);

        // 1) Bind vistas
        tvBrandModel       = root.findViewById(R.id.tvBrandModelDetail);
        tvPlate            = root.findViewById(R.id.tvPlateDetail);
        tvYear             = root.findViewById(R.id.tvYearDetail);
        imgBrandLogoDetail = root.findViewById(R.id.imgBrandLogoDetail);
        tvNoDocs           = root.findViewById(R.id.tvNoDocs);
        rvDocs             = root.findViewById(R.id.rvDocs);
        btnAddDoc          = root.findViewById(R.id.btnAddDoc);

        // 2) Recoge el ID del vehículo
        if (getArguments() != null) {
            vehicleId = getArguments().getString("vehicleId");
        }
        if (vehicleId == null) return root;

        // 3) Inicializa Firebase
        docsRef    = FirebaseDatabase.getInstance("https://autosmart-6e3c3-default-rtdb.firebaseio.com")
                .getReference("vehicles")
                .child(vehicleId)
                .child("documents");
        storageRef = FirebaseStorage.getInstance("gs://autosmart-6e3c3.appspot.com")
                .getReference("vehicles")
                .child(vehicleId)
                .child("documents");

        // 4) Carga datos del vehículo
        loadVehicleData(vehicleId);

        // 5) Prepara RecyclerView
        localDocuments = loadLocalDocuments(vehicleId);
        docAdapter = new DocumentAdapter(localDocuments);
        rvDocs.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDocs.setAdapter(docAdapter);

        // Controla la visibilidad del mensaje
        tvNoDocs.setVisibility(localDocuments.isEmpty() ? View.VISIBLE : View.GONE);

        // 6) Cuando quieran añadir documento, pide permisos primero
        View.OnClickListener addDocClick = v ->
                requestPermissionsAndThen(this::showAddDocBottomSheet);

        tvNoDocs .setOnClickListener(addDocClick);
        rvDocs   .setOnClickListener(addDocClick);
        btnAddDoc.setOnClickListener(addDocClick);

        // Configurar listeners del adapter
        docAdapter.setOnEditClickListener((doc, pos) -> showEditDocDialog(doc, pos));
        docAdapter.setOnDeleteClickListener((doc, pos) -> showDeleteDocDialog(doc, pos));

        return root;
    }

    private void loadVehicleData(String vid) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("vehicles")
                .child(vid);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                Vehicle v = snap.getValue(Vehicle.class);
                if (v == null) return;
                tvBrandModel.setText(v.getBrand() + " " + v.getModel());
                tvPlate     .setText(v.getPlate());
                tvYear      .setText(v.getYear());

                // Carga logo con Glide
                String domain = v.getBrand()
                        .trim().toLowerCase()
                        .replaceAll("\\s+","")
                        .replaceAll("[^a-z0-9\\-]","")
                        + ".com";
                String token = "pk_B-lbxINDRYCQG3JiIspqjg";
                String url   = "https://img.logo.dev/" + domain
                        + "?token=" + token + "&retina=true";

                Glide.with(imgBrandLogoDetail)
                        .load(url)
                        .placeholder(R.drawable.ic_car_placeholder)
                        .error(R.drawable.ic_car_placeholder)
                        .into(imgBrandLogoDetail);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadDocuments() {
        docsRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                documents.clear();
                for (DataSnapshot ds : snap.getChildren()) {
                    Document doc = ds.getValue(Document.class);
                    if (doc != null) documents.add(doc);
                }
                docAdapter.notifyDataSetChanged();
                tvNoDocs.setVisibility(documents.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void requestPermissionsAndThen(Runnable action) {
        pendingAction = action;
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        permissionsLauncher.launch(perms.toArray(new String[0]));
    }

    private void showAddDocBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_doc_options, null, false);

        view.findViewById(R.id.optionPhoto).setOnClickListener(v -> {
            sheet.dismiss();
            startActivityForResult(
                    new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                    RC_TAKE_PHOTO
            );
        });
        view.findViewById(R.id.optionGallery).setOnClickListener(v -> {
            sheet.dismiss();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"), RC_PICK_IMAGE);
        });
        view.findViewById(R.id.optionPdf).setOnClickListener(v -> {
            sheet.dismiss();
            Intent pdf = new Intent(Intent.ACTION_GET_CONTENT);
            pdf.setType("application/pdf");
            startActivityForResult(pdf, RC_PICK_PDF);
        });

        sheet.setContentView(view);
        sheet.show();
    }

    @Override
    public void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (res != Activity.RESULT_OK || data == null) return;

        Uri    fileUri = data.getData();
        Bitmap photo   = (req == RC_TAKE_PHOTO && data.getExtras()!=null)
                ? (Bitmap) data.getExtras().get("data")
                : null;

        // Diálogo para nombre/tipo de doc
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_doc, null, false);
        EditText etDocName = dialogView.findViewById(R.id.etDocName);
        ChipGroup chipGroup = dialogView.findViewById(R.id.chipGroupDocType);

        new AlertDialog.Builder(requireContext())
                .setTitle("Clasificar documento")
                .setView(dialogView)
                .setPositiveButton("Guardar", (d, w) -> {
                    String name = etDocName.getText().toString().trim();
                    String type = "";
                    int checked = chipGroup.getCheckedChipId();
                    if (checked != -1) {
                        type = ((Chip) dialogView.findViewById(checked)).getText().toString();
                    }
                    if (photo != null) {
                        uploadPhoto(photo, name, type);
                    } else if (fileUri != null) {
                        uploadFile(fileUri, name, type);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void uploadPhoto(Bitmap bmp, String name, String type) {
        String filename = "IMG_" + System.currentTimeMillis() + ".jpg";
        File file = new File(requireContext().getFilesDir(), filename);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            String localPath = file.getAbsolutePath();
            Document doc = new Document(name, type, localPath, System.currentTimeMillis(), "image");
            localDocuments.add(doc);
            saveLocalDocuments(vehicleId, localDocuments);
            docAdapter.notifyDataSetChanged();
            tvNoDocs.setVisibility(localDocuments.isEmpty() ? View.VISIBLE : View.GONE);
            showSavedSnackbar();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error guardando documento local", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void uploadFile(Uri uri, String name, String type) {
        String filename = "DOC_" + System.currentTimeMillis();
        File file = new File(requireContext().getFilesDir(), filename);
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            String localPath = file.getAbsolutePath();
            String fileType = uri.toString().endsWith(".pdf") ? "pdf" : "image";
            Document doc = new Document(name, type, localPath, System.currentTimeMillis(), fileType);
            localDocuments.add(doc);
            saveLocalDocuments(vehicleId, localDocuments);
            docAdapter.notifyDataSetChanged();
            tvNoDocs.setVisibility(localDocuments.isEmpty() ? View.VISIBLE : View.GONE);
            showSavedSnackbar();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error guardando documento local", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void showSavedSnackbar() {
        View rootView = getView();
        if (rootView == null) return;
        Snackbar snackbar = Snackbar.make(rootView, "  Documento guardado", Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.purple_500));
        snackbar.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        snackbar.setAction("OK", v -> {});
        snackbar.show();
    }

    private void showEditSuccessSnackbar() {
        View rootView = getView();
        if (rootView == null) return;
        Snackbar snackbar = Snackbar.make(rootView, "  ¡Documentación editada con éxito!", Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.purple_500));
        snackbar.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        snackbar.setAction("OK", v -> {});
        snackbar.show();
    }

    // Modelo de documento
    public static class Document {
        public String name, type, url, fileType;
        public long   date;
        public Document() {}
        public Document(String n, String t, String u, long d, String ft) {
            name = n; type = t; url = u; date = d; fileType = ft;
        }
    }

    // Adapter interno
    static class DocumentAdapter
            extends RecyclerView.Adapter<DocumentAdapter.DocVH> {
        private final List<Document> docs;
        private OnEditClickListener editListener;
        private OnDeleteClickListener deleteListener;

        public interface OnEditClickListener {
            void onEditClick(Document doc, int position);
        }

        public interface OnDeleteClickListener {
            void onDeleteClick(Document doc, int position);
        }

        DocumentAdapter(List<Document> docs) { 
            this.docs = docs; 
        }

        public void setOnEditClickListener(OnEditClickListener listener) {
            this.editListener = listener;
        }

        public void setOnDeleteClickListener(OnDeleteClickListener listener) {
            this.deleteListener = listener;
        }

        @NonNull @Override
        public DocVH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            View iv = LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_document, p, false);
            return new DocVH(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull DocVH h, int pos) {
            Document d = docs.get(pos);
            h.tvName.setText(d.name);
            h.tvType.setText(d.type);
            h.tvDate.setText(DateFormat.format("dd/MM/yyyy", d.date));
            if ("image".equals(d.fileType)) {
                Glide.with(h.itemView).load(new File(d.url)).into(h.imgThumb);
            } else {
                h.imgThumb.setImageResource(R.drawable.ic_document);
            }

            // Click en el documento para verlo
            h.itemView.setOnClickListener(v -> {
                if ("image".equals(d.fileType)) {
                    File file = new File(d.url);
                    Context context = v.getContext();
                    Uri uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            context.getPackageName() + ".provider",
                            file
                    );
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(uri, "image/*");
                    i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(i);
                } else {
                    File file = new File(d.url);
                    Context context = v.getContext();
                    Uri uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            context.getPackageName() + ".provider",
                            file
                    );
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(uri, "application/pdf");
                    i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(i);
                }
            });

            // Click en editar
            h.btnEdit.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onEditClick(d, h.getAdapterPosition());
                }
            });

            // Click en eliminar
            h.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(d, h.getAdapterPosition());
                }
            });
        }

        @Override public int getItemCount() { return docs.size(); }

        static class DocVH extends RecyclerView.ViewHolder {
            ImageView imgThumb;
            TextView tvName, tvType, tvDate;
            ImageButton btnEdit, btnDelete;

            DocVH(@NonNull View v) {
                super(v);
                imgThumb = v.findViewById(R.id.imgDocThumb);
                tvName = v.findViewById(R.id.tvDocName);
                tvType = v.findViewById(R.id.tvDocType);
                tvDate = v.findViewById(R.id.tvDocDate);
                btnEdit = v.findViewById(R.id.btnEditDoc);
                btnDelete = v.findViewById(R.id.btnDeleteDoc);
            }
        }
    }

    private void saveLocalDocuments(String vehicleId, List<Document> docs) {
        String json = gson.toJson(docs);
        requireContext().getSharedPreferences(PREFS_NAME, 0)
            .edit()
            .putString(PREFS_KEY_PREFIX + vehicleId, json)
            .apply();
    }

    private List<Document> loadLocalDocuments(String vehicleId) {
        String json = requireContext().getSharedPreferences(PREFS_NAME, 0)
            .getString(PREFS_KEY_PREFIX + vehicleId, null);
        if (json == null) return new ArrayList<>();
        return gson.fromJson(json, new TypeToken<List<Document>>(){}.getType());
    }

    private void showEditDocDialog(Document doc, int position) {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_doc, null, false);
        EditText etDocName = dialogView.findViewById(R.id.etDocName);
        ChipGroup chipGroup = dialogView.findViewById(R.id.chipGroupDocType);

        // Pre-llenar datos
        etDocName.setText(doc.name);
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            if (chip.getText().toString().equals(doc.type)) {
                chip.setChecked(true);
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Editar documento")
                .setView(dialogView)
                .setPositiveButton("Guardar", (d, w) -> {
                    String name = etDocName.getText().toString().trim();
                    String type = "";
                    int checked = chipGroup.getCheckedChipId();
                    if (checked != -1) {
                        type = ((Chip) dialogView.findViewById(checked)).getText().toString();
                    }
                    if (!name.isEmpty() && !type.isEmpty()) {
                        doc.name = name;
                        doc.type = type;
                        localDocuments.set(position, doc);
                        saveLocalDocuments(vehicleId, localDocuments);
                        docAdapter.notifyItemChanged(position);
                        showEditSuccessSnackbar();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showDeleteDocDialog(Document doc, int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿Eliminar documento?")
                .setMessage("Esta acción no se puede deshacer. ¿Seguro que quieres eliminar este documento?")
                .setIcon(R.drawable.ic_delete)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    // Eliminar archivo local
                    File file = new File(doc.url);
                    if (file.exists()) {
                        file.delete();
                    }
                    // Eliminar de la lista y guardar
                    localDocuments.remove(position);
                    saveLocalDocuments(vehicleId, localDocuments);
                    docAdapter.notifyItemRemoved(position);
                    // Mostrar mensaje
                    View rootView = getView();
                    if (rootView != null) {
                        Snackbar snackbar = Snackbar.make(
                            rootView,
                            "Documento eliminado",
                            Snackbar.LENGTH_LONG
                        );
                        snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.red_500));
                        snackbar.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                        snackbar.setAction("OK", v -> {});
                        snackbar.show();
                    }
                    // Actualizar visibilidad del mensaje de lista vacía
                    tvNoDocs.setVisibility(localDocuments.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .show();
    }
}
