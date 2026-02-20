package com.example.walkingmate.feature.misc.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.walkingmate.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RankingFragment extends Fragment {

    private static final int MAX_RANK = 10;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference users = db.collection("users");
    private final CollectionReference challenges = db.collection("challenge");

    private ImageView rankingFirstImage;
    private TextView rankingFirstName;
    private TextView rankingFirstStep;
    private ImageView rankingSecondImage;
    private TextView rankingSecondName;
    private TextView rankingSecondStep;
    private ImageView rankingThirdImage;
    private TextView rankingThirdName;
    private TextView rankingThirdStep;
    private LinearLayout rankingList;
    private TextView rankingUpdated;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ranking, container, false);

        rankingFirstImage = view.findViewById(R.id.ranking_first_image);
        rankingFirstName = view.findViewById(R.id.ranking_first_name);
        rankingFirstStep = view.findViewById(R.id.ranking_first_step);
        rankingSecondImage = view.findViewById(R.id.ranking_second_image);
        rankingSecondName = view.findViewById(R.id.ranking_second_name);
        rankingSecondStep = view.findViewById(R.id.ranking_second_step);
        rankingThirdImage = view.findViewById(R.id.ranking_third_image);
        rankingThirdName = view.findViewById(R.id.ranking_third_name);
        rankingThirdStep = view.findViewById(R.id.ranking_third_step);
        rankingList = view.findViewById(R.id.ranking_list);
        rankingUpdated = view.findViewById(R.id.ranking_updated);

        ImageButton refreshButton = view.findViewById(R.id.button_refresh);
        refreshButton.setOnClickListener(v -> loadRankingData());

        loadRankingData();
        return view;
    }

    private void loadRankingData() {
        rankingList.removeAllViews();
        setPodiumPlaceholders();
        setUpdatedNow();

        challenges.orderBy("step", Query.Direction.DESCENDING).limit(MAX_RANK).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (!task.isSuccessful() || task.getResult() == null) {
                            fillEmptyRows(4, MAX_RANK);
                            return;
                        }

                        List<DocumentSnapshot> challengeDocs = task.getResult().getDocuments();
                        if (challengeDocs.isEmpty()) {
                            fillEmptyRows(4, MAX_RANK);
                            return;
                        }

                        Map<String, DocumentSnapshot> userDocs = new HashMap<>();
                        int[] remain = {challengeDocs.size()};

                        for (DocumentSnapshot doc : challengeDocs) {
                            String userId = doc.getId();
                            users.document(userId).get().addOnCompleteListener(userTask -> {
                                if (userTask.isSuccessful() && userTask.getResult() != null && userTask.getResult().exists()) {
                                    userDocs.put(userId, userTask.getResult());
                                }
                                remain[0]--;
                                if (remain[0] == 0) {
                                    updateUI(challengeDocs, userDocs);
                                }
                            });
                        }
                    }
                });
    }

    private void updateUI(List<DocumentSnapshot> challengeDocs, Map<String, DocumentSnapshot> userDocs) {
        bindPodium(0, challengeDocs, userDocs, rankingFirstImage, rankingFirstName, rankingFirstStep);
        bindPodium(1, challengeDocs, userDocs, rankingSecondImage, rankingSecondName, rankingSecondStep);
        bindPodium(2, challengeDocs, userDocs, rankingThirdImage, rankingThirdName, rankingThirdStep);

        for (int i = 3; i < Math.min(MAX_RANK, challengeDocs.size()); i++) {
            addRankRow(i + 1, challengeDocs.get(i), userDocs.get(challengeDocs.get(i).getId()));
        }

        int startEmpty = Math.min(MAX_RANK, challengeDocs.size()) + 1;
        if (startEmpty <= MAX_RANK) {
            fillEmptyRows(startEmpty, MAX_RANK);
        }
    }

    private void bindPodium(int index, List<DocumentSnapshot> challengeDocs, Map<String, DocumentSnapshot> userDocs,
                            ImageView imageView, TextView nameView, TextView stepView) {
        if (challengeDocs.size() <= index) {
            setPodiumItem(imageView, nameView, stepView, "-", 0, null);
            return;
        }
        DocumentSnapshot challengeDoc = challengeDocs.get(index);
        DocumentSnapshot userDoc = userDocs.get(challengeDoc.getId());
        String name = userDoc != null ? safeText(userDoc.getString("appname"), "알 수 없는 사용자") : "알 수 없는 사용자";
        String imageUrl = userDoc != null ? userDoc.getString("profileImagesmall") : null;
        long step = safeLong(challengeDoc.getLong("step"));
        setPodiumItem(imageView, nameView, stepView, name, step, imageUrl);
    }

    private void setPodiumItem(ImageView imageView, TextView nameView, TextView stepView,
                               String name, long step, String imageUrl) {
        nameView.setText(name);
        stepView.setText(formatStep(step));
        if (imageUrl == null || imageUrl.trim().equals("")) {
            imageView.setImageResource(R.drawable.blank_profile);
        } else {
            Glide.with(requireContext()).load(imageUrl).placeholder(R.drawable.blank_profile).into(imageView);
        }
    }

    private void addRankRow(int rank, DocumentSnapshot challengeDoc, DocumentSnapshot userDoc) {
        View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_ranking_user, rankingList, false);
        ImageView itemImage = itemView.findViewById(R.id.ranking_item_image);
        TextView itemName = itemView.findViewById(R.id.ranking_item_name);
        TextView itemStep = itemView.findViewById(R.id.ranking_item_step);
        TextView itemRank = itemView.findViewById(R.id.ranking_item_rank);

        itemRank.setText(String.valueOf(rank));
        itemName.setText(userDoc == null ? "알 수 없는 사용자" : safeText(userDoc.getString("appname"), "알 수 없는 사용자"));
        itemStep.setText(formatStep(safeLong(challengeDoc.getLong("step"))));

        if (userDoc == null || userDoc.getString("profileImagesmall") == null || userDoc.getString("profileImagesmall").trim().equals("")) {
            itemImage.setImageResource(R.drawable.blank_profile);
        } else {
            Glide.with(requireContext()).load(userDoc.getString("profileImagesmall")).placeholder(R.drawable.blank_profile).into(itemImage);
        }

        rankingList.addView(itemView);
    }

    private void fillEmptyRows(int startRank, int endRank) {
        for (int rank = startRank; rank <= endRank; rank++) {
            View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_ranking_user, rankingList, false);
            ImageView itemImage = itemView.findViewById(R.id.ranking_item_image);
            TextView itemName = itemView.findViewById(R.id.ranking_item_name);
            TextView itemStep = itemView.findViewById(R.id.ranking_item_step);
            TextView itemRank = itemView.findViewById(R.id.ranking_item_rank);

            itemRank.setText(String.valueOf(rank));
            itemName.setText("비어 있음");
            itemStep.setText("- 걸음");
            itemImage.setImageResource(R.drawable.blank_profile);
            rankingList.addView(itemView);
        }
    }

    private void setPodiumPlaceholders() {
        setPodiumItem(rankingFirstImage, rankingFirstName, rankingFirstStep, "-", 0, null);
        setPodiumItem(rankingSecondImage, rankingSecondName, rankingSecondStep, "-", 0, null);
        setPodiumItem(rankingThirdImage, rankingThirdName, rankingThirdStep, "-", 0, null);
    }

    private void setUpdatedNow() {
        String now = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.KOREA).format(new Date());
        rankingUpdated.setText("업데이트: " + now + " · Top 10");
    }

    private String formatStep(long step) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.KOREA);
        return nf.format(step) + " 걸음";
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().equals("")) {
            return fallback;
        }
        return value.trim();
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
