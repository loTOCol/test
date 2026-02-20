package com.example.walkingmate.feature.user.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.user.data.UserData;
import com.example.walkingmate.feature.feed.ui.WalkingHomeActivity;
import com.example.walkingmate.feature.shop.data.CoinManager;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditUserProfileActivity extends AppCompatActivity {

    private ImageButton back;


    private Button setfinish, resetimg, editimg, checkname ,changeWalkingAnimalButton;

    private ImageView circleImageView;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseStorage storage=FirebaseStorage.getInstance();
    StorageReference storageReference=storage.getReference();
    CollectionReference challenge=db.collection("challenge");
    CollectionReference user= db.collection("users");

    UserData userData;

    EditText username;
    Spinner usertitle;

    TextView curappname,loading;

    ArrayAdapter<String> spinneradapter;

    Bitmap curimg;

    String appname,finalappname,profileImagebig,profileImagesmall,password;


    Uri downloadUribig, downloadUrismall;

    ArrayList<String> titles=new ArrayList<>();

    private SharedPreferences sharedPreferences;
    private ArrayList<Integer> authorizedGifs;
    private int selectedGifResId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user_profile);
        CoinManager.initialize(this);
        titles.add("없음");

        userData=UserData.loadData(this);
        appname="";
        finalappname=userData.appname;
        profileImagebig="";
        profileImagesmall="";
        password = userData.password;

        username=findViewById(R.id.userappname_userset);
        usertitle=findViewById(R.id.titlespin_userset);//이부분은 구현되면 추가
        back=findViewById(R.id.back_userset);
        setfinish=findViewById(R.id.setprofile_userset);
        resetimg=findViewById(R.id.profiledefault_userset);
        editimg=findViewById(R.id.profileEdit_userset);
        checkname=findViewById(R.id.checkdup_userset);
        circleImageView=findViewById(R.id.profileImage_userset);
        curappname=findViewById(R.id.curappname_userset);
        loading=findViewById(R.id.loading_userset);
        changeWalkingAnimalButton = findViewById(R.id.changeWalkingAnimalButton);
        curappname.setText("현재 선택한 닉네임: "+finalappname);
        sharedPreferences = getSharedPreferences("CoinShopPrefs", MODE_PRIVATE);

        authorizedGifs = new ArrayList<>();
        Set<String> purchasedGifs = CoinManager.getPurchasedGifs();
        for (String gif : purchasedGifs) {
            authorizedGifs.add(Integer.parseInt(gif));
        }


        // 프로필 이미지 설정
        circleImageView.setImageResource(R.drawable.blank_profile);

        // 걷는 동물 변경 버튼 클릭 리스너 설정
        changeWalkingAnimalButton.setOnClickListener(v -> showGifSelectionDialog());

        // 기타 초기화 코드...
        curappname.setText("현재 선택한 닉네임: " + finalappname);
        curimg=UserData.loadImageToBitmap(this);
        if(curimg==null){
            curimg=BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.blank_profile);
        }
        circleImageView.setImageBitmap(curimg);

        settitles();


        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent backpage=new Intent(getApplicationContext(),WalkingHomeActivity.class);
                setResult(RESULT_OK, backpage);
                finish();
            }
        });
        resetimg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                curimg=BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.blank_profile);
                circleImageView.setImageResource(R.drawable.blank_profile);
            }
        });
        editimg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryintent=new Intent();
                galleryintent.setType("image/*");
                galleryintent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(galleryintent,1);
            }
        });
        checkname.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appname="";
                appname+=username.getText().toString();
                if(appname.equals("")){
                    return;
                }
                db.collection("users").whereEqualTo("appname",appname).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(!task.getResult().getDocuments().isEmpty()){
                            Toast.makeText(getApplicationContext(),"이미 존재하는 닉네임입니다.",Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(getApplicationContext(),"사용 가능한 닉네임입니다.",Toast.LENGTH_SHORT).show();
                            finalappname=appname;
                            curappname.setText("현재 선택한 닉네임: "+finalappname);
                        }
                    }
                });
            }
        });

        setfinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loading.setVisibility(View.VISIBLE);

                // Firestore에서 현재 사용자의 데이터를 가져와 password 필드가 있는지 확인
                db.collection("users").document(userData.userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                // password 필드가 있으면 기존 비밀번호 유지
                                if (document.contains("password")) {
                                    password = document.getString("password");
                                }

                                // 프로필 이미지 업로드 메소드 호출
                                uploadImage();
                            } else {
                                Toast.makeText(getApplicationContext(), "유저 데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "데이터 불러오기 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void showGifSelectionDialog() {
        // CoinManager에서 authorizedGifs 가져오기
        ArrayList<Integer> authorizedGifs = CoinManager.getAuthorizedGifs();

        if (authorizedGifs.isEmpty()) {
            Toast.makeText(this, "구매한 상품이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Walking Animal 선택");

        String[] gifNames = new String[authorizedGifs.size()];
        for (int i = 0; i < authorizedGifs.size(); i++) {
            gifNames[i] = "Walking Animal " + (i + 1);
        }

        builder.setSingleChoiceItems(gifNames, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedGifResId = authorizedGifs.get(which);
            }
        });

        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveSelectedGif();
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void saveSelectedGif() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("selected_walking_gif", selectedGifResId);
            editor.apply();

            CoinManager.updateAuthorizedGifs();  // 여기에 추가
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "GIF 저장에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    public void settitles(){
        user.document(userData.userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    double rel = task.getResult().getDouble("reliability");
                    challenge.document(userData.userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                DocumentSnapshot document = task.getResult();
                                Long step = document.getLong("step");
                                Long seq = document.getLong("feedseq");
                                Long meet = document.getLong("meet");

                                String stepn = "", reln = "", seqn = "", meetn = "";

                                if (step != null) {
                                    if (step < 100000) {
                                        stepn = "";
                                    } else if (step < 300000) {
                                        stepn = "[브론즈]";
                                    } else if (step < 500000) {
                                        stepn = "[실버]";
                                    } else if (step < 700000) {
                                        stepn = "[골드]";
                                    } else if (step < 1000000) {
                                        stepn = "[다이아]";
                                    } else {
                                        stepn = "[챔피언]";
                                    }
                                }

                                if (rel < 55) {
                                    reln = "";
                                } else if (rel < 65) {
                                    reln = "[브론즈]";
                                } else if (rel < 75) {
                                    reln = "[실버]";
                                } else if (rel < 85) {
                                    reln = "[골드]";
                                } else if (rel < 95) {
                                    reln = "[다이아]";
                                } else {
                                    reln = "[챔피언]";
                                }

                                if (seq != null) {
                                    if (seq < 2) {
                                        seqn = "";
                                    } else if (seq < 4) {
                                        seqn = "[브론즈]";
                                    } else if (seq < 7) {
                                        seqn = "[실버]";
                                    } else if (seq < 10) {
                                        seqn = "[골드]";
                                    } else if (seq < 15) {
                                        seqn = "[다이아]";
                                    } else {
                                        seqn = "[챔피언]";
                                    }
                                }

                                if (meet != null) {
                                    if (meet < 3) {
                                        meetn = "";
                                    } else if (meet < 5) {
                                        meetn = "[브론즈]";
                                    } else if (meet < 10) {
                                        meetn = "[실버]";
                                    } else if (meet < 20) {
                                        meetn = "[골드]";
                                    } else if (meet < 40) {
                                        meetn = "[다이아]";
                                    } else {
                                        meetn = "[챔피언]";
                                    }
                                }

                                if (!stepn.isEmpty()) {
                                    titles.add(stepn + "건강한 워커");
                                }
                                if (!reln.isEmpty()) {
                                    titles.add(reln + "믿음직한 워커");
                                }
                                if (!seqn.isEmpty()) {
                                    titles.add(seqn + "꾸준한 워커");
                                }
                                if (!meetn.isEmpty()) {
                                    titles.add(meetn + "사교적인 워커");
                                }

                                spinneradapter = new ArrayAdapter<>(EditUserProfileActivity.this, android.R.layout.simple_spinner_item, titles);
                                spinneradapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                usertitle.setAdapter(spinneradapter);
                            }
                        }
                    });
                }
            }
        });
    }




    public void uploadImage() {
        String userid = userData.userid;
        String bigfilename = userid + "bigprofile.jpg";
        String smallfilename = userid + "smallprofile.jpg";

        Bitmap bitmap = UserData.getResizedImage(curimg, true);
        Bitmap smallbitmap = UserData.getResizedImage(curimg, false);

        StorageReference uploadRefbig = storageReference.child(bigfilename);
        StorageReference uploadRefsmall = storageReference.child(smallfilename);
        ByteArrayOutputStream baosbig = new ByteArrayOutputStream();
        ByteArrayOutputStream baossmall = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baosbig);
        smallbitmap.compress(Bitmap.CompressFormat.JPEG, 100, baossmall);
        byte[] datasbig = baosbig.toByteArray();
        byte[] datassmall = baossmall.toByteArray();

        UploadTask uploadTask = uploadRefbig.putBytes(datasbig);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "이미지 업로드 실패", Toast.LENGTH_SHORT).show();
                Log.d("이미지실패", e.toString());
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return uploadRefbig.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            downloadUribig = task.getResult();
                            profileImagebig = downloadUribig.toString();
                            UploadTask uploadTask1 = uploadRefsmall.putBytes(datassmall);
                            uploadTask1.addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                }
                            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    Task<Uri> uriTask1 = uploadTask1.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                        @Override
                                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                            if (!task.isSuccessful()) {
                                                throw task.getException();
                                            }
                                            return uploadRefsmall.getDownloadUrl();
                                        }
                                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Uri> task) {
                                            if (task.isSuccessful()) {
                                                downloadUrismall = task.getResult();
                                                profileImagesmall = downloadUrismall.toString();

                                                // 기존 비밀번호 유지
                                                userData.appname = finalappname;
                                                userData.profileImagebig = profileImagebig;
                                                userData.profileImagesmall = profileImagesmall;
                                                userData.title = (String) usertitle.getSelectedItem();
                                                // 비밀번호를 포함하여 데이터 저장
                                                userData.password = password;

                                                UserData.saveBitmapToJpeg(bitmap, smallbitmap, EditUserProfileActivity.this);


                                                db.collection("users").document(userData.userid).set(UserData.getHashmap(userData)).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {
                                                        Toast.makeText(getApplicationContext(), "프로필 변경 완료.", Toast.LENGTH_SHORT).show();
                                                        UserData.saveData(userData, EditUserProfileActivity.this);
                                                        loading.setVisibility(View.INVISIBLE);
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            });
                        } else {
                            Toast.makeText(getApplicationContext(), "다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }



    @Override
    public void onBackPressed() {
        back.performClick();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            try{
                InputStream in=getContentResolver().openInputStream(data.getData());
                Bitmap tmpbmp= BitmapFactory.decodeStream(in);

                Uri uri=data.getData();
                Log.d("uri체크",uri.toString());
                Uri PhotoUri;
                try{
                    PhotoUri=Uri.parse(getRealPathFromURI(uri));
                }catch (IllegalArgumentException e){
                    PhotoUri=Uri.parse(getRealPathFromURIgal(uri));
                }
                
                ExifInterface exif=new ExifInterface(PhotoUri.getPath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                curimg = rotateBitmap(tmpbmp, orientation);




                circleImageView.setImageBitmap(curimg);
                in.close();
            }catch (Exception e){e.printStackTrace();}
        }
    }

    //최근파일에서 동작
    private String getRealPathFromURI(Uri contentUri) {

        if (contentUri.getPath().startsWith("/storage")) {
            return contentUri.getPath();
        }
        String id = DocumentsContract.getDocumentId(contentUri).split(":")[1];
        String[] columns = { MediaStore.Files.FileColumns.DATA };
        String selection = MediaStore.Files.FileColumns._ID + " = " + id;
        Cursor cursor = getContentResolver().query(MediaStore.Files.getContentUri("external"), columns, selection, null, null);
        try {
            int columnIndex = cursor.getColumnIndex(columns[0]);
            if (cursor.moveToFirst()) {
                return cursor.getString(columnIndex);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    //갤러리에서 동작
    private String getRealPathFromURIgal(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }
}
