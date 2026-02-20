package com.example.walkingmate.feature.music.adapter;

import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.music.ui.MusicItemActivity;

import java.util.ArrayList;
import java.util.List;

public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.ViewHolder>{


    private List<MusicCategory> list = new ArrayList<MusicCategory>();
    public MusicListAdapter(List<MusicCategory> list){
        this.list=list;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music_category, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        MusicCategory vo = list.get(position);
        holder.tv_name.setText(vo.musicCategory);
        holder.img_main.setImageResource(vo.imageView);


    }

    //아이템 클릭 리스너 인터페이스
    interface OnItemClickListener{
        void onItemClick(View v, int position); //뷰와 포지션값
    }
    //리스너 객체 참조 변수
    private OnItemClickListener mListener = null;
    //리스너 객체 참조를 어댑터에 전달 메서드
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }
    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MusicCategory {

        public MusicCategory(String musicCategory, Integer imageView){
            this.musicCategory = musicCategory;
            this.imageView = imageView;
        }
        public String getMusicCategory() {
            return musicCategory;
        }

        public void setMusicCategory(String musicCategory) {
            this.musicCategory = musicCategory;
        }

        public int getImageView() {
            return imageView;
        }

        public void setImageView(int imageView) {
            this.imageView = imageView;
        }

        public String musicCategory;
        public int imageView;

    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv_name;
        ImageView img_main;
        Intent intent;
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tv_name = itemView.findViewById(R.id.music_item_text);
            img_main = itemView.findViewById(R.id.music_item_image);
            img_main.setClipToOutline(true);    // 모서리 둥글 효과 가능하게 함

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int pos = getAdapterPosition();
                    intent = new Intent(view.getContext(), MusicItemActivity.class);
                    intent.putExtra("number", pos);
                    view.getContext().startActivity(intent);

//                    Toast.makeText(view.getContext(), "클릭 되었습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
