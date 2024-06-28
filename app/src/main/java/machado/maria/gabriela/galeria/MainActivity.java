package machado.maria.gabriela.galeria;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static int RESULT_TAKE_PICTURE = 1;
    static int RESULT_REQUEST_PERMISSION = 2;
    String currentPhotoPath;
    List<String> photos = new ArrayList<>();
    MainAdapter mainAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File[] files = dir.listFiles(); //le as fotos ja salvas

        for(int i = 0; i < files.length; i++){ //adiciona as fotos ja salvas na atual lista de fotos
            photos.add(files[i].getAbsolutePath());
        }

        mainAdapter = new MainAdapter(MainActivity.this, photos);
        RecyclerView rvGallery = findViewById(R.id.rvGallery);
        rvGallery.setAdapter(mainAdapter);

        //calcula quantas colunas de fotos cabem na tela e exibe as fotos em grid de acordo com o resultado
        float w = getResources().getDimension(R.dimen.itemWidth);
        int numberOfColumns = Util.calculateNoOfColumns(MainActivity.this, w);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(MainActivity.this, numberOfColumns);
        rvGallery.setLayoutManager(gridLayoutManager);

        Toolbar toolbar = findViewById(R.id.tbMain);
        setSupportActionBar(toolbar); //seta tbMain como ActionBar padrao

        List<String> permissions = new ArrayList<>(); //pedindo permissao pra usar a camera
        permissions.add(Manifest.permission.CAMERA);
        checkForPermissions(permissions);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        //cria opcoes de menu
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_tb, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //sera chamado sempre que um item da toolbar for selecionado
        if (item.getItemId() == R.id.opCamera) { //caso a camera tenha sido clicada
            dispatchTakePictureIntent(); //abre a camera do celular
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startPhotoActivity(String photoPath){
        //recebe a foto que deve ser aberta em PhotoActivity, cria um intent e envia pra PhotoActivity
        Intent i = new Intent(MainActivity.this, PhotoActivity.class);
        i.putExtra("photo_path", photoPath);
        startActivity(i);
    }

    private void dispatchTakePictureIntent(){
        //dispara o app de camera
        File f = null;
        try {
            f = createImageFile(); //o arquivo que guarda a imagem
        } catch (IOException e){ //caso o arquivo nao possa ser criado, eh exibida essa mensagem
            Toast.makeText(MainActivity.this, "Não foi possível criar o arquivo", Toast.LENGTH_LONG).show();
            return;
        }

        currentPhotoPath = f.getAbsolutePath(); //local do arquivo de foto

        if(f != null) {
            //se existir o arquivo da imagem, eh disparado o app camera
            Uri fUri = FileProvider.getUriForFile(MainActivity.this, "machado.maria.gabriela.galeria.fileprovider", f);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, fUri);
            startActivityForResult(i, RESULT_TAKE_PICTURE);
        }
    }

    private File createImageFile() throws IOException {
        //cria o arquivo que vai guardar a imagem
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File f = File.createTempFile(imageFileName, ".jpg", storageDir);
        return f;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RESULT_TAKE_PICTURE){
            if(resultCode == Activity.RESULT_OK){ //caso a foto tenha sido tirada
                photos.add(currentPhotoPath); //ela eh adicionada a lista de fotos
                mainAdapter.notifyItemInserted(photos.size()-1); //notifica MainAdapter
            }
            else {
                //se a foto nao foi tirada, o arquivo f (feito para armazenar a foto) eh excluido
                File f =  new File(currentPhotoPath);
                f.delete();
            }
        }
    }

    private void checkForPermissions(List<String> permissions){
        List<String> permissionsNotGranted = new ArrayList<>();
        for(String permission : permissions){
            if(!hasPermission(permission)){ //caso o usuario nao tenha confirmado permissao
                permissionsNotGranted.add(permission); //a permissao negada eh posta num array
            }
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(permissionsNotGranted.size() > 0){
                //se houverem permissoes negadas, essas sao requisitadas
                requestPermissions(permissionsNotGranted.toArray(new String[permissionsNotGranted.size()]), RESULT_REQUEST_PERMISSION);
            }
        }
    }

    private boolean hasPermission(String permission){
        //verifica se determinada permissao foi concedida
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            return ActivityCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        //checa se todas as permissoes foram aceitas ou nao
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        final List<String> permissionsRejected = new ArrayList<>();
        if(requestCode == RESULT_REQUEST_PERMISSION){
            for(String permission : permissions){ //checa permissao por permissao
                if(!hasPermission(permission)){
                    permissionsRejected.add(permission);
                }
            }
        }
        if(permissionsRejected.size() > 0){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if(shouldShowRequestPermissionRationale(permissionsRejected.get(0)));{
                    //caso nao tenha uma permissao necessaria pro sistema, eh exibida a msg abaixo
                    new AlertDialog.Builder(MainActivity.this).setMessage("Para usar esse app, é preciso conceder essas permissões").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(permissionsRejected.toArray(new String[permissionsRejected.size()]), RESULT_REQUEST_PERMISSION);
                        }
                    }).create().show();
                }
            }
        }
    }
}