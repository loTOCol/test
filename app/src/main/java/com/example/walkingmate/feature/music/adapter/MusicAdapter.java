package com.example.walkingmate.feature.music.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.music.ui.MusicPlayActivity;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder>{

    private List<MusicItem> list = new ArrayList<MusicItem>();
    public MusicAdapter(List<MusicItem> list){
        this.list=list;
    }
    Context context;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music_track, parent, false);
        context = parent.getContext();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {

        MusicItem vo = list.get(position);
        holder.tv_name.setText(vo.musicTitle);
//        holder.img_main.setImageResource(vo.imageView);
        Glide.with(context).load(vo.imageView).into(holder.img_main);

        //Glide.with(getApplicationContext()).load(vo.imageView).into(holder.img_main);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Toast.makeText(getApplicationContext(),"클릭",Toast.LENGTH_SHORT);
                Intent intent = new Intent(context, MusicPlayActivity.class);

                //String name = list.get(getAdapterPosition()).getMusicTitle();
                intent.putExtra("category", vo.getCategory());
                intent.putExtra("position", position);
                intent.putExtra("title", vo.getMusicTitle());
                context.startActivity(intent);

            }
        });
    }
    //아이템 클릭 리스너 인터페이스
    public interface OnItemClickListener{
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
    public MusicItem getItem(int position){return list.get(position);}
    public static class MusicItem {

        public MusicItem(String musicTitle, Integer imageView, Integer category){
            this.musicTitle = musicTitle;
            this.imageView = imageView;
            this.category = category;
        }



        public int getImageView() {
            return imageView;
        }

        public void setImageView(int imageView) {
            this.imageView = imageView;
        }

        public String getMusicTitle() {
            return musicTitle;
        }

        public void setMusicTitle(String musicTitle) {
            this.musicTitle = musicTitle;
        }

        public int getCategory(){return category;}

        public String musicTitle;
        public int imageView;
        public int category;

    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv_name;
        ImageView img_main;

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tv_name = itemView.findViewById(R.id.music_title);
            tv_name.setSingleLine(true);    // 한줄로 표시하기
            tv_name.setEllipsize(TextUtils.TruncateAt.MARQUEE); // 흐르게 만들기
            tv_name.setSelected(true);      // 선택하기

            img_main = itemView.findViewById(R.id.music_image);
            img_main.setClipToOutline(true);    // 둥글 효과 가능하게 함

            itemView.setOnClickListener(new View.OnClickListener() {
                int pos = getAdapterPosition();


                @Override
                public void onClick(View view) {

                    if (pos!=RecyclerView.NO_POSITION){
                        if (mListener!=null){
                            mListener.onItemClick (view,pos);
                        }
                    }
                }
            });
        }
    }
}


