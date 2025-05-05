package com.example.autosmart;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.provider.Settings;

public class VehicleDetailFragment extends Fragment {
    private static final int RC_PICK_IMAGE = 1001;
    private static final int RC_TAKE_PHOTO = 1002;
    private static final int RC_PICK_PDF = 1003;

    private TextView tvBrandModel, tvPlate, tvYear;
    private RecyclerView rvDocs;
    private DocumentAdapter docAdapter;
    private List<Document> documents = new ArrayList<>();
    private String vehicleId;
    private DatabaseReference docsRef;
    private StorageReference storageRef;
    // Permisos
    private ActivityResultLauncher<String> singlePermissionLauncher;
    private String pendingPermission;
    private Runnable pendingAction;
    private ImageView imgBrandLogoDetail;
    private TextView tvNoDocs;
    private MaterialButton btnAddDoc;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_vehicle_detail, container, false);

        tvBrandModel = root.findViewById(R.id.tvBrandModelDetail);
        tvPlate = root.findViewById(R.id.tvPlateDetail);
        tvYear = root.findViewById(R.id.tvYearDetail);
        rvDocs = root.findViewById(R.id.rvDocs);
        imgBrandLogoDetail = root.findViewById(R.id.imgBrandLogoDetail);
        tvNoDocs = root.findViewById(R.id.tvNoDocs);
        btnAddDoc = root.findViewById(R.id.btnAddDoc);

        // Recibe el ID del vehículo por argumentos
        vehicleId = getArguments() != null ? getArguments().getString("vehicleId") : null;
        if (vehicleId == null) return root;

        // Referencias Firebase
        docsRef = FirebaseDatabase.getInstance().getReference("vehiculos").child(vehicleId).child("documentos");
        storageRef = FirebaseStorage.getInstance().getReference("vehiculos").child(vehicleId).child("documentos");

        // Cargar datos del vehículo (puedes obtenerlos por argumentos o desde Firebase)
        loadVehicleData(vehicleId);

        // Documentos
        docAdapter = new DocumentAdapter(documents);
        rvDocs.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDocs.setAdapter(docAdapter);

        loadDocuments();

        // Permisos robustos
        singlePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted && pendingAction != null) {
                    pendingAction.run();
                } else if (!isGranted && pendingPermission != null) {
                    if (!shouldShowRequestPermissionRationale(pendingPermission)) {
                        showPermissionSettingsDialog();
                    } else {
                        Toast.makeText(getContext(), "Permiso requerido para continuar", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
        // Click en el área de documentación (título o lista)
        View.OnClickListener addDocClick = v -> showAddDocBottomSheet();
        rvDocs.setOnClickListener(v -> showAddDocBottomSheet());

        btnAddDoc.setOnClickListener(v -> showAddDocBottomSheet());

        return root;
    }

    private void loadVehicleData(String vehicleId) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("vehicles").child(vehicleId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                Vehicle v = snap.getValue(Vehicle.class);
                if (v != null) {
                    tvBrandModel.setText(v.getBrand() + " " + v.getModel());
                    tvPlate.setText(v.getPlate());
                    tvYear.setText(v.getYear());
                    if (imgBrandLogoDetail != null && v.getBrand() != null) {
                        String brandRaw = v.getBrand().trim().toLowerCase();
                        String brandDomain = brandRaw.replaceAll("\\s+", "").replaceAll("[^a-z0-9\\-]", "");
                        String domain = brandDomain + ".com";
                        String token = "pk_B-lbxINDRYCQG3JiIspqjg";
                        String url = "https://img.logo.dev/" + domain + "?token=" + token + "&retina=true";
                        Glide.with(imgBrandLogoDetail.getContext())
                            .load(url)
                            .placeholder(R.drawable.ic_car_placeholder)
                            .error(R.drawable.ic_car_placeholder)
                            .into(imgBrandLogoDetail);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
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
                if (documents.isEmpty()) {
                    tvNoDocs.setVisibility(View.VISIBLE);
                } else {
                    tvNoDocs.setVisibility(View.GONE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // BottomSheetDialog para elegir acción
    private void showAddDocBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_doc_options, null, false);
        view.findViewById(R.id.optionPhoto).setOnClickListener(v -> {
            sheet.dismiss();
            requestPhotoPermissions();
        });
        view.findViewById(R.id.optionGallery).setOnClickListener(v -> {
            sheet.dismiss();
            requestGalleryPermissions();
        });
        view.findViewById(R.id.optionPdf).setOnClickListener(v -> {
            sheet.dismiss();
            requestPdfPermissions();
        });
        sheet.setContentView(view);
        sheet.show();
    }

    // Métodos para pedir permisos robustos y lanzar acción
    private void requestPhotoPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingPermission = Manifest.permission.CAMERA;
            pendingAction = this::requestPhotoPermissions;
            singlePermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                pendingPermission = Manifest.permission.READ_MEDIA_IMAGES;
                pendingAction = this::requestPhotoPermissions;
                singlePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                return;
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                pendingPermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                pendingAction = this::requestPhotoPermissions;
                singlePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return;
            }
        }
        // Todos los permisos OK
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePicture, RC_TAKE_PHOTO);
    }

    private void requestGalleryPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                pendingPermission = Manifest.permission.READ_MEDIA_IMAGES;
                pendingAction = this::requestGalleryPermissions;
                singlePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                pendingPermission = Manifest.permission.READ_EXTERNAL_STORAGE;
                pendingAction = this::requestGalleryPermissions;
                singlePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                return;
            }
        }
        Intent pickImage = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickImage, RC_PICK_IMAGE);
    }

    private void requestPdfPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                pendingPermission = Manifest.permission.READ_MEDIA_IMAGES;
                pendingAction = this::requestPdfPermissions;
                singlePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                pendingPermission = Manifest.permission.READ_EXTERNAL_STORAGE;
                pendingAction = this::requestPdfPermissions;
                singlePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                return;
            }
        }
        Intent pickPdf = new Intent(Intent.ACTION_GET_CONTENT);
        pickPdf.setType("application/pdf");
        startActivityForResult(pickPdf, RC_PICK_PDF);
    }

    // Dialogo para ir a ajustes si el permiso está denegado permanentemente
    private void showPermissionSettingsDialog() {
        new AlertDialog.Builder(getContext())
            .setTitle("Permiso requerido")
            .setMessage("Debes conceder el permiso en Ajustes para poder añadir documentos.")
            .setPositiveButton("Abrir ajustes", (d, w) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(android.net.Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(intent);
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) return;

        final Uri fileUri = data.getData();
        final Bitmap photo = (requestCode == RC_TAKE_PHOTO && data.getExtras() != null) ? 
            (Bitmap) data.getExtras().get("data") : null;

        // Diálogo para clasificar y nombrar el documento
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_doc, null, false);
        EditText etDocName = dialogView.findViewById(R.id.etDocName);
        ChipGroup chipGroup = dialogView.findViewById(R.id.chipGroupDocType);

        new AlertDialog.Builder(getContext())
                .setTitle("Clasificar documento")
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String docName = etDocName.getText().toString().trim();
                    String docType = "";
                    int checkedId = chipGroup.getCheckedChipId();
                    if (checkedId != -1) {
                        Chip chip = dialogView.findViewById(checkedId);
                        docType = chip.getText().toString();
                    }
                    if (photo != null) {
                        uploadPhoto(photo, docName, docType);
                    } else if (fileUri != null) {
                        uploadFile(fileUri, docName, docType);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void uploadPhoto(Bitmap photo, String docName, String docType) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] data = baos.toByteArray();
        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
        StorageReference fileRef = storageRef.child(fileName);
        fileRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveDocMetadata(docName, docType, uri.toString(), "image");
                }));
    }

    private void uploadFile(Uri fileUri, String docName, String docType) {
        String fileName = "DOC_" + System.currentTimeMillis();
        StorageReference fileRef = storageRef.child(fileName);
        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String type = fileUri.toString().endsWith(".pdf") ? "pdf" : "image";
                    saveDocMetadata(docName, docType, uri.toString(), type);
                }));
    }

    private void saveDocMetadata(String name, String type, String url, String fileType) {
        String key = docsRef.push().getKey();
        Document doc = new Document(name, type, url, System.currentTimeMillis(), fileType);
        docsRef.child(key).setValue(doc);
    }

    // Modelo simple de documento
    public static class Document {
        public String name, type, url, fileType;
        public long date;
        public Document() {}
        public Document(String name, String type, String url, long date, String fileType) {
            this.name = name; this.type = type; this.url = url; this.date = date; this.fileType = fileType;
        }
    }

    // Adaptador para la lista de documentos
    static class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocViewHolder> {
        List<Document> docs;
        DocumentAdapter(List<Document> docs) { this.docs = docs; }
        @NonNull
        @Override
        public DocViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document, parent, false);
            return new DocViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull DocViewHolder h, int pos) {
            Document d = docs.get(pos);
            h.tvName.setText(d.name);
            h.tvType.setText(d.type);
            h.tvDate.setText(DateFormat.format("dd/MM/yyyy", d.date));
            if (d.fileType != null && d.fileType.equals("image")) {
                Glide.with(h.itemView.getContext()).load(d.url).into(h.imgThumb);
            } else {
                h.imgThumb.setImageResource(R.drawable.ic_document);
            }
            h.itemView.setOnClickListener(view -> {
                if (d.fileType != null && d.fileType.equals("image")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                    ImageView img = new ImageView(view.getContext());
                    Glide.with(view.getContext()).load(d.url).into(img);
                    builder.setView(img).setPositiveButton("Cerrar", null).show();
                } else if (d.fileType != null && d.fileType.equals("pdf")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(d.url), "application/pdf");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    view.getContext().startActivity(intent);
                }
            });
        }
        @Override
        public int getItemCount() { return docs.size(); }
        static class DocViewHolder extends RecyclerView.ViewHolder {
            ImageView imgThumb;
            TextView tvName, tvType, tvDate;
            DocViewHolder(View v) {
                super(v);
                imgThumb = v.findViewById(R.id.imgDocThumb);
                tvName = v.findViewById(R.id.tvDocName);
                tvType = v.findViewById(R.id.tvDocType);
                tvDate = v.findViewById(R.id.tvDocDate);
            }
        }
    }
} 