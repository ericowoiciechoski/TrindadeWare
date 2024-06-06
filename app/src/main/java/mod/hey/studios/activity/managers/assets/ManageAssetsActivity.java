package mod.hey.studios.activity.managers.assets;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.PopupMenu;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sketchware.remod.R;
import com.sketchware.remod.databinding.DialogCreateNewFileLayoutBinding;
import com.sketchware.remod.databinding.DialogInputLayoutBinding;
import com.sketchware.remod.databinding.ManageFileBinding;
import com.sketchware.remod.databinding.ManageJavaItemHsBinding;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import mod.SketchwareUtil;
import mod.agus.jcoderz.lib.FilePathUtil;
import mod.agus.jcoderz.lib.FileUtil;
import mod.hey.studios.code.SrcCodeEditor;
import mod.hey.studios.util.Helper;
import mod.jbk.util.AddMarginOnApplyWindowInsetsListener;

public class ManageAssetsActivity extends AppCompatActivity {

    private final ArrayList<String> currentTree = new ArrayList<>();
    private String current_path;
    private FilePathUtil fpu;
    private AssetsAdapter assetsAdapter;
    private String sc_id;

    private ManageFileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ManageFileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sc_id = getIntent().getStringExtra("sc_id");
        Helper.fixFileprovider();
        setupUI();

        fpu = new FilePathUtil();
        current_path = Uri.parse(fpu.getPathAssets(sc_id)).getPath();

        refresh();
    }

    private void setupUI() {
        binding.topAppBar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));
        binding.showOptionsButton.setOnClickListener(view -> hideShowOptionsButton(false));
        binding.closeButton.setOnClickListener(view -> hideShowOptionsButton(true));
        binding.createNewButton.setOnClickListener(v -> {
            showCreateDialog();
            hideShowOptionsButton(true);
        });
        binding.importNewButton.setOnClickListener(v -> {
            showImportDialog();
            hideShowOptionsButton(true);
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.createNewButton,
                new AddMarginOnApplyWindowInsetsListener(WindowInsetsCompat.Type.navigationBars(), WindowInsetsCompat.CONSUMED));
    }

    private void hideShowOptionsButton(boolean isHide) {
        binding.optionsLayout.animate()
                .translationY(isHide ? 300 : 0)
                .alpha(isHide ? 0 : 1)
                .setInterpolator(new OvershootInterpolator());

        binding.showOptionsButton.animate()
                .translationY(isHide ? 0 : 300)
                .alpha(isHide ? 1 : 0)
                .setInterpolator(new OvershootInterpolator());
    }

    @Override
    public void onBackPressed() {
        if (Objects.equals(
                Uri.parse(current_path).getPath(),
                Uri.parse(fpu.getPathAssets(sc_id)).getPath()
        )) {
            super.onBackPressed();
        } else {
            current_path = current_path.substring(0, current_path.lastIndexOf(DialogConfigs.DIRECTORY_SEPERATOR));
            refresh();
        }
    }

    @SuppressLint("SetTextI18n")
    private void showCreateDialog() {
        DialogCreateNewFileLayoutBinding dialogBinding = DialogCreateNewFileLayoutBinding.inflate(getLayoutInflater());
        var inputText = dialogBinding.inputText;

        var dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setTitle("Create new")
                .setMessage("If you're creating a file, make sure to add an extension.")
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Create", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = ((AlertDialog) dialogInterface).getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String editable = inputText.getText().toString().trim();

                if (editable.isEmpty()) {
                    SketchwareUtil.toastError("Invalid name");
                    return;
                }

                int checkedChipId = dialogBinding.chipGroupTypes.getCheckedChipId();
                if (checkedChipId == R.id.chip_file) {
                    FileUtil.writeFile(new File(current_path, editable).getAbsolutePath(), "");
                } else if (checkedChipId == R.id.chip_folder) {
                    FileUtil.makeDir(new File(current_path, editable).getAbsolutePath());
                } else {
                    SketchwareUtil.toast("Select a file type");
                    return;
                }

                refresh();
                SketchwareUtil.toast("File was created successfully");
                dialogInterface.dismiss();
            });
        });

        dialogBinding.chipFile.setVisibility(View.VISIBLE);
        dialogBinding.chipFolder.setVisibility(View.VISIBLE);

        dialog.setView(dialogBinding.getRoot());
        dialog.show();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        inputText.requestFocus();
    }

    private void showImportDialog() {
        DialogProperties properties = new DialogProperties();

        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_AND_DIR_SELECT;
        properties.root = Environment.getExternalStorageDirectory();
        properties.error_dir = Environment.getExternalStorageDirectory();
        properties.offset = Environment.getExternalStorageDirectory();
        properties.extensions = null;

        FilePickerDialog dialog = new FilePickerDialog(this, properties);
        dialog.setTitle("Select an asset file");
        dialog.setDialogSelectionListener(selections -> {
            for (String path : selections) {
                File file = new File(path);
                try {
                    FileUtil.copyDirectory(file, new File(current_path, file.getName()));
                    refresh();
                } catch (IOException e) {
                    SketchwareUtil.toastError("Couldn't import file! [" + e.getMessage() + "]");
                }
            }
        });

        dialog.show();
    }

    private void showRenameDialog(final int position) {
        DialogInputLayoutBinding dialogBinding = DialogInputLayoutBinding.inflate(getLayoutInflater());

        var inputText = dialogBinding.inputText;

        var dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Rename " + assetsAdapter.getFileName(position))
                .setView(dialogBinding.getRoot())
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Rename", (dialogInterface, i) -> {
                    if (!inputText.getText().toString().isEmpty()) {
                        FileUtil.renameFile(assetsAdapter.getItem(position), new File(current_path, inputText.getText().toString()).getAbsolutePath());
                        refresh();
                        SketchwareUtil.toast("Renamed successfully");
                    }
                    dialogInterface.dismiss();
                })
                .create();

        inputText.setText(assetsAdapter.getFileName(position));

        dialog.show();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        inputText.requestFocus();
    }

    private void showDeleteDialog(final int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete " + assetsAdapter.getFileName(position) + "?")
                .setMessage("Are you sure you want to delete this " + (assetsAdapter.isFolder(position) ? "folder" : "file") + "? "
                        + "This action cannot be undone.")
                .setPositiveButton(R.string.common_word_delete, (dialog, which) -> {
                    FileUtil.deleteFile(assetsAdapter.getItem(position));
                    refresh();
                    SketchwareUtil.toast("Deleted successfully");
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .create()
                .show();
    }

    private void refresh() {
        if (!FileUtil.isExistFile(fpu.getPathAssets(sc_id))) {
            FileUtil.makeDir(fpu.getPathAssets(sc_id));
            refresh();
        }

        currentTree.clear();
        FileUtil.listDir(current_path, currentTree);
        Helper.sortPaths(currentTree);

        assetsAdapter = new AssetsAdapter();
        binding.filesListRecyclerView.setAdapter(assetsAdapter);
        if (currentTree.size() == 0) {
            binding.noContentLayout.setVisibility(View.VISIBLE);
        } else {
            binding.noContentLayout.setVisibility(View.GONE);
        }
    }

    public class AssetsAdapter extends RecyclerView.Adapter<AssetsAdapter.AssetsViewHolder> {
        @NonNull
        @Override
        public AssetsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ManageJavaItemHsBinding binding = ManageJavaItemHsBinding.inflate(inflater, parent, false);
            var layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            binding.getRoot().setLayoutParams(layoutParams);
            return new AssetsViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull AssetsViewHolder holder, int position) {
            var item = getItem(position);
            var binding = holder.binding;

            binding.title.setText(getFileName(position));
            binding.getRoot().setOnClickListener(view -> {
                if (isFolder(position)) {
                    current_path = getItem(position);
                    refresh();
                } else {
                    goEditFile(position);
                }
            });

            binding.getRoot().setOnLongClickListener(view -> {
                holder.binding.more.performClick();
                return false;
            });
            if (isFolder(position)) {
                holder.binding.icon.setImageResource(R.drawable.ic_folder_24);
            } else {
                try {
                    if (FileUtil.isImageFile(item)) {
                        Glide.with(holder.binding.icon.getContext()).load(new File(item)).into(holder.binding.icon);
                    } else {
                        holder.binding.icon.setImageResource(R.drawable.ic_file_24);
                    }
                } catch (Exception ignored) {
                    holder.binding.icon.setImageResource(R.drawable.ic_file_24);
                }
            }
            Helper.applyRipple(holder.itemView.getContext(), holder.binding.more);
            holder.binding.more.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(holder.itemView.getContext(), v);

                if (!isFolder(position)) {
                    popupMenu.getMenu().add(0, 0, 0, "Edit");
                }

                popupMenu.getMenu().add(0, 1, 0, "Rename");
                popupMenu.getMenu().add(0, 2, 0, "Delete");

                popupMenu.setOnMenuItemClickListener(itemMenu -> {
                    switch (itemMenu.getItemId()) {
                        case 0 -> goEditFile(position);
                        case 1 -> showRenameDialog(position);
                        case 2 -> showDeleteDialog(position);
                        default -> {
                            return false;
                        }
                    }

                    return true;
                });
                popupMenu.show();
            });
        }

        @Override
        public int getItemCount() {
            return currentTree.size();
        }

        public String getItem(int position) {
            return currentTree.get(position);
        }

        public String getFileName(int position) {
            String item = getItem(position);
            return item.substring(item.lastIndexOf(DialogConfigs.DIRECTORY_SEPERATOR) + 1);
        }

        public boolean isFolder(int position) {
            return FileUtil.isDirectory(getItem(position));
        }

        public void goEditFile(int position) {
            if (getItem(position).endsWith(".json") || getItem(position).endsWith(".txt")) {
                Intent launchIntent = new Intent();

                launchIntent.setClass(getApplicationContext(), SrcCodeEditor.class);
                launchIntent.putExtra("title", getFileName(position));
                launchIntent.putExtra("content", getItem(position));

                startActivity(launchIntent);
            } else {
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);

                viewIntent.setDataAndType(Uri.fromFile(new File(getItem(position))), "*/*");
                viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(viewIntent);
            }
        }

        class AssetsViewHolder extends RecyclerView.ViewHolder {
            ManageJavaItemHsBinding binding;

            public AssetsViewHolder(ManageJavaItemHsBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
